import request from '@/utils/request'

// 获取文章列表
export function getArticleListApi(params: any) {
  return request({
    url: '/sys/article/list',
    method: 'get',
    params
  })
}

// 获取文章详情
export function getDetailApi(id: any) {
  return request({
    url: `/sys/article/detail/${id}`,
    method: 'get',
  })
}

// 新增文章
export function addArticleApi(data: any) {
  return request({
    url: '/sys/article/add',
    method: 'post',
    timeout: 60000,
    data
  })
}

// 修改文章
export function updateArticleApi(data: any) {
  return request({
    url: '/sys/article/update',
    method: 'put',
    timeout: 60000,
    data
  })
}


// 修改文章状态
export function updateStatusApi(data: any) {
  return request({
    url: '/sys/article/updateStatus',
    method: 'put',
    data
  })
}

// 推送文章到百度
export function pushBaiduApi(id: any) {
  return request({
    url: `/sys/article/pushBaidu/${id}`,
    method: 'post'
  })
}

// 批量推送最近文章到百度
export function pushBaiduRecentApi() {
  return request({
    url: `/sys/article/pushBaiduRecent`,
    method: 'post'
  })
}
// 删除文章
export function deleteArticleApi(ids: any) {
  return request({
    url: `/sys/article/delete/${ids}`,
    method: 'delete'
  })
}

// 爬取文章
export function reptileArticleApi(url: any) {
  return request({
    url: '/sys/article/reptile',
    method: 'get',
    params: {url: url}
  })
}

// 导入 Notion 笔记为文章草稿
export function importNotionArticleApi(data: any) {
  return request({
    url: '/sys/article/import/notion',
    method: 'post',
    timeout: 120000,
    data
  })
}

export function syncNotionArticleApi(id: any) {
  return request({
    url: `/sys/article/sync/notion/${id}`,
    method: 'post',
    timeout: 120000
  })
}

export function getNotionSyncLogsApi(id: any) {
  return request({
    url: id ? `/sys/article/notion/logs/${id}` : '/sys/article/notion/logs',
    method: 'get'
  })
}
