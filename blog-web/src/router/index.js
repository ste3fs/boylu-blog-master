import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '@/store';
import { getToken } from '@/utils/cookie'
import { setSeoMeta } from '@/utils/seo'

Vue.use(VueRouter)

const Layout = () => import('@/layout/index.vue')
const Home = () => import('@/views/home/index.vue')
const NotFound = () => import('@/views/404/404.vue')
const Article = () => import('@/views/article/index.vue')
const Archive = () => import('@/views/archives/index.vue')
const Categories = () => import('@/views/categories/index.vue')
const Tags = () => import('@/views/tags/index.vue')
const Messages = () => import('@/views/messages/index.vue')
const About = () => import('@/views/about/index.vue')
const Photos = () => import('@/views/photos/index.vue')

const routePrefetchMap = {
  '/': [() => import('@/components/ArticleList/index.vue')],
  '/article/:id': [
    () => import('@/components/Comment/index.vue'),
    () => import('@/utils/markdown')
  ]
}

const routes = [

    {
        path: "/",
        component: Layout,
        meta: {
            title: "boylu博客-一个专注于技术分享的博客平台",
            loading: true
        },
        children: [
            {
                path: '/',
                name: 'Home',
                component: Home,
                meta: {
                    title: '首页',
                    transition: 'fade',
                    icon: 'fas fa-home',
                    loading: true
                 }
              },
              {
                path: '/archive',
                name: 'Archive',
                component: Archive,
                meta: { 
                  transition: 'fade',
                  title: '归档 - boylu博客',
                  icon: 'fas fa-archive'
                }
              },
              {
                path: '/categories',
                name: 'Categories',
                component: Categories,
                meta: {
                    transition: 'fade',
                    title: "分类 - boylu博客",
                    icon: 'fas fa-folder'
                 }
              },
              {
                path: '/tags',
                name: 'Tags',
                component: Tags,
                meta: {
                    transition: 'fade',
                    title: '标签 - boylu博客',
                    icon: 'fas fa-tags'
                }
              },
              {
                path: '/moments',
                name: 'Moments',
                component: () => import('@/views/moments/index.vue'),
                meta: {
                  title: '说说 - boylu博客',
                  icon: 'fas fa-comment-dots'
                }
              },
              {
                path: '/photos',
                name: 'Photos',
                component: Photos,
                meta: {
                    transition: 'fade',
                    title: '相册 - boylu博客',
                    icon: 'fas fa-images'
                }
              },
              {
                path: '/photos/:id',
                name: 'PhotoDetail',
                component: () => import('@/views/photos/detail.vue'),
                meta: {
                    transition: 'fade',
                    title: '相册详情 - boylu博客',
                    icon: 'fas fa-images',
                    hidden: true
                }
              },
              {
                path: '/hotSearch',
                name: 'HotSearch',
                component: () => import(/* webpackPrefetch: true */ '@/views/hotSearch/index.vue'),
                meta: { 
                  transition: 'fade',
                  title: '热搜 - boylu博客',
                  icon: 'fas fa-fire'
                }
              },
              {
                path: '/resources',
                name: 'Resources',
                component: () => import('@/views/resources/index.vue'),
                meta: {
                  title: '资源',
                  icon: 'fas fa-cloud-download-alt'

                }
              },
              {
                path: '/messages',
                name: 'Messages',
                component: Messages,
                meta: { 
                  transition: 'fade',
                  title: '留言板 - boylu博客',
                  icon: 'fas fa-comments'
                }
              },
              {
                path: '/friends',
                name: 'Friends',
                component: () => import(/* webpackPrefetch: true */ '@/views/friends/index.vue'),
                meta: { 
                  transition: 'fade',
                  title: '友情链接 - boylu博客',
                  icon: 'fas fa-user-friends'
                }
              },
              {
                path: '/about',
                name: 'About',
                component: About,
                meta: { 
                  transition: 'fade',
                  title: '关于本站 - boylu博客',
                  icon: 'fas fa-info-circle'
                }
              },
              {
                path: '/post/:id',
                redirect: to => `/article/${to.params.id}`
              },
              {
                path: '/article/:id',
                name: 'ArticleDetail',
                component: Article,
                props: true,
                meta: {
                  title: '文章详情 - boylu博客',
                  description: '阅读 boylu 博客的技术文章，涵盖前端、后端、运维与实战经验。',
                  keywords: '技术博客,编程,前端,后端,运维,教程',
                  hidden: true
                }
              },
              {
                path: '/user/profile',
                name: 'Profile',
                component: () => import(/* webpackPrefetch: true */ '@/views/profile/index.vue'),
                meta: {
                  title: '个人主页 - boylu博客',
                  icon: 'fas fa-user',
                  hidden: true
                }
              },
              {
                path: '/editor',
                name: 'Editor',
                component: () => import(/* webpackPrefetch: true */ '@/views/editor/index.vue'),
                meta: {
                  title: '写文章 - boylu博客',
                  icon: 'fas fa-edit',
                  requireAuth: true,
                  hidden: true
                }
              },
              {
                path: '/chat',
                name: 'Chat',
                component: () => import(/* webpackPrefetch: true */ '@/views/chat/index.vue'),
                meta: {
                  title: '聊天 - boylu博客',
                  icon: 'fas fa-comments',
                  hidden: true
                }
              },
              {
                path: '/ai',
                name: 'AiAssistant',
                component: () => import('@/views/ai/index.vue'),
                meta: {
                  transition: 'fade',
                  title: 'AI 助手 - boylu博客',
                  icon: 'fas fa-robot',
                  fullscreen: true
                }
              }, {
                path: '/login',
                name: 'Login',
                component: () => import('@/views/login/index.vue'),
                meta: {
                  title: '登录',
                  hidden: true,
                  fullscreen: true
                }
              },
              {
                path: '/notifications',
                name: 'Notifications',
                component: () => import('@/views/notifications/index.vue'),
                meta: {
                  title: '消息通知',
                  requireAuth: true,
                  hidden: true
                }
              },
              {
                path: '/:pathMatch(.*)*',
                name: 'NotFound',
                component: NotFound,
                meta: {
                  hidden: true
                }
              }
        ]
    }
]

const router = new VueRouter({
  mode: 'history',
  base: '/',
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    return { x: 0, y: 0 }
  }
})


// 解决重复点击导航时，控制台出现报错
const VueRouterPush = VueRouter.prototype.push
VueRouter.prototype.push = function push (to) {
  return VueRouterPush.call(this, to).catch(err => err)
}


router.beforeEach((to, from, next) => {
  const title = to.meta.title || 'boylu博客-一个专注于技术分享的博客平台'
  setSeoMeta({
    title,
    description: to.meta.description || 'boylu 的个人技术博客，记录开发实践与学习笔记',
    keywords: to.meta.keywords || 'boylu博客,技术博客,编程学习',
    canonicalUrl: to.fullPath || to.path
  })
  //关闭搜索框
  store.commit('SET_SEARCH_VISIBLE', false)

  if (to.meta.requireAuth && !getToken()) {
    next({
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    })
    return
  }

  next()
})

router.afterEach((to) => {
  if (typeof window === 'undefined') {
    return
  }
  const key = to.matched && to.matched.some(item => item.path === '/article/:id')
    ? '/article/:id'
    : to.path
  const loaders = routePrefetchMap[key] || []
  if (!loaders.length) {
    return
  }
  const runner = () => loaders.forEach(load => load().catch(() => {}))
  if (typeof window.requestIdleCallback === 'function') {
    window.requestIdleCallback(runner, { timeout: 1500 })
    return
  }
  window.setTimeout(runner, 900)
})

export default router 
