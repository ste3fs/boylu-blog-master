const LOCAL_FILE_SEGMENT = '/localFile/'
const PUBLIC_CONTENT_PREFIX = '/boylu/file/content/'

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

const FILE_PREFIXES = [...CONTENT_PREFIXES, ...VIEW_PREFIXES]

function stripOrigin(url = '') {
  if (!url) {
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

function splitSuffix(value: string) {
  const queryIndex = value.indexOf('?')
  const hashIndex = value.indexOf('#')
  let end = value.length

  if (queryIndex >= 0) {
    end = Math.min(end, queryIndex)
  }
  if (hashIndex >= 0) {
    end = Math.min(end, hashIndex)
  }

  return {
    path: value.slice(0, end),
    suffix: value.slice(end)
  }
}

export function normalizeImageUrl(url = '') {
  if (typeof url !== 'string') {
    return ''
  }

  const rawUrl = url.trim()
  if (!rawUrl) {
    return ''
  }

  const isRelativeAsset = !rawUrl.startsWith('/')
    && !rawUrl.startsWith('//')
    && !/^https?:\/\//i.test(rawUrl)
    && !rawUrl.startsWith('data:')
    && !rawUrl.startsWith('blob:')
    && /\.[a-z0-9]{2,8}([?#].*)?$/i.test(rawUrl)

  const isManagedUrl = rawUrl.includes(LOCAL_FILE_SEGMENT)
    || FILE_PREFIXES.some(prefix => rawUrl.includes(prefix))

  if (!isManagedUrl) {
    return isRelativeAsset ? `/${rawUrl}` : rawUrl
  }

  let normalized = stripOrigin(rawUrl)

  const localIndex = normalized.indexOf(LOCAL_FILE_SEGMENT)
  if (localIndex >= 0) {
    return normalized.slice(localIndex)
  }

  normalized = normalized.replace(
    /(?:\/(?:boylu|mojian))+\/file\/(view|content)\//i,
    '/boylu/file/$1/'
  )

  for (const prefix of CONTENT_PREFIXES) {
    const index = normalized.indexOf(prefix)
    if (index >= 0) {
      return PUBLIC_CONTENT_PREFIX + normalized.slice(index + prefix.length)
    }
  }

  for (const prefix of VIEW_PREFIXES) {
    const index = normalized.indexOf(prefix)
    if (index >= 0) {
      return PUBLIC_CONTENT_PREFIX + normalized.slice(index + prefix.length)
    }
  }

  return normalized
}

export function isManagedImageUrl(url = '') {
  if (typeof url !== 'string' || !url) {
    return false
  }

  const normalized = stripOrigin(url)
  return normalized.includes(LOCAL_FILE_SEGMENT)
    || FILE_PREFIXES.some(prefix => normalized.includes(prefix))
}

export function getImageName(url = '') {
  const normalized = normalizeImageUrl(url)
  if (!normalized) {
    return ''
  }

  const { path } = splitSuffix(normalized)
  const segments = path.split('/').filter(Boolean)
  return segments[segments.length - 1] || 'image'
}

export function normalizeImageList(value: string | string[] | undefined | null) {
  if (!value) {
    return []
  }

  const urls = Array.isArray(value) ? value : [value]
  return urls
    .map(url => normalizeImageUrl(url))
    .filter(Boolean)
}

export function normalizeManagedFilePayload<T>(payload: T, visited = new WeakSet<object>()): T {
  if (typeof payload === 'string') {
    return normalizeImageUrl(payload) as T
  }

  if (Array.isArray(payload)) {
    return payload.map(item => normalizeManagedFilePayload(item, visited)) as T
  }

  if (payload && typeof payload === 'object') {
    if (visited.has(payload as object)) {
      return payload
    }
    visited.add(payload as object)

    Object.keys(payload as Record<string, unknown>).forEach((key) => {
      const nextPayload = payload as Record<string, unknown>
      nextPayload[key] = normalizeManagedFilePayload(nextPayload[key], visited)
    })
  }

  return payload
}
