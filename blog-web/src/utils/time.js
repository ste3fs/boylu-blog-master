function toDate(time) {
  const date = time instanceof Date ? time : new Date(time)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value) {
  return String(value).padStart(2, '0')
}

function formatMonthDay(date) {
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatDate(time) {
  const date = toDate(time)
  if (!date) {
    return ''
  }

  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatDateTime(time, options = {}) {
  const date = toDate(time)
  if (!date) {
    return ''
  }

  const { withSeconds = false } = options
  const base = `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
  return withSeconds ? `${base}:${pad(date.getSeconds())}` : base
}

export function formatTime(time) {
  const date = toDate(time)
  if (!date) {
    return ''
  }

  const now = new Date()
  const diff = (now - date) / 1000

  if (diff < 60) {
    return '刚刚'
  }
  if (diff < 3600) {
    return `${Math.floor(diff / 60)}分钟前`
  }
  if (diff < 86400) {
    return `${Math.floor(diff / 3600)}小时前`
  }
  if (diff < 2592000) {
    return `${Math.floor(diff / 86400)}天前`
  }
  if (diff < 31536000) {
    return `${Math.floor(diff / 2592000)}个月前`
  }
  return formatDate(date)
}

export function formatPublishTime(time, options = {}) {
  const date = toDate(time)
  if (!date) {
    return ''
  }

  const { maxRelativeDays = 5 } = options
  const now = new Date()
  const diff = (now - date) / 1000

  if (diff < Math.max(1, Number(maxRelativeDays) || 5) * 86400) {
    return formatTime(date)
  }

  return formatDate(date)
}

export function getRelativeTime(time) {
  const date = toDate(time)
  if (!date) {
    return ''
  }

  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today.getTime() - 86400000)
  const thisYear = new Date(now.getFullYear(), 0, 1)

  if (date >= today) {
    return formatTime(date)
  }
  if (date >= yesterday) {
    return '昨天'
  }
  if (date >= thisYear) {
    return formatMonthDay(date)
  }
  return formatDate(date)
}
