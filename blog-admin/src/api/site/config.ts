import request from '@/utils/request'

// 获取网站配置信息
export function getWebConfigApi() {
  return request({
    url: '/sys/web/config',
    method: 'get'
  })
}

// 保存网站配置信息
export function updateWebConfigApi(data: any) {
  return request({
    url: '/sys/web/update',
    method: 'put',
    data
  })
}

export function getAiConfigApi() {
  return request({
    url: '/sys/ai/config',
    method: 'get'
  })
}

export function updateAiRuntimeConfigApi(data: any) {
  return request({
    url: '/sys/ai/config/runtime',
    method: 'put',
    data
  })
}

export function updateAiTrainingConfigApi(data: any) {
  return request({
    url: '/sys/ai/config/training',
    method: 'put',
    data
  })
}

export function chatAiTrainingApi(data: any) {
  return request({
    url: '/sys/ai/config/training/chat',
    method: 'post',
    timeout: 120000,
    data
  })
}
