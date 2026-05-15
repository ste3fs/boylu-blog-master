import request from '@/utils/request'

export function reportPerfApi(data: {
  eventType: 'article_detail' | 'upload'
  durationMs?: number
  success?: boolean
}) {
  return request({
    url: '/api/perf/report',
    method: 'post',
    data,
    timeout: 8000
  })
}
