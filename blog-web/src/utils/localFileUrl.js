const LOCAL_FILE_SEGMENT = '/localFile/'
const CONTENT_PREFIXES = [
  '/boylu/file/content/',
  '/mojian/file/content/',
  '/file/content/'
]
const VIEW_PREFIXES = [
  '/boylu/file/view/',
  '/mojian/file/view/',
  '/file/view/'
]
const FILE_VIEW_PREFIXES = [...CONTENT_PREFIXES, ...VIEW_PREFIXES]

const ABSOLUTE_LOCAL_FILE_PREFIX = /https?:\/\/[^"'\\s)]+\/localFile\//gi
const PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX = /\/\/[^"'\\s)]+\/localFile\//gi
const ABSOLUTE_FILE_VIEW_PREFIX = /https?:\/\/[^"'\\s)]+(?:\/(?:boylu|mojian))?\/file\/(?:view|content)\//gi
const PROTOCOL_RELATIVE_FILE_VIEW_PREFIX = /\/\/[^"'\\s)]+(?:\/(?:boylu|mojian))?\/file\/(?:view|content)\//gi
const DUPLICATED_NAMESPACE_FILE_PREFIX = /(?:\/(?:boylu|mojian)){2,}\/file\/(?:view|content)\//i
const STANDALONE_ABSOLUTE_LOCAL_FILE_PREFIX = /^https?:\/\/[^"'\\s)]+\/localFile\//i
const STANDALONE_PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX = /^\/\/[^"'\\s)]+\/localFile\//i
const STANDALONE_ABSOLUTE_FILE_VIEW_PREFIX = /^https?:\/\/[^"'\\s)]+(?:\/(?:boylu|mojian))?\/file\/(?:view|content)\//i
const STANDALONE_PROTOCOL_RELATIVE_FILE_VIEW_PREFIX = /^\/\/[^"'\\s)]+(?:\/(?:boylu|mojian))?\/file\/(?:view|content)\//i
const SELF_HOSTED_SITE_PREFIX = /^https?:\/\/(?:(?:www\.)?boylu\.(?:cn|top)|111\.229\.123\.234)(?::\d+)?\//i
const SELF_HOSTED_PROTOCOL_PREFIX = /^\/\/(?:(?:www\.)?boylu\.(?:cn|top)|111\.229\.123\.234)(?::\d+)?\//i
const LOCAL_FILE_REFERENCE = /\/localFile\/[^"'\\s)<]+/g

function stripOrigin(url = '') {
  if (typeof url !== 'string' || !url) {
    return ''
  }

  if (url.startsWith('//')) {
    return url.replace(/^\/\/[^/]+/, '')
  }

  if (/^https?:\/\//i.test(url)) {
    try {
      const parsed = new URL(url)
      return `${parsed.pathname}${parsed.search}${parsed.hash}`
    } catch (error) {
      return url
    }
  }

  return url
}

function normalizeSelfHostedOrigin(url = '') {
  if (typeof url !== 'string' || !url) {
    return ''
  }

  if (SELF_HOSTED_PROTOCOL_PREFIX.test(url)) {
    return url.replace(/^\/\/[^/]+/, '/')
  }

  if (SELF_HOSTED_SITE_PREFIX.test(url)) {
    try {
      const parsed = new URL(url)
      return `${parsed.pathname}${parsed.search}${parsed.hash}`
    } catch (error) {
      return url
    }
  }

  return url
}

function toLocalFilePath(url = '') {
  if (typeof url !== 'string' || !url) {
    return ''
  }

  const normalized = stripOrigin(url)
  const localIndex = normalized.indexOf(LOCAL_FILE_SEGMENT)
  if (localIndex < 0) {
    return normalized || ''
  }

  const localPath = normalized.slice(localIndex)
  return localPath
}

function findPrefix(url = '', prefixes = []) {
  return prefixes.find(prefix => url.includes(prefix)) || ''
}

function isStandaloneFileReference(text = '') {
  if (typeof text !== 'string') {
    return false
  }

  const trimmed = text.trim()
  if (!trimmed || /[<>\r\n]/.test(trimmed)) {
    return false
  }

  return trimmed.startsWith(LOCAL_FILE_SEGMENT)
    || FILE_VIEW_PREFIXES.some(prefix => trimmed.startsWith(prefix))
    || STANDALONE_ABSOLUTE_LOCAL_FILE_PREFIX.test(trimmed)
    || STANDALONE_PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX.test(trimmed)
    || STANDALONE_ABSOLUTE_FILE_VIEW_PREFIX.test(trimmed)
    || STANDALONE_PROTOCOL_RELATIVE_FILE_VIEW_PREFIX.test(trimmed)
}

function normalizeContentPath(url = '') {
  let normalized = stripOrigin(url)
  if (DUPLICATED_NAMESPACE_FILE_PREFIX.test(normalized)) {
    normalized = normalized.replace(/(?:\/(?:boylu|mojian))+\/file\/(view|content)\//i, '/boylu/file/$1/')
  }
  const contentPrefix = findPrefix(normalized, CONTENT_PREFIXES)
  if (contentPrefix) {
    return normalized.slice(normalized.indexOf(contentPrefix))
  }

  const viewPrefix = findPrefix(normalized, VIEW_PREFIXES)
  if (viewPrefix) {
    const contentTarget = CONTENT_PREFIXES[VIEW_PREFIXES.indexOf(viewPrefix)]
    return `${contentTarget}${normalized.slice(normalized.indexOf(viewPrefix) + viewPrefix.length)}`
  }

  return normalized || ''
}

export function isFileViewUrl(url = '') {
  if (typeof url !== 'string' || !url) {
    return false
  }

  const normalized = stripOrigin(url)
  return FILE_VIEW_PREFIXES.some(prefix => normalized.includes(prefix))
}

export function normalizeLocalFileUrl(url = '') {
  const selfHostedUrl = normalizeSelfHostedOrigin(url)

  if (isFileViewUrl(selfHostedUrl)) {
    return normalizeFileViewUrl(selfHostedUrl)
  }

  if (typeof selfHostedUrl !== 'string' || !selfHostedUrl.includes(LOCAL_FILE_SEGMENT)) {
    return selfHostedUrl || ''
  }

  return toLocalFilePath(selfHostedUrl)
}

export function normalizeFileViewUrl(url = '') {
  if (!isFileViewUrl(url)) {
    return url || ''
  }

  return normalizeContentPath(url)
}

export function normalizeLocalFileText(text = '') {
  const normalizedSelfHostedText = normalizeSelfHostedOrigin(text)
  if (
    typeof normalizedSelfHostedText !== 'string'
    || (
      !normalizedSelfHostedText.includes(LOCAL_FILE_SEGMENT)
      && !isFileViewUrl(normalizedSelfHostedText)
      && !ABSOLUTE_LOCAL_FILE_PREFIX.test(normalizedSelfHostedText)
      && !ABSOLUTE_FILE_VIEW_PREFIX.test(normalizedSelfHostedText)
    )
  ) {
    ABSOLUTE_LOCAL_FILE_PREFIX.lastIndex = 0
    PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX.lastIndex = 0
    ABSOLUTE_FILE_VIEW_PREFIX.lastIndex = 0
    PROTOCOL_RELATIVE_FILE_VIEW_PREFIX.lastIndex = 0
    return normalizedSelfHostedText || ''
  }

  ABSOLUTE_LOCAL_FILE_PREFIX.lastIndex = 0
  PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX.lastIndex = 0
  ABSOLUTE_FILE_VIEW_PREFIX.lastIndex = 0
  PROTOCOL_RELATIVE_FILE_VIEW_PREFIX.lastIndex = 0

  let normalized = normalizedSelfHostedText
    .replace(ABSOLUTE_LOCAL_FILE_PREFIX, LOCAL_FILE_SEGMENT)
    .replace(PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX, LOCAL_FILE_SEGMENT)
    .replace(ABSOLUTE_FILE_VIEW_PREFIX, match => normalizeContentPath(match))
    .replace(PROTOCOL_RELATIVE_FILE_VIEW_PREFIX, match => normalizeContentPath(match))
    .replace(LOCAL_FILE_REFERENCE, match => toLocalFilePath(match))

  const trimmedNormalized = normalized.trim()
  if (isStandaloneFileReference(trimmedNormalized)) {
    let normalizedUrl = trimmedNormalized
    if (normalizedUrl.startsWith(LOCAL_FILE_SEGMENT)) {
      normalizedUrl = normalizeLocalFileUrl(normalizedUrl)
    } else if (isFileViewUrl(normalizedUrl)) {
      normalizedUrl = normalizeFileViewUrl(normalizedUrl)
    }
    normalized = normalized.replace(trimmedNormalized, normalizedUrl)
  }

  return normalized
}

export function normalizeLocalFilePayload(payload, visited = new WeakSet()) {
  if (Array.isArray(payload)) {
    for (let i = 0; i < payload.length; i += 1) {
      payload[i] = normalizeLocalFilePayload(payload[i], visited)
    }
    return payload
  }

  if (payload && typeof payload === 'object') {
    if (visited.has(payload)) {
      return payload
    }
    visited.add(payload)
    Object.keys(payload).forEach((key) => {
      payload[key] = normalizeLocalFilePayload(payload[key], visited)
    })
    return payload
  }

  if (typeof payload === 'string') {
    return normalizeLocalFileText(payload)
  }

  return payload
}
