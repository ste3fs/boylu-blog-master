<template>
  <div class="app-container album-page">
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:album:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >
              新增相册
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:album:delete']"
              type="danger"
              plain
              :icon="Delete"
              :disabled="selectedIds.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </PageToolbarGroup>
          <template #right>
            <PageToolbarGroup kind="utility">
              <el-button type="success" plain :icon="Check" @click="handleAllSelect">
                {{ selectedIds.length === albumList.length && albumList.length ? '清空选择' : '全选当前页' }}
              </el-button>
            </PageToolbarGroup>
          </template>
        </PageToolbar>
      </template>

      <div v-loading="loading" class="album-list">
        <template v-if="albumList.length">
          <article v-for="item in albumList" :key="item.id" class="album-card">
            <div class="album-card__media">
              <el-checkbox
                class="album-card__check"
                :model-value="selectedIds.includes(item.id)"
                @change="(checked) => toggleSelect(item.id, Boolean(checked))"
              />
              <el-image class="album-card__cover" :src="item.cover" fit="cover" />
              <div v-if="item.isLock" class="album-card__lock">
                <el-icon><Lock /></el-icon>
              </div>
            </div>
            <div class="album-card__body">
              <div class="album-card__name">{{ item.name || '未命名相册' }}</div>
              <div class="album-card__desc">{{ item.description || '暂无描述' }}</div>
              <PageTableActions class="album-card__actions">
                <PageTableAction type="success" size="small" :icon="Picture" @click="handlePreviewPhotos(item)">
                  查看照片
                </PageTableAction>
                <PageTableAction
                  v-permission="['sys:album:update']"
                  type="primary"
                  size="small"
                  :icon="Edit"
                  @click="handleUpdate(item)"
                >
                  编辑
                </PageTableAction>
                <PageTableAction
                  v-permission="['sys:album:delete']"
                  type="danger"
                  size="small"
                  :icon="Delete"
                  @click="handleDelete(item)"
                >
                  删除
                </PageTableAction>
              </PageTableActions>
            </div>
          </article>
        </template>

        <el-empty v-else description="还没有相册数据" />
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
      :width="isMobile ? '100vw' : '640px'"
      :top="isMobile ? '0' : '6vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      class="album-dialog"
    >
      <el-form
        ref="albumFormRef"
        :model="albumForm"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="88px"
        class="album-form"
      >
        <el-form-item label="封面" prop="cover">
          <UploadImage v-model="albumForm.cover" :source="'album-cover'" :limit="1" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="albumForm.name" placeholder="请输入相册名称" clearable />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="albumForm.description"
            type="textarea"
            :rows="4"
            show-word-limit
            placeholder="请输入相册描述"
            clearable
          />
        </el-form-item>
        <el-form-item label="是否加密" prop="isLock">
          <el-switch v-model="albumForm.isLock" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="albumForm.isLock === 1" label="访问密码" prop="password">
          <el-input
            v-model="albumForm.password"
            type="password"
            show-password
            placeholder="请输入相册访问密码"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <Photos v-model:openPhotos="openPhotos" :albumId="albumForm.id" />
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Lock, Picture, Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import UploadImage from '@/components/Upload/Image.vue'
import {
  addAlbumApi,
  deleteAlbumApi,
  listAlbumApi,
  updateAlbumApi,
  verifyAlbumPasswordApi
} from '@/api/site/album'
import Photos from './Photos.vue'

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const albumList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const albumFormRef = ref<FormInstance>()
const submitLoading = ref(false)
const openPhotos = ref(false)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10
})

const dialog = reactive({
  title: '',
  visible: false,
  type: 'add' as 'add' | 'edit'
})

const albumForm = reactive<any>({
  id: undefined,
  name: '',
  description: '',
  cover: '',
  isLock: 0,
  password: ''
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入相册名称', trigger: 'blur' }
  ],
  isLock: [
    { required: true, message: '请选择是否加密', trigger: 'change' }
  ],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (albumForm.isLock === 1 && !value) {
          callback(new Error('加密相册必须填写访问密码'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const resetForm = () => {
  albumForm.id = undefined
  albumForm.name = ''
  albumForm.description = ''
  albumForm.cover = ''
  albumForm.isLock = 0
  albumForm.password = ''
}

const getList = async () => {
  loading.value = true
  try {
    const { data } = await listAlbumApi(queryParams)
    albumList.value = data.records || []
    total.value = data.total || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const toggleSelect = (id: number, checked: boolean) => {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
    return
  }
  selectedIds.value = selectedIds.value.filter(item => item !== id)
}

const handleAllSelect = () => {
  if (albumList.value.length && selectedIds.value.length === albumList.value.length) {
    selectedIds.value = []
    return
  }
  selectedIds.value = albumList.value.map(item => item.id)
}

const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增相册'
  dialog.visible = true
  resetForm()
}

const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '编辑相册'
  dialog.visible = true
  resetForm()
  Object.assign(albumForm, {
    id: row.id,
    name: row.name,
    description: row.description || '',
    cover: row.cover || '',
    isLock: row.isLock ?? 0,
    password: row.password || ''
  })
}

const submitForm = async () => {
  if (!albumFormRef.value) return

  await albumFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      if (dialog.type === 'add') {
        await addAlbumApi(albumForm)
        ElMessage.success('相册新增成功')
      } else {
        await updateAlbumApi(albumForm)
        ElMessage.success('相册修改成功')
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
  albumFormRef.value?.resetFields()
  resetForm()
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 个相册吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteAlbumApi(selectedIds.value)
  ElMessage.success('批量删除成功')
  getList()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除相册“${row.name || '未命名相册'}”吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deleteAlbumApi(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handlePreviewPhotos = async (row: any) => {
  if (row.isLock === 1) {
    try {
      const { value } = await ElMessageBox.prompt('请输入相册访问密码', '访问受保护相册', {
        inputType: 'password',
        inputPlaceholder: '请输入密码',
        confirmButtonText: '进入相册',
        cancelButtonText: '取消'
      })
      await verifyAlbumPasswordApi(row.id, value)
    } catch (error) {
      return
    }
  }

  albumForm.id = row.id
  openPhotos.value = true
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
.album-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.album-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.album-card__media {
  position: relative;
}

.album-card__check {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  padding: 4px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.album-card__cover {
  display: block;
  width: 100%;
  height: 180px;
  background: var(--el-fill-color);
}

.album-card__lock {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(8px);
}

.album-card__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.album-card__name {
  font-size: 17px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.album-card__desc {
  min-height: 44px;
  color: var(--el-text-color-secondary);
  line-height: 1.65;
  word-break: break-word;
}

.album-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  .album-list {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  :deep(.album-dialog) {
    border-radius: 0 !important;
  }

  :deep(.album-form .el-form-item) {
    display: block;
    margin-bottom: 16px !important;
  }

  :deep(.album-form .el-form-item__label) {
    display: block;
    width: 100% !important;
    padding: 0 0 8px !important;
    text-align: left !important;
    font-weight: 600;
  }

  :deep(.album-form .el-form-item__content) {
    margin-left: 0 !important;
    width: 100%;
  }
}
</style>
