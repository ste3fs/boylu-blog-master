<template>
  <div id="app">
    <div v-if="!siteBootstrapped" class="app-shell-skeleton" aria-hidden="true">
      <div class="app-shell-skeleton__header"></div>
      <div class="app-shell-skeleton__hero"></div>
      <div class="app-shell-skeleton__cards">
        <span v-for="n in 3" :key="n" class="app-shell-skeleton__card"></span>
      </div>
    </div>
    <MobileMenu v-if="deferredUiReady" />
    <SearchDialog v-if="deferredUiReady" />
    <TheHeader v-if="showShellUi" />
    <router-view :class="{ 'main-container': showShellUi }" />
    <TheFooter v-if="showShellUi" />
    <FloatingButtons v-if="showShellUi && deferredUiReady" />
    <Lantern v-if="showShellUi && ambientUiReady" />
    <RandomVideo v-if="showShellUi && ambientUiReady" />
    <div class="cursor-container"></div>
    <ContextMenu v-if="deferredUiReady" ref="contextMenuRef" />
  </div>
</template>

<script>
import TheHeader from '@/layout/Header/index.vue'
import TheFooter from '@/layout/Footer/index.vue'
import { getWebConfigApi, reportApi,getNoticeApi } from '@/api/site'
import { mapActions } from 'vuex'
import { initTheme } from '@/utils/theme'
import { getCookie, getToken, removeCookie } from '@/utils/cookie'
import { setSeoMeta, setStructuredData } from '@/utils/seo'

export default {
  name: 'App',
  components: {
    TheHeader,
    TheFooter,
    FloatingButtons: () => import('@/components/common/FloatingButtons.vue'),
    SearchDialog: () => import('@/components/Search/index.vue'),
    MobileMenu: () => import('@/layout/MobileMenu/index.vue'),
    Lantern: () => import('@/components/Lanterns/index.vue'),
    RandomVideo: () => import('@/components/RandomVideo/index.vue'),
    ContextMenu: () => import('@/components/ContextMenu/index.vue'),
  },
  data() {
    return {
      deferredUiReady: false,
      ambientUiReady: false,
      siteBootstrapped: false
    }
  },

  computed: {
    showShellUi() {
      return !(this.$route && this.$route.meta && this.$route.meta.fullscreen)
    }
  },

  async created() {
    initTheme()
    this.initWebsiteSeo()
    await this.handleThirdPartyLogin()
    const url = this.resolveRedirectTarget(getCookie('redirectUrl'))
    if (url) {
      removeCookie('redirectUrl')
      this.$router.replace(url).catch(() => {})
    }
    this.bootstrapAppData()
  },
  methods: {
    ...mapActions(['setSiteInfo','getUserInfo']),
    scheduleIdleTask(callback, timeout = 1200) {
      if (typeof window !== 'undefined' && typeof window.requestIdleCallback === 'function') {
        window.requestIdleCallback(callback, { timeout })
        return
      }
      window.setTimeout(callback, Math.min(timeout, 1200))
    },
    bootstrapAppData() {
      getWebConfigApi()
        .then(res => {
          this.setSiteInfo(res.data)
          this.$store.commit('setVisitorAccess', res.extra.visitorCount)
          this.$store.commit('setSiteAccess', res.extra.blogViewsCount)
          this.$store.commit(
            'setDailyVisitorAccess',
            Object.prototype.hasOwnProperty.call(res.extra || {}, 'dailyVisitorCount')
              ? res.extra.dailyVisitorCount
              : null
          )
          this.$store.commit(
            'setDailySiteAccess',
            Object.prototype.hasOwnProperty.call(res.extra || {}, 'dailyBlogViewsCount')
              ? res.extra.dailyBlogViewsCount
              : null
          )
          this.initWebsiteSeo(res.data)
          this.siteBootstrapped = true
        })
        .catch(() => {
          this.siteBootstrapped = true
        })

      getNoticeApi()
        .then(noticeRes => {
          this.$store.commit('SET_NOTICE', noticeRes.data)
        })
        .catch(() => {})

      this.scheduleIdleTask(() => {
        reportApi().catch(() => {})
        if (getToken()) {
          this.getUserInfo().catch(() => {})
        }
      }, 1800)
    },
    initWebsiteSeo(siteInfo = {}) {
      const title = siteInfo.name || 'boylu博客-一个专注于技术分享的博客平台'
      const description = siteInfo.authorInfo || 'boylu 的个人技术博客，记录开发实践与学习笔记'
      const keywords = siteInfo.keyword || siteInfo.keywords || 'boylu博客,技术博客,编程学习'
      setSeoMeta({
        title,
        description,
        keywords,
        canonicalUrl: window.location.pathname + window.location.search
      })
      setStructuredData('schema-website', {
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: siteInfo.name || 'boylu博客',
        url: window.location.origin,
        description
      })
      setStructuredData('schema-person', {
        '@context': 'https://schema.org',
        '@type': 'Person',
        name: siteInfo.author || 'boylu',
        url: window.location.origin,
        image: siteInfo.authorAvatar || `${window.location.origin}/boylu-avatar.jpg`
      })
    },

    /**
     * 处理第三方登录用回调逻辑
     */
     async handleThirdPartyLogin() {
      const url = new URL(window.location.href)
      const token = url.searchParams.get('token')
      if (token) {
        this.$store.commit('SET_TOKEN', token)
        url.searchParams.delete('token')
        window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`)
      }
    },

    resolveRedirectTarget(redirect) {
      const target = (redirect || '').toString().trim()
      if (!target || target === '/login') {
        return ''
      }

      try {
        const url = new URL(target, window.location.origin)
        if (url.origin !== window.location.origin) {
          return ''
        }
        return `${url.pathname}${url.search}${url.hash}` || '/'
      } catch (error) {
        return ''
      }
    },

    /**
     * 初始化鼠标点击效果
     */
    initCursorEffect() {
      const container = document.querySelector('.cursor-container')
      
      document.addEventListener('click', (e) => {
        const cursor = document.createElement('div')
        cursor.className = 'cursor-fx'
        cursor.style.left = `${e.clientX}px`
        cursor.style.top = `${e.clientY}px`
        container.appendChild(cursor)
        
        cursor.addEventListener('animationend', () => {
          cursor.remove()
        })
      })
    },

    initContextMenu() {
      if (!this.$refs.contextMenuRef) {
        return
      }
      const handleContextMenu = (e) => {
        this.$refs.contextMenuRef.show(e)
      }

      const handleClick = () => {
        this.$refs.contextMenuRef.hide()
      }

      document.addEventListener('contextmenu', handleContextMenu)
      document.addEventListener('click', handleClick)

      // 在组件销毁时移除事件监听
      this.$once('hook:beforeDestroy', () => {
        document.removeEventListener('contextmenu', handleContextMenu)
        document.removeEventListener('click', handleClick)
      })
    }
  },
  mounted() {
    this.scheduleIdleTask(() => {
      this.deferredUiReady = true
      this.$nextTick(() => {
        this.initContextMenu()
      })
    }, 900)
    this.scheduleIdleTask(() => {
      this.ambientUiReady = true
      this.initCursorEffect()
    }, 2200)
  }
}
</script>

<style lang="scss">
* {
  margin: 0;
  padding: 0;
  font-family: var(--font-sans);
  box-sizing: border-box;
}

.app-shell-skeleton {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
}

.app-shell-skeleton__header,
.app-shell-skeleton__hero,
.app-shell-skeleton__card {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08) 25%, rgba(64, 158, 255, 0.18) 37%, rgba(64, 158, 255, 0.08) 63%);
  background-size: 400% 100%;
  animation: appSkeletonShimmer 1.4s ease infinite;
  border-radius: 14px;
}

.app-shell-skeleton__header {
  height: 64px;
  margin-bottom: 16px;
}

.app-shell-skeleton__hero {
  height: 220px;
  margin-bottom: 16px;
}

.app-shell-skeleton__cards {
  display: grid;
  gap: 16px;
}

.app-shell-skeleton__card {
  display: block;
  height: 140px;
}

@keyframes appSkeletonShimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style> 
