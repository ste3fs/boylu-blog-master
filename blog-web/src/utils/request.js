import axios from 'axios'
import { getToken, removeToken } from '@/utils/cookie'
import store from '@/store'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API,
  timeout: 30000
})

function buildLoginRedirect() {
  const currentPath = router.currentRoute?.fullPath || '/'
  if (currentPath.startsWith('/login')) {
    return { path: '/login' }
  }

  return {
    path: '/login',
    query: {
      redirect: currentPath || '/'
    }
  }
}

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      return Promise.reject(new Error('请求超时，请稍后重试'))
    }
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const res = response.data

    if (res.code === 200) {
      return res
    }

    if (res.code === 404) {
      return Promise.reject(new Error('请求路径不存在'))
    }

    if (res.code === 401) {
      removeToken()
      store.commit('SET_USER_INFO', null)
      router.push(buildLoginRedirect())
      return Promise.reject(new Error('当前登录已过期，请重新登录'))
    }

    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => Promise.reject(error)
)

export default service
