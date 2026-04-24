<template>
  <div class="app-container moment-page">
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:moment:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >
              新增说说
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:moment:delete']"
              type="danger"
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table
        v-if="!isMobile"
        v-loading="loading"
        :data="momentList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="内容" align="center" prop="content" show-overflow-tooltip />
        <el-table-column label="图片" align="center" width="180">
          <template #default="scope">
            <div class="moment-images">
              <el-image
                v-for="item in parseImages(scope.row.images)"
                :key="item"
                :src="item"
                fit="cover"
                class="moment-thumb"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
        <el-table-column label="操作" align="center" width="180" fixed="right">
          <template #default="scope">
            <PageTableActions>
              <PageTableAction
                v-permission="['sys:moment:update']"
                type="primary"
                :icon="Edit"
                @click="handleUpdate(scope.row)"
              >
                编辑
              </PageTableAction>
              <PageTableAction
                v-permission="['sys:moment:delete']"
                type="danger"
                :icon="Delete"
                @click="handleDelete(scope.row)"
              >
                删除
              </PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <div v-else v-loading="loading" class="moment-mobile-list">
        <div v-if="selectedIds.length" class="moment-selection-bar">
          <span>已选 {{ selectedIds.length }} 条说说</span>
          <el-button link type="primary" @click="selectedIds = []">清空</el-button>
        </div>

        <template v-if="momentList.length">
          <article v-for="row in momentList" :key="row.id" class="moment-card">
            <div class="moment-card__header">
              <el-checkbox
                :model-value="isMomentSelected(row.id)"
                @change="(checked) => toggleMomentSelection(row.id, Boolean(checked))"
              />
              <div class="moment-card__content" v-html="row.content || '暂无内容'" />
            </div>

            <div v-if="parseImages(row.images).length" class="moment-card__images">
              <el-image
                v-for="item in parseImages(row.images).slice(0, 4)"
                :key="item"
                :src="item"
                fit="cover"
                class="moment-card__image"
              />
            </div>

            <div class="moment-card__meta">
              <span>发布时间</span>
              <strong>{{ row.createTime || '未记录' }}</strong>
            </div>

            <div class="moment-card__actions">
              <el-button type="primary" plain :icon="Edit" @click="handleUpdate(row)">编辑</el-button>
              <el-button type="danger" plain :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </div>
          </article>
        </template>

        <el-empty v-else description="暂无说说数据" />
      </div>

      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      :width="isMobile ? '100vw' : '600px'"
      :top="isMobile ? '0' : '8vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="custom-dialog moment-dialog"
    >
      <el-form
        ref="momentFormRef"
        :model="momentForm"
        :rules="rules"
        :label-width="isMobile ? '100%' : '80px'"
        :label-position="isMobile ? 'top' : 'right'"
        class="custom-form"
      >
        <el-form-item label="内容" prop="content">
          <div class="moment-editor">
            <Toolbar
              class="moment-editor__toolbar"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              :mode="mode"
            />
            <Editor
              class="moment-editor__body"
              v-model="momentForm.content"
              :defaultConfig="editorConfig"
              :mode="mode"
              @onCreated="handleCreated"
            />
          </div>
          <div v-if="isMobile" class="moment-editor__hint">
            手机端已精简工具栏，保留常用格式、链接、表情和图片。
          </div>
        </el-form-item>
        <el-form-item label="图片" prop="images">
          <UploadImage v-model="momentForm.images" :source="'moment'" :limit="9" :multiple="true" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getSysMomentListApi,
  addSysMomentApi,
  updateSysMomentApi,
  deleteSysMomentApi
} from '@/api/article/moment'
import UploadImage from '@/components/Upload/Image.vue'
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
const toolbarConfig = computed(() => (
  isMobile.value
    ? { toolbarKeys: MOBILE_TOOLBAR_KEYS }
    : {}
))
const editorConfig = {
  placeholder: '请输入说说内容...',
  MENU_CONF: {
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

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const loading = ref(false)
const total = ref(0)
const momentList = ref<any[]>([])
const momentFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const selectedIds = ref<number[]>([])
const isMobile = ref(false)

const dialog = reactive({
  title: '',
  visible: false,
  type: 'add'
})

const momentForm = reactive<any>({
  id: undefined,
  content: '',
  images: ''
})

const rules = reactive<FormRules>({
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const parseImages = (images?: string | string[]) => {
  if (Array.isArray(images)) {
    return images.filter(Boolean)
  }
  return String(images || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

const isMomentSelected = (id: number) => selectedIds.value.includes(id)

const toggleMomentSelection = (id: number, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
    return
  }
  selectedIds.value = selectedIds.value.filter(item => item !== id)
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await getSysMomentListApi(queryParams)
    momentList.value = data.records
    total.value = data.total
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: any[]) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return

  ElMessageBox.confirm(`是否确认删除 ${selectedIds.value.length} 条说说？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteSysMomentApi(selectedIds.value)
    ElMessage.success('批量删除成功')
    getList()
  })
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('是否确认删除这条说说？', '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteSysMomentApi(row.id)
    ElMessage.success('删除成功')
    getList()
  })
}

const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增说说'
  dialog.visible = true
  momentForm.id = undefined
  momentForm.content = ''
  momentForm.images = ''
}

const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '编辑说说'
  dialog.visible = true
  Object.assign(momentForm, row)
  momentForm.images = parseImages(momentForm.images)
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

const submitForm = async () => {
  if (!momentFormRef.value) return

  await momentFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const payload = {
        ...momentForm,
        images: Array.isArray(momentForm.images) ? momentForm.images.join(',') : momentForm.images
      }
      if (dialog.type === 'add') {
        await addSysMomentApi(payload)
        ElMessage.success('新增成功')
      } else {
        await updateSysMomentApi(payload)
        ElMessage.success('编辑成功')
      }
      dialog.visible = false
      getList()
    } finally {
      submitLoading.value = false
    }
  })
}

const cancel = () => {
  dialog.visible = false
  momentFormRef.value?.resetFields()
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

onMounted(() => {
  syncMobile()
  getList()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
.moment-images {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.moment-thumb {
  width: 50px;
  height: 50px;
  border-radius: 8px;
}

.moment-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
}

.moment-editor__toolbar {
  border-bottom: 1px solid var(--el-border-color);
}

.moment-editor__body {
  min-height: 300px;
  overflow-y: hidden;
}

.moment-editor__hint {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .moment-page {
    .moment-mobile-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .moment-selection-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 2px 2px 0;
      color: var(--el-text-color-secondary);
      font-size: 13px;
    }

    .moment-card {
      padding: 14px;
      border-radius: 16px;
      background: var(--el-fill-color-extra-light);
      border: 1px solid var(--el-border-color-lighter);
      box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
    }

    .moment-card__header {
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }

    .moment-card__content {
      flex: 1;
      min-width: 0;
      color: var(--el-text-color-primary);
      font-size: 14px;
      line-height: 1.7;
      word-break: break-word;

      :deep(p) {
        margin: 0;
      }
    }

    .moment-card__images {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 8px;
      margin-top: 12px;
    }

    .moment-card__image {
      width: 100%;
      aspect-ratio: 1;
      border-radius: 10px;
      overflow: hidden;
      background: var(--el-fill-color);
    }

    .moment-card__meta {
      display: flex;
      justify-content: space-between;
      gap: 10px;
      margin-top: 12px;
      padding-top: 12px;
      border-top: 1px solid var(--el-border-color-lighter);
      color: var(--el-text-color-secondary);
      font-size: 12px;

      strong {
        color: var(--el-text-color-primary);
        font-weight: 500;
      }
    }

    .moment-card__actions {
      display: flex;
      gap: 10px;
      margin-top: 14px;

      .el-button {
        flex: 1;
        min-height: 38px;
        margin: 0;
      }
    }

    :deep(.moment-dialog) {
      border-radius: 0 !important;
    }

    :deep(.el-dialog__body) {
      padding: 14px !important;
    }

    :deep(.el-dialog__footer) {
      position: sticky;
      bottom: 0;
      z-index: 2;
      background: var(--el-bg-color);
      border-top: 1px solid var(--el-border-color-lighter);
    }

    :deep(.el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.el-form-item__label) {
      display: block;
      width: 100% !important;
      padding: 0 0 8px !important;
      text-align: left !important;
      font-weight: 600;
    }

    :deep(.el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }

    .moment-editor__toolbar {
      overflow-x: auto;
      scrollbar-width: none;

      &::-webkit-scrollbar {
        display: none;
      }
    }

    .moment-editor__body {
      min-height: 48dvh;
    }
  }
}
</style>
