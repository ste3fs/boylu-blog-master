import Alert from 'element-ui/lib/alert'
import Avatar from 'element-ui/lib/avatar'
import Button from 'element-ui/lib/button'
import Card from 'element-ui/lib/card'
import Dialog from 'element-ui/lib/dialog'
import Empty from 'element-ui/lib/empty'
import Form from 'element-ui/lib/form'
import FormItem from 'element-ui/lib/form-item'
import Input from 'element-ui/lib/input'
import Message from 'element-ui/lib/message'
import MessageBox from 'element-ui/lib/message-box'
import Pagination from 'element-ui/lib/pagination'

import 'element-ui/lib/theme-chalk/base.css'
import 'element-ui/lib/theme-chalk/icon.css'
import 'element-ui/lib/theme-chalk/popper.css'
import 'element-ui/lib/theme-chalk/avatar.css'
import 'element-ui/lib/theme-chalk/button.css'
import 'element-ui/lib/theme-chalk/card.css'
import 'element-ui/lib/theme-chalk/dialog.css'
import 'element-ui/lib/theme-chalk/empty.css'
import 'element-ui/lib/theme-chalk/form.css'
import 'element-ui/lib/theme-chalk/form-item.css'
import 'element-ui/lib/theme-chalk/input.css'
import 'element-ui/lib/theme-chalk/message.css'
import 'element-ui/lib/theme-chalk/message-box.css'
import 'element-ui/lib/theme-chalk/pagination.css'

const components = [
  Avatar,
  Button,
  Card,
  Dialog,
  Empty,
  Form,
  FormItem,
  Input,
  Pagination,
]

export function setupElementUI(Vue) {
  components.forEach((component) => {
    Vue.use(component)
  })

  Vue.prototype.$message = Message
  Vue.prototype.$msgbox = MessageBox
  Vue.prototype.$alert = MessageBox.alert
  Vue.prototype.$confirm = MessageBox.confirm
  Vue.prototype.$prompt = MessageBox.prompt
}
