<template>
  <div class="app-container notice-page">
    <PageSearch>
      <el-form ref="queryFormRef" :model="queryParams" :inline="!isMobile">
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="queryParams.content"
            placeholder="请输入公告内容"
            clearable
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="是否展示" prop="isShow">
          <el-select v-model="queryParams.isShow" placeholder="请选择展示状态" clearable>
            <el-option label="是" :value="1" />
            <el-option label="否" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="显示位置" prop="position">
          <el-select v-model="queryParams.position" placeholder="请选择显示位置" clearable>
            <el-option
              v-for="item in positionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <PageSearchActions @search="handleQuery" @reset="resetQuery" />
        </el-form-item>
      </el-form>
    </PageSearch>

    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              type="primary"
              :icon="Plus"
              v-permission="['sys:notice:add']"
              @click="handleAdd"
            >
              新增公告
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              type="danger"
              plain
              :icon="Delete"
              v-permission="['sys:notice:delete']"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
        </PageToolbar>
      </template>

      <el-table
        v-loading="loading"
        :data="dataList"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="公告内容" prop="content" min-width="320" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="notice-content" v-html="row.content" />
          </template>
        </el-table-column>
        <el-table-column label="是否展示" prop="isShow" align="center" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.isShow"
              :active-value="1"
              :inactive-value="0"
              style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
              @change="handleChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="显示位置" prop="position" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="resolvePositionMeta(row.position).style || 'info'">
              {{ resolvePositionMeta(row.position).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" prop="createTime" align="center" width="170" />
        <el-table-column label="操作" align="center" width="160" fixed="right">
          <template #default="{ row }">
            <PageTableActions>
              <PageTableAction
                type="primary"
                :icon="Edit"
                v-permission="['sys:notice:update']"
                @click="handleUpdate(row)"
              >
                编辑
              </PageTableAction>
              <PageTableAction
                type="danger"
                :icon="Delete"
                v-permission="['sys:notice:delete']"
                @click="handleDelete(row)"
              >
                删除
              </PageTableAction>
            </PageTableActions>
          </template>
        </el-table-column>
      </el-table>

      <PagePagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog
      v-model="open"
      :title="title"
      :width="isMobile ? '100vw' : '640px'"
      :top="isMobile ? '0' : '6vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="notice-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="88px"
        class="notice-form"
      >
        <el-form-item label="公告内容" prop="content">
          <div class="notice-editor">
            <Toolbar
              class="notice-editor__toolbar"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              :mode="mode"
            />
            <Editor
              class="notice-editor__body"
              v-model="form.content"
              :defaultConfig="editorConfig"
              :mode="mode"
              @onCreated="handleCreated"
            />
          </div>
          <div v-if="isMobile" class="notice-editor__hint">
            手机端已精简工具栏，保留常用格式和撤销重做；电脑端仍显示完整工具栏。
          </div>
        </el-form-item>

        <el-form-item label="是否展示" prop="isShow">
          <el-switch
            v-model="form.isShow"
            :active-value="1"
            :inactive-value="0"
            style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
          />
        </el-form-item>

        <el-form-item label="显示位置" prop="position">
          <el-radio-group v-model="form.position" class="notice-position-group">
            <el-radio
              v-for="item in positionOptions"
              :key="item.value"
              :value="item.value"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import {
  addSysNoticeApi,
  deleteSysNoticeApi,
  listSysNoticeApi,
  updateSysNoticeApi
} from '@/api/site/notice'
import { getDictDataByDictTypesApi } from '@/api/system/dict'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const MOBILE_TOOLBAR_KEYS = [
  'headerSelect',
  'bold',
  'italic',
  'underline',
  'color',
  'bgColor',
  '|',
  'bulletedList',
  'numberedList',
  'blockquote',
  '|',
  'insertLink',
  'emotion',
  'insertImage',
  '|',
  'undo',
  'redo'
]

const editorRef = shallowRef()
const mode = 'default'
const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const open = ref(false)
const title = ref('')
const dataList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const positionOptions = ref<any[]>([])

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  content: undefined as string | undefined,
  isShow: undefined as number | undefined,
  position: undefined as string | undefined
})

const form = reactive<any>({
  id: undefined,
  content: '',
  isShow: 1,
  position: 'right'
})

const toolbarConfig = computed(() => (
  isMobile.value
    ? { toolbarKeys: MOBILE_TOOLBAR_KEYS }
    : {}
))

const editorConfig = computed(() => ({
  placeholder: '请输入公告内容...'
}))

const rules = reactive({
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' }
  ],
  isShow: [
    { required: true, message: '请选择是否展示', trigger: 'change' }
  ],
  position: [
    { required: true, message: '请选择显示位置', trigger: 'change' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const resolvePositionMeta = (value: string) => {
  return positionOptions.value.find(item => item.value === value) || {
    label: value || '未设置',
    style: 'info'
  }
}

const reset = () => {
  form.id = undefined
  form.content = ''
  form.isShow = 1
  form.position = positionOptions.value[0]?.value || 'right'
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await listSysNoticeApi(queryParams)
    dataList.value = data.records || []
    total.value = data.total || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const getDicts = async () => {
  const { data } = await getDictDataByDictTypesApi(['notice_position'])
  positionOptions.value = data.notice_position?.list || []
  if (!form.position && positionOptions.value.length) {
    form.position = positionOptions.value[0].value
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const handleSelectionChange = (selection: Array<{ id: number }>) => {
  selectedIds.value = selection.map(item => item.id)
}

const handleAdd = () => {
  reset()
  title.value = '新增公告'
  open.value = true
}

const handleUpdate = (row: any) => {
  reset()
  Object.assign(form, {
    id: row.id,
    content: row.content,
    isShow: row.isShow,
    position: row.position
  })
  title.value = '编辑公告'
  open.value = true
}

const submitForm = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return

    if (form.id !== undefined) {
      await updateSysNoticeApi(form)
      ElMessage.success('公告修改成功')
    } else {
      await addSysNoticeApi(form)
      ElMessage.success('公告新增成功')
    }
    open.value = false
    getList()
  })
}

const cancel = () => {
  open.value = false
  reset()
  formRef.value?.resetFields()
}

const handleBatchDelete = async () => {
  if (!selectedIds.value.length) return

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条公告吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteSysNoticeApi(selectedIds.value)
  ElMessage.success('删除成功')
  getList()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除这条公告吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteSysNoticeApi(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handleChange = async (row: any) => {
  const previous = row.isShow === 1 ? 0 : 1
  try {
    await updateSysNoticeApi({
      id: row.id,
      isShow: row.isShow,
      position: row.position,
      content: row.content
    })
    ElMessage.success('展示状态已更新')
  } catch (error) {
    row.isShow = previous
  }
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

const handleCreated = (editor: any) => {
  editorRef.value = editor
}

onMounted(async () => {
  syncMobile()
  window.addEventListener('resize', syncMobile)
  await getDicts()
  reset()
  getList()
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
  editorRef.value?.destroy?.()
})
</script>

<style scoped lang="scss">
.notice-content {
  line-height: 1.7;
  color: var(--el-text-color-primary);

  :deep(p) {
    margin: 0;
  }
}

.notice-editor {
  width: 100%;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
  background: var(--el-bg-color);
}

.notice-editor__toolbar {
  border-bottom: 1px solid var(--el-border-color);
}

.notice-editor__body {
  min-height: 320px;
  overflow-y: hidden;
}

.notice-editor__hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.notice-position-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .notice-page {
    :deep(.notice-dialog) {
      border-radius: 0 !important;
    }

    :deep(.notice-form .el-form-item) {
      display: block;
      margin-bottom: 16px !important;
    }

    :deep(.notice-form .el-form-item__label) {
      display: block;
      width: 100% !important;
      padding: 0 0 8px !important;
      text-align: left !important;
      font-weight: 600;
    }

    :deep(.notice-form .el-form-item__content) {
      margin-left: 0 !important;
      width: 100%;
    }

    .notice-editor__toolbar {
      overflow-x: auto;
      scrollbar-width: none;

      &::-webkit-scrollbar {
        display: none;
      }
    }

    .notice-editor__body {
      min-height: 48dvh;
    }
  }
}
</style>
