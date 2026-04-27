<template>
  <div class="app-container site-config-page">
    <el-card class="site-config-card">
      <el-tabs v-model="activeTab" class="site-config-tabs">
        <el-tab-pane name="basic">
          <template #label>
            <el-icon><Setting /></el-icon>
            <span class="tab-label">基本信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-width="formLabelWidth"
            :label-position="formLabelPosition"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="网站 Logo" prop="logo">
                  <UploadImage v-model="form.logo" :limit="1" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="网站名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入网站名称" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="网站介绍" prop="summary">
                  <el-input v-model="form.summary" type="textarea" :rows="4" placeholder="请输入网站介绍" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="备案号" prop="recordNum">
                  <el-input v-model="form.recordNum" placeholder="请输入备案号" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="网站地址" prop="webUrl">
                  <el-input v-model="form.webUrl" placeholder="请输入网站地址" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="author">
          <template #label>
            <el-icon><User /></el-icon>
            <span class="tab-label">作者信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-width="formLabelWidth"
            :label-position="formLabelPosition"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="作者头像" prop="authorAvatar">
                  <UploadImage v-model="form.authorAvatar" :limit="1" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="作者名称" prop="author">
                  <el-input v-model="form.author" placeholder="请输入作者名称" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="个性签名" prop="authorInfo">
                  <el-input v-model="form.authorInfo" placeholder="请输入个性签名" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="关于我" prop="aboutMe" class="site-config-editor-item">
              <div class="site-config-editor">
                <Toolbar
                  class="site-config-editor__toolbar"
                  :editor="editorRef"
                  :defaultConfig="toolbarConfig"
                  :mode="mode"
                />
                <Editor
                  class="site-config-editor__body"
                  v-model="form.aboutMe"
                  :defaultConfig="editorConfig"
                  :mode="mode"
                  @onCreated="handleCreated"
                />
              </div>
              <div v-if="isMobile" class="site-config-editor__hint">
                手机端已精简工具栏，保留常用格式、链接和插图；电脑端仍显示完整工具栏。
              </div>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="social">
          <template #label>
            <el-icon><Share /></el-icon>
            <span class="tab-label">社交信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-width="formLabelWidth"
            :label-position="formLabelPosition"
          >
            <el-form-item label="Github 地址" prop="github">
              <el-input v-model="form.github" placeholder="请输入 Github 地址">
                <template #prefix>
                  <el-icon><ElementPlus /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="Gitee 地址" prop="gitee">
              <el-input v-model="form.gitee" placeholder="请输入 Gitee 地址">
                <template #prefix>
                  <el-icon><Platform /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="QQ 号" prop="qqNumber">
              <el-input v-model="form.qqNumber" placeholder="请输入 QQ 号">
                <template #prefix>
                  <el-icon><ChatDotRound /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="QQ群" prop="qqGroup">
              <el-input v-model="form.qqGroup" placeholder="请输入 QQ 群">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="微信" prop="wechat">
              <el-input v-model="form.wechat" placeholder="请输入微信号">
                <template #prefix>
                  <el-icon><ChatLineRound /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱地址">
                <template #prefix>
                  <el-icon><Message /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="payment">
          <template #label>
            <el-icon><Money /></el-icon>
            <span class="tab-label">收款信息</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-width="formLabelWidth"
            :label-position="formLabelPosition"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="支付宝收款码" prop="aliPay">
                  <UploadImage v-model="form.aliPay" :limit="1" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="微信收款码" prop="weixinPay">
                  <UploadImage v-model="form.weixinPay" :limit="1" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="settings">
          <template #label>
            <el-icon><Tools /></el-icon>
            <span class="tab-label">网站设置</span>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            :label-width="formLabelWidth"
            :label-position="formLabelPosition"
          >
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="游客头像" prop="touristAvatar">
                  <UploadImage v-model="form.touristAvatar" :limit="1" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="显示的社交信息" prop="showList">
                  <el-select
                    v-model="showList"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    :max-collapse-tags="isMobile ? 2 : 4"
                    placeholder="请选择要显示的社交信息"
                  >
                    <el-option label="邮箱" value="email" />
                    <el-option label="QQ" value="qq" />
                    <el-option label="QQ群" value="qqGroup" />
                    <el-option label="Github" value="github" />
                    <el-option label="Gitee" value="gitee" />
                    <el-option label="微信" value="wechat" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="登录方式" prop="loginTypeList">
                  <el-select
                    v-model="loginTypeList"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    :max-collapse-tags="isMobile ? 2 : 4"
                    placeholder="请选择登录方式"
                  >
                    <el-option
                      v-for="item in loginTypes"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="20">
              <el-col :xs="24" :sm="8">
                <el-form-item label="开启评论">
                  <el-switch v-model="form.openComment" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="开启赞赏">
                  <el-switch v-model="form.openAdmiration" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="开启灯笼">
                  <el-switch v-model="form.openLantern" :active-value="1" :inactive-value="0" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="bottom-buttons">
        <el-button
          :icon="Refresh"
          type="primary"
          v-permission="['sys:web:update']"
          @click="submitForm"
        >
          保存配置
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import UploadImage from '@/components/Upload/Image.vue'
import { getWebConfigApi, updateWebConfigApi } from '@/api/site/config'
import { getDictDataByDictTypesApi } from '@/api/system/dict'
import { uploadApi } from '@/api/file'
import { prepareImageFileForUpload } from '@/utils/upload-image'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const MOBILE_TOOLBAR_KEYS = [
  'headerSelect',
  'bold',
  'italic',
  'underline',
  'color',
  'bulletedList',
  'numberedList',
  'blockquote',
  'insertLink',
  'emotion',
  'insertImage',
  'undo',
  'redo',
  'fullScreen'
]

const editorRef = shallowRef()
const mode = 'default'
const isMobile = ref(false)
const toolbarConfig = computed(() => (
  isMobile.value
    ? { toolbarKeys: MOBILE_TOOLBAR_KEYS }
    : {}
))
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      customUpload: contentUpload
    },
    codeSelectLang: {
      codeLangs: [
        { text: 'CSS', value: 'css' },
        { text: 'HTML', value: 'html' },
        { text: 'XML', value: 'xml' },
        { text: 'Java', value: 'java' }
      ]
    }
  }
}

const formLabelWidth = computed(() => (isMobile.value ? '100%' : '120px'))
const formLabelPosition = computed(() => (isMobile.value ? 'top' : 'right'))

const activeTab = ref('basic')
const formRef = ref<FormInstance>()
const form = ref({
  logo: '',
  name: '',
  summary: '',
  recordNum: '',
  webUrl: '',
  author: '',
  authorInfo: '',
  authorAvatar: '',
  github: '',
  gitee: '',
  qqNumber: '',
  qqGroup: '',
  wechat: '',
  email: '',
  aliPay: '',
  weixinPay: '',
  showList: '',
  loginTypeList: '',
  openComment: 1,
  openAdmiration: 1,
  touristAvatar: '',
  bulletin: '',
  aboutMe: '',
  openLantern: 0
})
const showList = ref<string[]>([])
const loginTypeList = ref<string[]>([])
const loginTypes = ref<any[]>([])
const files = ref()

const rules = {
  name: [{ required: true, message: '请输入网站名称', trigger: 'blur' }],
  logo: [{ required: true, message: '请上传网站 Logo', trigger: 'change' }],
  summary: [{ required: true, message: '请输入网站介绍', trigger: 'blur' }],
  recordNum: [{ required: true, message: '请输入备案号', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者名称', trigger: 'blur' }]
}

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const safeParseArray = (value: string) => {
  if (!value) return []
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

async function contentUpload(file: File, insertFn: any) {
  try {
    files.value = await prepareImageFileForUpload(file)
    const formData = new FormData()
    formData.append('file', files.value)
    const res = await uploadApi(formData, 'site-config')
    insertFn(res.data, '', res.data)
  } catch (error: any) {
    if (error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const getDictDataByDictTypes = async () => {
  const res = await getDictDataByDictTypesApi(['login_type'])
  loginTypes.value = res.data.login_type.list
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate((valid) => {
    if (!valid) return

    form.value.showList = JSON.stringify(showList.value)
    form.value.loginTypeList = JSON.stringify(loginTypeList.value)
    updateWebConfigApi(form.value).then(() => {
      ElMessage.success('保存成功')
    })
  })
}

onMounted(() => {
  syncMobile()
  getWebConfigApi().then((res) => {
    form.value = res.data
    showList.value = safeParseArray(form.value.showList)
    loginTypeList.value = safeParseArray(form.value.loginTypeList)
  })
  getDictDataByDictTypes()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  editorRef.value?.destroy?.()
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
.site-config-page {
  padding: 10px;
}

.tab-label {
  margin-left: 4px;
  vertical-align: middle;
}

.bottom-buttons {
  margin-top: 20px;
  text-align: center;
}

:deep(.el-tabs__item) {
  display: flex !important;
  align-items: center;
  justify-content: center;
}

:deep(.el-input-group__prepend) {
  padding: 0 10px;
}

.el-form-item {
  max-width: 640px;
}

.site-config-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.site-config-editor__toolbar {
  border-bottom: 1px solid var(--el-border-color);
}

.site-config-editor__body {
  min-height: 320px;
  overflow-y: hidden;
}

.site-config-editor__hint {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .site-config-page {
    padding: 0;
  }

  .bottom-buttons {
    position: sticky;
    bottom: 0;
    z-index: 5;
    margin: 0 -16px -16px;
    padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
    background: var(--el-bg-color);
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .tab-label {
    font-size: 13px;
  }

  .el-form-item {
    max-width: none;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }

  :deep(.el-tabs__header) {
    margin-bottom: 14px;
  }

  :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }

  :deep(.el-tabs__nav-wrap::-webkit-scrollbar) {
    display: none;
  }

  :deep(.el-tabs__item) {
    padding: 0 12px !important;
  }

  .site-config-editor__toolbar {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .site-config-editor__toolbar::-webkit-scrollbar {
    display: none;
  }

  .site-config-editor__body {
    min-height: 48dvh;
  }
}
</style>
