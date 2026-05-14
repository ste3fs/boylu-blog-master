import {
  normalizeLocalFileText,
  normalizeLocalFileUrl as normalizeManagedFileUrl,
} from '@/utils/localFileUrl'

const RETRY_PARAM = '_imgv'
const LOCAL_FILE_SEGMENT = '/localFile/'
const BROKEN_LOGO_PATH = '/boylu-logo.png'
const FULL_AVATAR_PATH = '/boylu-avatar.jpg'
const lazyLoadingImage = new URL('../assets/loading.svg', import.meta.url).href
const lazyErrorImage = new URL('../assets/img-error.svg', import.meta.url).href
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
const LOCAL_IMAGE_STYLE_PREFIX = '/img/local/'
export const SMART_STYLE_WIDTHS = [160, 240, 320, 480, 640, 960, 1280]

export function isLocalFileUrl(url = '') {
  return typeof url === 'string' && (
    url.includes(LOCAL_FILE_SEGMENT)
    || FILE_BY_URL_SEGMENTS.some(segment => url.includes(segment))
    || FILE_VIEW_SEGMENTS.some(segment => url.includes(segment))
  )
}

export function normalizeLocalFileUrl(url = '') {
  const normalized = normalizeLocalFileText(url || '')
  const managedUrl = normalizeManagedFileUrl(normalized)
  if (typeof managedUrl === 'string' && managedUrl.includes(BROKEN_LOGO_PATH)) {
    return managedUrl.replace(BROKEN_LOGO_PATH, FULL_AVATAR_PATH)
  }
  return managedUrl
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

export function isStylableImageUrl(url = '') {
  const normalized = resolveImageUrl(url)
  if (!normalized || normalized.startsWith('data:') || normalized.startsWith(LOCAL_IMAGE_STYLE_PREFIX)) {
    return false
  }
  return normalized.includes(LOCAL_FILE_SEGMENT) || FILE_VIEW_SEGMENTS.some(segment => normalized.includes(segment))
}

function toBase64Url(value = '') {
  const bytes = encodeURIComponent(value).replace(/%([0-9A-F]{2})/g, (_match, hex) => {
    return String.fromCharCode(Number.parseInt(hex, 16))
  })
  return btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

export function buildLocalImageStyleUrl(url = '', width = 480, format = 'webp') {
  const normalized = resolveImageUrl(url)
  if (!isStylableImageUrl(normalized)) {
    return normalized
  }
  const safeWidth = SMART_STYLE_WIDTHS.includes(Number(width)) ? Number(width) : 480
  const safeFormat = ['webp', 'jpg', 'jpeg'].includes(String(format).toLowerCase())
    ? String(format).toLowerCase().replace('jpeg', 'jpg')
    : 'webp'
  return `${LOCAL_IMAGE_STYLE_PREFIX}${toBase64Url(normalized)}!w${safeWidth}.${safeFormat}`
}

export function buildLocalImageSrcset(url = '', format = 'webp', widths = SMART_STYLE_WIDTHS) {
  const normalized = resolveImageUrl(url)
  if (!isStylableImageUrl(normalized)) {
    return ''
  }
  return (Array.isArray(widths) ? widths : SMART_STYLE_WIDTHS)
    .map(width => Number(width))
    .filter(width => SMART_STYLE_WIDTHS.includes(width))
    .sort((a, b) => a - b)
    .map(width => `${buildLocalImageStyleUrl(normalized, width, format)} ${width}w`)
    .join(', ')
}

export function prewarmImageUrls(urls = [], limit = 3) {
  if (typeof window === 'undefined') {
    return
  }
  const queue = (Array.isArray(urls) ? urls : [])
    .map(url => resolveImageUrl(url))
    .filter(Boolean)
    .slice(0, Math.max(0, Number(limit) || 0))
  queue.forEach(url => {
    const image = new Image()
    image.decoding = 'async'
    image.src = url
  })
}

export function getLazyImageOptions(url, fallback = '') {
  return {
    src: resolveImageUrl(url, fallback),
    loading: lazyLoadingImage,
    error: resolveImageUrl(fallback) || lazyErrorImage
  }
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

  const shouldRetryWithCacheBust = isLocalFileUrl(originalSrc) && !originalSrc.startsWith(LOCAL_FILE_SEGMENT)

  if (shouldRetryWithCacheBust && retryCount < maxRetries) {
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
