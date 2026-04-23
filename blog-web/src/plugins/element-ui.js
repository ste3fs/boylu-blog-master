import Alert from 'element-ui/lib/alert'
import Avatar from 'element-ui/lib/avatar'
import Badge from 'element-ui/lib/badge'
import Button from 'element-ui/lib/button'
import Card from 'element-ui/lib/card'
import Carousel from 'element-ui/lib/carousel'
import CarouselItem from 'element-ui/lib/carousel-item'
import Checkbox from 'element-ui/lib/checkbox'
import Col from 'element-ui/lib/col'
import Descriptions from 'element-ui/lib/descriptions'
import DescriptionsItem from 'element-ui/lib/descriptions-item'
import Dialog from 'element-ui/lib/dialog'
import Drawer from 'element-ui/lib/drawer'
import Empty from 'element-ui/lib/empty'
import Form from 'element-ui/lib/form'
import FormItem from 'element-ui/lib/form-item'
import Input from 'element-ui/lib/input'
import Link from 'element-ui/lib/link'
import Menu from 'element-ui/lib/menu'
import MenuItem from 'element-ui/lib/menu-item'
import Message from 'element-ui/lib/message'
import MessageBox from 'element-ui/lib/message-box'
import Option from 'element-ui/lib/option'
import Pagination from 'element-ui/lib/pagination'
import Radio from 'element-ui/lib/radio'
import RadioGroup from 'element-ui/lib/radio-group'
import Row from 'element-ui/lib/row'
import Select from 'element-ui/lib/select'
import Switch from 'element-ui/lib/switch'
import TabPane from 'element-ui/lib/tab-pane'
import Tabs from 'element-ui/lib/tabs'
import Tag from 'element-ui/lib/tag'
import Tooltip from 'element-ui/lib/tooltip'
import Upload from 'element-ui/lib/upload'

import 'element-ui/lib/theme-chalk/base.css'
import 'element-ui/lib/theme-chalk/icon.css'
import 'element-ui/lib/theme-chalk/popper.css'
import 'element-ui/lib/theme-chalk/alert.css'
import 'element-ui/lib/theme-chalk/avatar.css'
import 'element-ui/lib/theme-chalk/badge.css'
import 'element-ui/lib/theme-chalk/button.css'
import 'element-ui/lib/theme-chalk/card.css'
import 'element-ui/lib/theme-chalk/carousel.css'
import 'element-ui/lib/theme-chalk/carousel-item.css'
import 'element-ui/lib/theme-chalk/checkbox.css'
import 'element-ui/lib/theme-chalk/col.css'
import 'element-ui/lib/theme-chalk/descriptions.css'
import 'element-ui/lib/theme-chalk/descriptions-item.css'
import 'element-ui/lib/theme-chalk/dialog.css'
import 'element-ui/lib/theme-chalk/drawer.css'
import 'element-ui/lib/theme-chalk/empty.css'
import 'element-ui/lib/theme-chalk/form.css'
import 'element-ui/lib/theme-chalk/form-item.css'
import 'element-ui/lib/theme-chalk/input.css'
import 'element-ui/lib/theme-chalk/link.css'
import 'element-ui/lib/theme-chalk/menu.css'
import 'element-ui/lib/theme-chalk/menu-item.css'
import 'element-ui/lib/theme-chalk/message.css'
import 'element-ui/lib/theme-chalk/message-box.css'
import 'element-ui/lib/theme-chalk/option.css'
import 'element-ui/lib/theme-chalk/pagination.css'
import 'element-ui/lib/theme-chalk/radio.css'
import 'element-ui/lib/theme-chalk/radio-group.css'
import 'element-ui/lib/theme-chalk/row.css'
import 'element-ui/lib/theme-chalk/select.css'
import 'element-ui/lib/theme-chalk/select-dropdown.css'
import 'element-ui/lib/theme-chalk/switch.css'
import 'element-ui/lib/theme-chalk/tab-pane.css'
import 'element-ui/lib/theme-chalk/tabs.css'
import 'element-ui/lib/theme-chalk/tag.css'
import 'element-ui/lib/theme-chalk/tooltip.css'
import 'element-ui/lib/theme-chalk/upload.css'

const components = [
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Carousel,
  CarouselItem,
  Checkbox,
  Col,
  Descriptions,
  DescriptionsItem,
  Dialog,
  Drawer,
  Empty,
  Form,
  FormItem,
  Input,
  Link,
  Menu,
  MenuItem,
  Option,
  Pagination,
  Radio,
  RadioGroup,
  Row,
  Select,
  Switch,
  TabPane,
  Tabs,
  Tag,
  Tooltip,
  Upload
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
