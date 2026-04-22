import {
  normalizeLocalFileText,
  normalizeLocalFileUrl as normalizeManagedFileUrl,
} from '@/utils/localFileUrl'

const RETRY_PARAM = '_imgv'
const LOCAL_FILE_SEGMENT = '/localFile/'
const FILE_BY_URL_SEGMENTS = [
  '/boylu/file/by-url?url=',
  '/mojian/file/by-url?url=',
  '/file/by-url?url='
]
const FILE_VIEW_SEGMENTS = [
  '/boylu/file/content/',
  '/mojian/file/content/',
  '/file/content/',
  '/boylu/file/view/',
  '/mojian/file/view/',
  '/file/view/'
]

export function isLocalFileUrl(url = '') {
  return typeof url === 'string' && (
    url.includes(LOCAL_FILE_SEGMENT)
    || FILE_BY_URL_SEGMENTS.some(segment => url.includes(segment))
    || FILE_VIEW_SEGMENTS.some(segment => url.includes(segment))
  )
}

export function normalizeLocalFileUrl(url = '') {
  const normalized = normalizeLocalFileText(url || '')
  return normalizeManagedFileUrl(normalized)
}

export function stripImageRetryParam(url = '') {
  if (!url) {
    return ''
  }

  const [withoutHash, hash = ''] = url.split('#')
  const [base, query = ''] = withoutHash.split('?')
  if (!query) {
    return url
  }

  const nextQuery = query
    .split('&')
    .filter(Boolean)
    .filter(item => !item.startsWith(`${RETRY_PARAM}=`))
    .join('&')

  const rebuilt = nextQuery ? `${base}?${nextQuery}` : base
  return hash ? `${rebuilt}#${hash}` : rebuilt
}

export function withImageRetryParam(url, token = Date.now()) {
  const cleanUrl = stripImageRetryParam(url)
  if (!cleanUrl) {
    return ''
  }

  const joiner = cleanUrl.includes('?') ? '&' : '?'
  return `${cleanUrl}${joiner}${RETRY_PARAM}=${token}`
}

export function resolveImageUrl(url, fallback = '') {
  return normalizeLocalFileUrl(url) || normalizeLocalFileUrl(fallback) || ''
}

export function retryImageLoad(target, fallbackSrc = '', maxRetries = 2) {
  if (!target) {
    return false
  }

  const originalSrc = resolveImageUrl(
    target.dataset.origin || stripImageRetryParam(target.currentSrc || target.src || '')
  )
  const retryCount = Number(target.dataset.retryCount || 0)
  const normalizedFallback = resolveImageUrl(fallbackSrc)

  if (isLocalFileUrl(originalSrc) && retryCount < maxRetries) {
    target.dataset.origin = originalSrc
    target.dataset.retryCount = String(retryCount + 1)
    target.src = withImageRetryParam(originalSrc)
    return true
  }

  if (normalizedFallback) {
    target.src = normalizedFallback
    target.classList.add('fallback')
  }

  return false
}
