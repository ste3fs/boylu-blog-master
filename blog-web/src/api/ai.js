import request from '@/utils/request'
import { getToken } from '@/utils/cookie'

const rawBaseApi = import.meta.env.VITE_APP_BASE_API
const baseURL = typeof rawBaseApi === 'string' && rawBaseApi.trim()
  ? rawBaseApi.trim().replace(/^['"]|['"]$/g, '')
  : '/boylu'

const AI_ERROR_MESSAGES = {
  loginRequired: '请先登录后再使用 AI 助手',
  streamFailed: '流式请求失败，请稍后再试',
  emptyStream: '流式响应为空',
  streamUnavailable: '流式通道暂时不可用',
  emptyReply: 'AI 暂时没有返回内容，请稍后再试'
}

export function getAiSessionListApi(params) {
  return request({
    url: '/api/ai/sessions',
    method: 'get',
    params
  })
}

export function getAiSessionDetailApi(sessionId) {
  return request({
    url: `/api/ai/session/${sessionId}`,
    method: 'get'
  })
}

export function createAiSessionApi(data) {
  return request({
    url: '/api/ai/session',
    method: 'post',
    data
  })
}

export function deleteAiSessionApi(sessionId) {
  return request({
    url: `/api/ai/session/${sessionId}`,
    method: 'delete'
  })
}

export function sendAiMessageApi(data) {
  return request({
    url: '/api/ai/chat/send',
    method: 'post',
    data
  })
}

function normalizeAiErrorMessage(rawText, fallbackMessage) {
  if (!rawText) {
    return fallbackMessage
  }

  try {
    const parsed = JSON.parse(rawText)
    if (parsed?.code === 401) {
      return AI_ERROR_MESSAGES.loginRequired
    }
    if (parsed?.message) {
      return parsed.message
    }
    if (parsed?.error?.message) {
      return parsed.error.message
    }
  } catch (error) {
    // Ignore parse failure and fall through to raw text.
  }

  return rawText
}

export async function streamAiMessageApi(data, handlers = {}) {
  const token = getToken()
  const response = await fetch(`${baseURL}/api/ai/chat/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: token } : {})
    },
    body: JSON.stringify(data)
  })

  if (!response.ok) {
    const text = await response.text()
    if (response.status === 401) {
      throw new Error(AI_ERROR_MESSAGES.loginRequired)
    }
    throw new Error(normalizeAiErrorMessage(text, AI_ERROR_MESSAGES.streamFailed))
  }

  const contentType = response.headers.get('content-type') || ''
  if (!response.body) {
    throw new Error(AI_ERROR_MESSAGES.emptyStream)
  }
  if (!/text\/event-stream/i.test(contentType)) {
    const text = await response.text()
    throw new Error(normalizeAiErrorMessage(text, AI_ERROR_MESSAGES.streamUnavailable))
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let eventCount = 0

  const parseEventBlock = (block) => {
    const lines = block.split('\n')
    let eventName = 'message'
    const dataLines = []

    lines.forEach((line) => {
      if (line.startsWith('event:')) {
        eventName = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        dataLines.push(line.slice(5).trim())
      }
    })

    if (!dataLines.length) {
      return
    }

    eventCount += 1
    const raw = dataLines.join('\n')
    let payload = raw

    try {
      payload = JSON.parse(raw)
    } catch (error) {
      payload = raw
    }

    if (typeof handlers.onEvent === 'function') {
      handlers.onEvent(eventName, payload)
    }

    const eventHandlerMap = {
      session: handlers.onSession,
      user: handlers.onUser,
      delta: handlers.onDelta,
      done: handlers.onDone,
      error: handlers.onError
    }

    const handler = eventHandlerMap[eventName]
    if (typeof handler === 'function') {
      handler(payload)
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done }).replace(/\r\n/g, '\n')

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex !== -1) {
      const block = buffer.slice(0, separatorIndex).trim()
      buffer = buffer.slice(separatorIndex + 2)
      if (block) {
        parseEventBlock(block)
      }
      separatorIndex = buffer.indexOf('\n\n')
    }

    if (done) {
      const tail = buffer.trim()
      if (tail) {
        parseEventBlock(tail)
      }
      break
    }
  }

  if (eventCount === 0) {
    throw new Error(AI_ERROR_MESSAGES.emptyReply)
  }
}
