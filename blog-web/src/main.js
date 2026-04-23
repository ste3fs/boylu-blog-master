import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import '@/styles/global.scss'
import '@/styles/element-ui.scss'
import gsap from 'gsap'
import 'animate.css'
import VueLazyload from 'vue-lazyload'
import { setupElementUI } from '@/plugins/element-ui'

import ScrollTrigger from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

const lazyErrorImage = new URL('./assets/img-error.svg', import.meta.url).href
const lazyLoadingImage = new URL('./assets/loading.svg', import.meta.url).href

// 配置 vue-lazyload
Vue.use(VueLazyload, {
  preLoad: 1.3,
  error: lazyErrorImage,
  loading: lazyLoadingImage,
  attempt: 1,
  observer: true,
  observerOptions: {
    rootMargin: '0px',
    threshold: 0.1
  }
})

setupElementUI(Vue)


//表情组件
import EmojiPicker from '@/components/common/EmojiPicker.vue'
Vue.component('mj-emoji', EmojiPicker)



import ClickOutside from '@/directives/clickOutside'
Vue.directive('click-outside', ClickOutside)

//加载组件
import loading from './directives/loading'
Vue.directive('loading', loading)

//高亮
import 'highlight.js/styles/atom-one-dark.css'
import { animateOnScroll } from './directives/animate'
Vue.directive('animate-on-scroll', animateOnScroll)

//图片预览组件
import ImagePreview from '@/components/common/ImagePreview.vue'
Vue.component('mj-image-preview', ImagePreview)



// 为了支持 HMR
if (import.meta.hot) {
  import.meta.hot.accept()
}

Vue.config.productionTip = false

import 'virtual:svg-icons-register'

// 注册全局组件
import SvgIcon from '@/components/SvgIcon/index.vue'
Vue.component('svg-icon', SvgIcon)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app') 
