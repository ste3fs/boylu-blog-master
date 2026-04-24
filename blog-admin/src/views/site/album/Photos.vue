<template>
  <el-dialog
    title="相册照片"
    :model-value="props.openPhotos"
    :width="isMobile ? '100vw' : '88%'"
    :top="isMobile ? '0' : '5vh'"
    :fullscreen="isMobile"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
    class="photos-dialog"
    @update:model-value="handleDialogClose"
  >
    <el-card class="box-card">
      <template #header>
        <PageToolbar>
          <PageToolbarGroup>
            <el-button
              v-permission="['sys:photo:add']"
              type="primary"
              :icon="Plus"
              @click="handleAdd"
            >
              新增照片
            </el-button>
            <el-button
              v-permission="['sys:photo:move']"
              type="info"
              plain
              :icon="Notification"
              :disabled="selectedIds.length === 0"
              @click="handleBatchMove"
            >
              批量移动
            </el-button>
          </PageToolbarGroup>
          <PageToolbarGroup kind="danger">
            <el-button
              v-permission="['sys:photo:delete']"
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
                {{ selectedIds.length === photoList.length && photoList.length ? '清空选择' : '全选当前页' }}
              </el-button>
            </PageToolbarGroup>
          </template>
        </PageToolbar>
      </template>

      <div v-loading="loading" class="photo-list">
        <template v-if="photoList.length">
          <article v-for="item in photoList" :key="item.id" class="photo-card">
            <div class="photo-card__media">
              <el-checkbox
                class="photo-card__check"
                :model-value="selectedIds.includes(item.id)"
                @change="(checked) => toggleSelect(item.id, Boolean(checked))"
              />
              <el-image class="photo-card__image" :src="item.url" fit="cover" />
            </div>
            <div class="photo-card__body">
              <div class="photo-card__meta">
                <span>记录时间</span>
                <strong>{{ item.recordTime || '未填写' }}</strong>
              </div>
              <div class="photo-card__desc">{{ item.description || '暂无描述' }}</div>
              <PageTableActions class="photo-card__actions">
                <PageTableAction type="success" size="small" :icon="View" @click="handlePreviewPhotos(item)">
                  预览
                </PageTableAction>
                <PageTableAction
                  v-permission="['sys:photo:update']"
                  type="primary"
                  size="small"
                  :icon="Edit"
                  @click="handleUpdate(item)"
                >
                  编辑
                </PageTableAction>
                <PageTableAction
                  v-permission="['sys:photo:delete']"
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

        <el-empty v-else description="当前相册还没有照片" />
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
      class="photo-edit-dialog"
    >
      <el-form
        ref="photoFormRef"
        :model="photoForm"
        :rules="rules"
        :label-position="isMobile ? 'top' : 'right'"
        label-width="88px"
        class="photo-form"
      >
        <el-form-item label="照片" prop="url">
          <UploadImage
            v-model="photoForm.url"
            :source="'photo'"
            :limit="dialog.type === 'add' ? 20 : 1"
            :multiple="dialog.type === 'add'"
          />
          <div class="photo-upload-hint">
            <template v-if="dialog.type === 'add'">
              可一次上传多张图片，当前最多 20 张，会按顺序自动创建照片记录。
            </template>
            <template v-else>
              编辑时只修改当前这一张照片。
            </template>
          </div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="photoForm.description"
            type="textarea"
            :rows="4"
            show-word-limit
            placeholder="可选，批量上传时会写入到所有选中的图片"
          />
        </el-form-item>
        <el-form-item label="记录时间" prop="recordTime">
          <el-date-picker
            v-model="photoForm.recordTime"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择记录时间"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="photoForm.sort" :min="1" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      title="移动照片"
      v-model="moveDialog.visible"
      :width="isMobile ? '100vw' : '420px'"
      :top="isMobile ? '0' : '10vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="88px" class="photo-form">
        <el-form-item label="目标相册">
          <el-select v-model="moveDialog.targetAlbumId" placeholder="请选择目标相册" style="width: 100%">
            <el-option
              v-for="album in albumList"
              :key="album.id"
              :label="album.name"
              :value="album.id"
              :disabled="album.id === props.albumId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelMove">取消</el-button>
          <el-button type="primary" @click="confirmMove">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-image-viewer v-if="openPreview" :url-list="previewList" @close="closeViewer" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Notification, Plus, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import UploadImage from '@/components/Upload/Image.vue'
import { listAlbumAllApi } from '@/api/site/album'
import {
  addPhotoApi,
  deletePhotoApi,
  listPhotoApi,
  movePhotoApi,
  updatePhotoApi
} from '@/api/site/photo'
import { normalizeImageList } from '@/utils/image'

const props = defineProps({
  openPhotos: {
    type: Boolean,
    default: false
  },
  albumId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:openPhotos', 'close'])

const isMobile = ref(false)
const loading = ref(false)
const total = ref(0)
const photoList = ref<any[]>([])
const albumList = ref<any[]>([])
const selectedIds = ref<number[]>([])
const previewList = ref<string[]>([])
const openPreview = ref(false)
const submitLoading = ref(false)
const photoFormRef = ref<FormInstance>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  albumId: 0
})

const dialog = reactive({
  title: '',
  visible: false,
  type: 'add' as 'add' | 'edit'
})

const moveDialog = reactive({
  visible: false,
  targetAlbumId: undefined as number | undefined
})

const photoForm = reactive<any>({
  id: undefined,
  albumId: 0,
  url: [] as string[] | string,
  description: '',
  recordTime: '',
  sort: 1
})

const rules = reactive<FormRules>({
  url: [
    { required: true, message: '请先上传照片', trigger: 'change' }
  ]
})

const syncMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

const resetForm = () => {
  photoForm.id = undefined
  photoForm.albumId = props.albumId
  photoForm.url = dialog.type === 'add' ? [] : ''
  photoForm.description = ''
  photoForm.recordTime = ''
  photoForm.sort = 1
}

const getList = async () => {
  loading.value = true
  try {
    queryParams.albumId = props.albumId
    const { data } = await listPhotoApi(queryParams)
    photoList.value = data.records || []
    total.value = data.total || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const getAlbumList = async () => {
  const { data } = await listAlbumAllApi()
  albumList.value = data || []
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
  if (photoList.value.length && selectedIds.value.length === photoList.value.length) {
    selectedIds.value = []
    return
  }
  selectedIds.value = photoList.value.map(item => item.id)
}

const handleAdd = () => {
  dialog.type = 'add'
  dialog.title = '新增照片'
  dialog.visible = true
  resetForm()
}

const handleUpdate = (row: any) => {
  dialog.type = 'edit'
  dialog.title = '编辑照片'
  dialog.visible = true
  resetForm()
  Object.assign(photoForm, {
    id: row.id,
    albumId: props.albumId,
    url: row.url,
    description: row.description || '',
    recordTime: row.recordTime || '',
    sort: row.sort || 1
  })
}

const submitForm = async () => {
  if (!photoFormRef.value) return

  await photoFormRef.value.validate(async (valid) => {
    if (!valid) return

    const urls = normalizeImageList(photoForm.url)
    if (!urls.length) {
      ElMessage.warning('请先上传照片')
      return
    }

    submitLoading.value = true
    try {
      if (dialog.type === 'add') {
        await Promise.all(
          urls.map((url, index) => addPhotoApi({
            albumId: props.albumId,
            url,
            description: photoForm.description,
            recordTime: photoForm.recordTime,
            sort: Number(photoForm.sort || 1) + index
          }))
        )
        ElMessage.success(`已新增 ${urls.length} 张照片`)
      } else {
        await updatePhotoApi({
          ...photoForm,
          albumId: props.albumId,
          url: urls[0]
        })
        ElMessage.success('照片修改成功')
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
  photoFormRef.value?.resetFields()
  resetForm()
}

const handleBatchMove = () => {
  if (selectedIds.value.length === 0) return
  moveDialog.visible = true
}

const confirmMove = async () => {
  if (!moveDialog.targetAlbumId) {
    ElMessage.warning('请选择目标相册')
    return
  }

  await movePhotoApi({
    photoIds: selectedIds.value,
    albumId: moveDialog.targetAlbumId
  })
  ElMessage.success('照片移动成功')
  moveDialog.visible = false
  moveDialog.targetAlbumId = undefined
  selectedIds.value = []
  getList()
}

const cancelMove = () => {
  moveDialog.visible = false
  moveDialog.targetAlbumId = undefined
}

const handleBatchDelete = async () => {
  if (selectedIds.value.length === 0) return

  await ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 张照片吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deletePhotoApi(selectedIds.value)
  ElMessage.success('批量删除成功')
  getList()
}

const handleDelete = async (row: any) => {
  await ElMessageBox.confirm(
    `确定要删除这张照片吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await deletePhotoApi(row.id)
  ElMessage.success('删除成功')
  getList()
}

const handlePreviewPhotos = (item: any) => {
  previewList.value = [item.url]
  openPreview.value = true
}

const closeViewer = () => {
  openPreview.value = false
  previewList.value = []
}

const handleSizeChange = (val: number) => {
  queryParams.pageSize = val
  getList()
}

const handleCurrentChange = (val: number) => {
  queryParams.pageNum = val
  getList()
}

const handleDialogClose = (val: boolean) => {
  emit('update:openPhotos', val)
}

watch(
  () => props.openPhotos,
  (visible) => {
    if (!visible) return
    queryParams.pageNum = 1
    getAlbumList()
    getList()
  }
)

onMounted(() => {
  syncMobile()
  window.addEventListener('resize', syncMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncMobile)
})
</script>

<style scoped lang="scss">
.photo-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.photo-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.photo-card__media {
  position: relative;
}

.photo-card__check {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  padding: 4px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.photo-card__image {
  display: block;
  width: 100%;
  height: 220px;
  background: var(--el-fill-color);
}

.photo-card__body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.photo-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;

  strong {
    color: var(--el-text-color-primary);
    font-weight: 600;
  }
}

.photo-card__desc {
  min-height: 44px;
  color: var(--el-text-color-primary);
  line-height: 1.65;
  word-break: break-word;
}

.photo-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.photo-upload-hint {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 768px) {
  .photo-list {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .photo-card__image {
    height: 200px;
  }

  :deep(.photo-edit-dialog),
  :deep(.photos-dialog) {
    border-radius: 0 !important;
  }

  :deep(.photo-form .el-form-item) {
    display: block;
    margin-bottom: 16px !important;
  }

  :deep(.photo-form .el-form-item__label) {
    display: block;
    width: 100% !important;
    padding: 0 0 8px !important;
    text-align: left !important;
    font-weight: 600;
  }

  :deep(.photo-form .el-form-item__content) {
    margin-left: 0 !important;
    width: 100%;
  }
}
</style>
