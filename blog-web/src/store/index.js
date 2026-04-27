import Vue from 'vue'
import Vuex from 'vuex'
import { loginApi,logoutApi,getUserInfoApi } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/cookie'

Vue.use(Vuex)

function getCachedUserInfo() {
  const cachedUser = sessionStorage.getItem("user")
  if (!cachedUser) {
    return null
  }

  try {
    return JSON.parse(cachedUser)
  } catch (error) {
    sessionStorage.removeItem("user")
    return null
  }
}

export default new Vuex.Store({
  state: {
    userInfo: getCachedUserInfo(),
    defaultImage: '/boylu-logo.png',
    imageVersion: Date.now(),
    webSiteInfo: {
      logo: '/boylu-logo.png',
      name: 'boylu博客',
      author: 'boylu',
      authorInfo: '专注技术分享与实践记录。',
      authorAvatar: '/boylu-avatar.jpg',
      showList:[]
    },
    token: getToken() || '',
    searchVisible: false,
    mobileMenuVisible: false,
    visitorAccess: 0,
    siteAccess: 0,
    dailyVisitorAccess: null,
    dailySiteAccess: null,
    isLoading: false,
    notice: null,
    isUnread: false
  },
  mutations: {
    setSiteInfo(state, info) {
      state.webSiteInfo = info
    },
    SET_TOKEN(state, payload) {
      const token = typeof payload === 'string' ? payload : payload?.token
      const expires = typeof payload === 'object' && payload?.expires ? payload.expires : 7
      state.token = token || ''
      if (token) {
        setToken(token, expires)
      } else {
        removeToken()
      }
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      if (userInfo) {
        sessionStorage.setItem("user", JSON.stringify(userInfo))
      } else {
        sessionStorage.removeItem("user")
      }
    },

    SET_SEARCH_VISIBLE(state, visible) {
      state.searchVisible = visible
    },
    SET_MOBILE_MENU_VISIBLE(state, visible) {
      state.mobileMenuVisible = visible
    },
    setMobileMenuVisible(state, value) {
      state.mobileMenuVisible = value
    },
    setVisitorAccess(state, value) {
      state.visitorAccess = value
    },
    setSiteAccess(state, value) {
      state.siteAccess = value
    },
    setDailyVisitorAccess(state, value) {
      state.dailyVisitorAccess = value
    },
    setDailySiteAccess(state, value) {
      state.dailySiteAccess = value
    },
    SET_LOADING(state, status) {
      state.isLoading = status
    },
    SET_NOTICE(state, notice) {
      state.notice = notice
    }
  },
  actions: {
    
    /**
     * 设置公告信息
     */
    setNotice({ commit }, notice) {
      commit('SET_NOTICE', notice)
    },

    /**
     * 设置站点信息
     */
    setSiteInfo({ commit }, info) {
      commit('setSiteInfo', info)
    },
    /**
     * 获取用户信息
     */
    async getUserInfo({ commit }) {
      if(getToken()){
        const res = await getUserInfoApi()
        commit('SET_USER_INFO', res.data)
      }
    },

    /**
     * 登录
     */
    async loginAction({ commit }, payload) {
      const loginData = payload?.loginData || payload
      const rememberMe = !!payload?.rememberMe
      try {
        const res = await loginApi(loginData)
        if (res.data) {
          commit('SET_TOKEN', { token: res.data.token, expires: rememberMe ? 30 : 7 })
          commit('SET_USER_INFO', res.data)
          return Promise.resolve(res)
        }
       return Promise.reject(res)
      } catch (error) {
        return Promise.reject(error)
      }
    },

    /**
     * 退出登录
     */
    async logout({ commit }) {
      await logoutApi()
      removeToken()
      commit('SET_USER_INFO', null)
    },

    showLoading({ commit }) {
      commit('SET_LOADING', true)
    },

    hideLoading({ commit }) {
      commit('SET_LOADING', false)
    }
  }
})
