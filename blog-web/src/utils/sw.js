export function registerServiceWorker() {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator) || import.meta.env.DEV) {
    return
  }

  const register = () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {})
  }

  if (typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(register, { timeout: 2000 })
    return
  }

  window.setTimeout(register, 1200)
}
