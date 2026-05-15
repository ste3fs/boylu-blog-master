const STATIC_CACHE = 'boylu-static-v1'
const PAGE_CACHE = 'boylu-pages-v1'
const API_CACHE = 'boylu-api-v1'
const CORE_ASSETS = ['/', '/index.html', '/boylu-avatar.jpg', '/boylu-logo.png']

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then(cache => cache.addAll(CORE_ASSETS)).catch(() => {})
  )
  self.skipWaiting()
})

self.addEventListener('activate', event => {
  const validCaches = [STATIC_CACHE, PAGE_CACHE, API_CACHE]
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(key => !validCaches.includes(key)).map(key => caches.delete(key)))
    ).then(() => self.clients.claim())
  )
})

function isHtmlRequest(request) {
  return request.mode === 'navigate' || (request.headers.get('accept') || '').includes('text/html')
}

function isCacheableArticleApi(url) {
  return /\/(boylu|mojian)\/api\/article\/(detail\/\d+|home-list)/.test(url.pathname + url.search)
}

async function staleWhileRevalidate(request, cacheName) {
  const cache = await caches.open(cacheName)
  const cached = await cache.match(request)
  const networkPromise = fetch(request)
    .then(response => {
      if (response && response.ok) {
        cache.put(request, response.clone())
      }
      return response
    })
    .catch(() => cached)

  return cached || networkPromise
}

self.addEventListener('fetch', event => {
  const { request } = event
  if (request.method !== 'GET') {
    return
  }

  const url = new URL(request.url)
  if (url.origin !== self.location.origin) {
    return
  }

  if (request.destination === 'script' || request.destination === 'style' || request.destination === 'font' || request.destination === 'image') {
    event.respondWith(staleWhileRevalidate(request, STATIC_CACHE))
    return
  }

  if (isCacheableArticleApi(url)) {
    event.respondWith(staleWhileRevalidate(request, API_CACHE))
    return
  }

  if (isHtmlRequest(request)) {
    event.respondWith(
      fetch(request)
        .then(response => {
          if (response && response.ok) {
            caches.open(PAGE_CACHE).then(cache => cache.put(request, response.clone()))
          }
          return response
        })
        .catch(() => caches.match(request).then(match => match || caches.match('/index.html')))
    )
  }
})
