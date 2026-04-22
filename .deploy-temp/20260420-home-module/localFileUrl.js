const ABSOLUTE_LOCAL_FILE_PREFIX = /https?:\/\/[^"'\\s)]+\/localFile\//gi
const PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX = /\/\/[^"'\\s)]+\/localFile\//gi
const ABSOLUTE_FILE_VIEW_PREFIX = /https?:\/\/[^"'\\s)]+(?:\/mojian)?\/file\/(?:view|content)\//gi
const PROTOCOL_RELATIVE_FILE_VIEW_PREFIX = /\/\/[^"'\\s)]+(?:\/mojian)?\/file\/(?:view|content)\//gi
const LOCAL_FILE_SEGMENT = '/localFile/'
const FILE_VIEW_SEGMENT = '/mojian/file/content/'
const FILE_VIEW_SEGMENT_NO_PREFIX = '/file/content/'
const LEGACY_FILE_VIEW_SEGMENT = '/mojian/file/view/'
const LEGACY_FILE_VIEW_SEGMENT_NO_PREFIX = '/file/view/'

function isFileViewUrl(url = '') {
  return typeof url === 'string' && (
    url.includes(FILE_VIEW_SEGMENT)
    || url.includes(FILE_VIEW_SEGMENT_NO_PREFIX)
    || url.includes(LEGACY_FILE_VIEW_SEGMENT)
    || url.includes(LEGACY_FILE_VIEW_SEGMENT_NO_PREFIX)
  )
}

export function normalizeLocalFileUrl(url = '') {
  if (isFileViewUrl(url)) {
    return normalizeFileViewUrl(url)
  }
  if (typeof url !== 'string' || !url.includes(LOCAL_FILE_SEGMENT)) {
    return url || ''
  }
  return url.slice(url.indexOf(LOCAL_FILE_SEGMENT))
}

export function normalizeFileViewUrl(url = '') {
  if (!isFileViewUrl(url)) {
    return url || ''
  }
  if (url.includes(FILE_VIEW_SEGMENT)) {
    return url.slice(url.indexOf(FILE_VIEW_SEGMENT))
  }
  if (url.includes(LEGACY_FILE_VIEW_SEGMENT)) {
    return `${FILE_VIEW_SEGMENT}${url.slice(url.indexOf(LEGACY_FILE_VIEW_SEGMENT) + LEGACY_FILE_VIEW_SEGMENT.length)}`
  }
  if (url.includes(FILE_VIEW_SEGMENT_NO_PREFIX)) {
    return `${FILE_VIEW_SEGMENT}${url.slice(url.indexOf(FILE_VIEW_SEGMENT_NO_PREFIX) + FILE_VIEW_SEGMENT_NO_PREFIX.length)}`
  }
  return `${FILE_VIEW_SEGMENT}${url.slice(url.indexOf(LEGACY_FILE_VIEW_SEGMENT_NO_PREFIX) + LEGACY_FILE_VIEW_SEGMENT_NO_PREFIX.length)}`
}

export function normalizeLocalFileText(text = '') {
  if (
    typeof text !== 'string'
    || (
      !text.includes(LOCAL_FILE_SEGMENT)
      && !text.includes(FILE_VIEW_SEGMENT_NO_PREFIX)
      && !text.includes(LEGACY_FILE_VIEW_SEGMENT_NO_PREFIX)
      && !text.includes(FILE_VIEW_SEGMENT)
      && !text.includes(LEGACY_FILE_VIEW_SEGMENT)
    )
  ) {
    return text || ''
  }

  let normalized = text
    .replace(ABSOLUTE_LOCAL_FILE_PREFIX, LOCAL_FILE_SEGMENT)
    .replace(PROTOCOL_RELATIVE_LOCAL_FILE_PREFIX, LOCAL_FILE_SEGMENT)
    .replace(ABSOLUTE_FILE_VIEW_PREFIX, FILE_VIEW_SEGMENT)
    .replace(PROTOCOL_RELATIVE_FILE_VIEW_PREFIX, FILE_VIEW_SEGMENT)

  if (normalized.startsWith(LOCAL_FILE_SEGMENT)) {
    normalized = normalizeLocalFileUrl(normalized)
  } else if (normalized.startsWith(FILE_VIEW_SEGMENT_NO_PREFIX) || normalized.startsWith(FILE_VIEW_SEGMENT)) {
    normalized = normalizeFileViewUrl(normalized)
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
