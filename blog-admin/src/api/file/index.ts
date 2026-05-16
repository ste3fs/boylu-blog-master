import request from '@/utils/request'

// 获取文件列表
export function getFileListApi(params: any) {
  return request({
    url: '/file/list',
    method: 'get',
    params
  })
}

// 上传文件
export function uploadApi(data: any, source: string, config: Record<string, any> = {}) {
  return request({
    url: '/file/upload',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 180000,
    data,
    params: { source },
    ...config
  })
}

export function initChunkUploadApi(params: {
  fileName: string
  totalSize: number
  totalChunks: number
  fileHash?: string
  source?: string
}) {
  return request({
    url: '/file/upload/chunk/init',
    method: 'post',
    params
  })
}

export function uploadChunkPartApi(data: FormData, config: Record<string, any> = {}) {
  return request({
    url: '/file/upload/chunk/part',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
    data,
    ...config
  })
}

export function completeChunkUploadApi(uploadId: string) {
  return request({
    url: '/file/upload/chunk/complete',
    method: 'post',
    timeout: 180000,
    params: { uploadId }
  })
}

export function abortChunkUploadApi(uploadId: string) {
  return request({
    url: '/file/upload/chunk/abort',
    method: 'post',
    params: { uploadId }
  })
}

// 删除文件，支持 /boylu/file/content/{id}、/file/view/{id} 和旧 URL。
export function deleteFileApi(url: string) {
  return request({
    url: '/file/delete',
    method: 'get',
    params: { url }
  })
}

// 获取存储配置
export function getOssConfigApi() {
  return request({
    url: '/file/getOssConfig',
    method: 'get'
  })
}

// 添加存储配置
export function addOssApi(data: any) {
  return request({
    url: '/file/addOss',
    method: 'post',
    data
  })
}

// 更新存储配置
export function updateOssApi(data: any) {
  return request({
    url: '/file/updateOss',
    method: 'put',
    data
  })
}
