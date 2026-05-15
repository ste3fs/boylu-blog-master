import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setupStore } from '@/store'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-notification.css'
import '@/styles/global.scss'


import { setupElIcons, setupPermission } from "@/plugins";
import ButtonGroup from '@/components/ButtonGroup/index.vue'
import permission from '@/directives/permission'

const app = createApp(App)

import SvgIcon from '@/components/SvgIcon/index.vue'
app.component('svg-icon', SvgIcon)

// 初始化权限
setupStore(app)

app.use(router)

app.component('ButtonGroup', ButtonGroup)

setupPermission()
setupElIcons(app)

app.directive('permission', permission)

app.mount('#app')
