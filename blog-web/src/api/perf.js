import request from '@/utils/request'

export function reportPerfApi(data) {
  return request({
    url: '/api/perf/report',
    method: 'post',
    data,
    timeout: 8000
  })
}
