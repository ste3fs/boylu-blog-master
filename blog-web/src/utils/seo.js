function ensureMetaByName(name) {
  let el = document.head.querySelector(`meta[name="${name}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('name', name)
    document.head.appendChild(el)
  }
  return el
}

function ensureMetaByProperty(property) {
  let el = document.head.querySelector(`meta[property="${property}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute('property', property)
    document.head.appendChild(el)
  }
  return el
}

function ensureCanonicalLink() {
  let link = document.head.querySelector('link[rel="canonical"]')
  if (!link) {
    link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.appendChild(link)
  }
  return link
}

function toAbsoluteUrl(pathOrUrl) {
  try {
    return new URL(pathOrUrl, window.location.origin).toString()
  } catch (error) {
    return window.location.href
  }
}

export function setSeoMeta({
  title,
  description,
  keywords,
  canonicalUrl,
  image,
  type = 'website'
}) {
  if (title) {
    document.title = title
  }

  const desc = (description || '').trim()
  const kw = Array.isArray(keywords) ? keywords.join(',') : String(keywords || '').trim()
  const canonical = toAbsoluteUrl(canonicalUrl || window.location.pathname + window.location.search)
  const imageUrl = image ? toAbsoluteUrl(image) : ''

  ensureMetaByName('description').setAttribute('content', desc)
  ensureMetaByName('keywords').setAttribute('content', kw)
  ensureMetaByName('robots').setAttribute('content', 'index,follow,max-snippet:-1,max-image-preview:large,max-video-preview:-1')
  ensureCanonicalLink().setAttribute('href', canonical)

  ensureMetaByProperty('og:type').setAttribute('content', type)
  ensureMetaByProperty('og:title').setAttribute('content', title || document.title)
  ensureMetaByProperty('og:description').setAttribute('content', desc)
  ensureMetaByProperty('og:url').setAttribute('content', canonical)
  ensureMetaByProperty('og:site_name').setAttribute('content', 'boylu博客')
  if (imageUrl) {
    ensureMetaByProperty('og:image').setAttribute('content', imageUrl)
  }

  ensureMetaByName('twitter:card').setAttribute('content', imageUrl ? 'summary_large_image' : 'summary')
  ensureMetaByName('twitter:title').setAttribute('content', title || document.title)
  ensureMetaByName('twitter:description').setAttribute('content', desc)
  if (imageUrl) {
    ensureMetaByName('twitter:image').setAttribute('content', imageUrl)
  }
}

export function setStructuredData(id, jsonData) {
  if (!id) {
    return
  }
  removeStructuredData(id)
  const script = document.createElement('script')
  script.type = 'application/ld+json'
  script.id = id
  script.textContent = JSON.stringify(jsonData)
  document.head.appendChild(script)
}

export function removeStructuredData(id) {
  if (!id) {
    return
  }
  const node = document.getElementById(id)
  if (node && node.parentNode) {
    node.parentNode.removeChild(node)
  }
}
