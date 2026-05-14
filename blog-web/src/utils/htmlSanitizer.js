import DOMPurify from 'dompurify'

const SANITIZE_CONFIG = {
  USE_PROFILES: { html: true },
  ADD_TAGS: ['mention'],
  ADD_ATTR: ['target', 'data-src', 'data-origin', 'data-srcset', 'data-sizes', 'srcset', 'sizes', 'loading', 'decoding'],
  FORBID_ATTR: ['style'],
  FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed', 'form', 'input', 'button'],
  ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto|tel):|[^a-z]|[a-z+.-]+(?:[^a-z+.-:]|$))/i
}

const SAFE_LINK_PROTOCOLS = new Set(['http:', 'https:', 'mailto:', 'tel:'])

DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    node.setAttribute('target', '_blank')
    node.setAttribute('rel', 'noopener noreferrer')
  }
})

export function sanitizeHtml(html = '') {
  if (html === null || html === undefined) {
    return ''
  }

  return DOMPurify.sanitize(String(html), SANITIZE_CONFIG)
}

export function sanitizeCommentHtml(html = '') {
  return sanitizeHtml(html).trim()
}

export function sanitizeUrl(url = '', fallback = '#') {
  if (!url) {
    return fallback
  }

  const value = String(url).trim()
  if (!value) {
    return fallback
  }

  if (value.startsWith('/') || value.startsWith('#')) {
    return value
  }

  try {
    const parsed = new URL(value, window.location.origin)
    return SAFE_LINK_PROTOCOLS.has(parsed.protocol) ? value : fallback
  } catch (error) {
    return fallback
  }
}

export function escapeHtml(value = '') {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
