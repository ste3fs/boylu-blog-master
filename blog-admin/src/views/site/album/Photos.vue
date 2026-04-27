<template>
  <el-dialog
    :title="props.albumName ? `相册照片 · ${props.albumName}` : '相册照片'"
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
              v-permission="['sys:photo:add']"
              type="primary"
              plain
              :icon="PictureFilled"
              @click="handleOpenLibraryDialog"
            >
              从文件库批量添加
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
            @uploading-change="handlePhotoUploadingChange"
          />
          <div class="photo-upload-hint">
            <template v-if="dialog.type === 'add'">
              可一次上传多张图片，当前最多 20 张，会按顺序自动创建照片记录。
            </template>
            <template v-else>
              编辑时只修改当前这一张照片。
            </template>
          </div>
          <div v-if="photoUploading" class="photo-upload-hint photo-upload-hint--warning">
            图片还在上传中，请等待上传完成后再点确定，避免写入临时地址。
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
          <el-button type="primary" :loading="submitLoading" :disabled="photoUploading" @click="submitForm">确定</el-button>
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

    <el-dialog
      v-model="libraryDialog.visible"
      title="从文件库添加照片"
      :width="isMobile ? '100vw' : '960px'"
      :top="isMobile ? '0' : '4vh'"
      :fullscreen="isMobile"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
      class="photo-library-dialog"
    >
      <div class="library-toolbar">
        <el-input
          v-model="libraryQuery.filename"
          clearable
          placeholder="搜索文件名"
          @keyup.enter="handleLibraryQuery"
        />
        <el-button type="primary" :icon="Search" @click="handleLibraryQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetLibraryQuery">重置</el-button>
      </div>

      <div class="library-summary">
        已选 {{ selectedLibraryUrls.length }} 张图片，确认后会直接加入当前相册，不会删除原文件。
      </div>

      <div class="library-summary__actions">
        <el-button type="success" plain size="small" :icon="Check" @click="handleLibrarySelectCurrentPage">
          {{ isLibraryCurrentPageSelected ? '取消本页全选' : '全选本页' }}
        </el-button>
        <el-button link type="primary" :disabled="!selectedLibraryUrls.length" @click="clearLibrarySelection">
          清空已选
        </el-button>
      </div>

      <div v-if="selectedLibraryItems.length" class="library-selection-tray">
        <div class="library-selection-tray__header">
          <div class="library-selection-tray__title">已选图片</div>
          <div class="library-selection-tray__count">{{ selectedLibraryItems.length }} 张，跨页累计保留</div>
        </div>
        <div class="library-selection-tray__list">
          <article
            v-for="item in selectedLibraryItems"
            :key="item.url"
            class="library-selection-chip"
          >
            <el-image class="library-selection-chip__image" :src="item.url" fit="cover" />
            <div class="library-selection-chip__body">
              <div class="library-selection-chip__name">{{ item.filename || item.url }}</div>
              <div class="library-selection-chip__meta">{{ item.extLabel }} · {{ item.sizeLabel }}</div>
            </div>
            <el-button link type="danger" @click="removeLibrarySelection(item.url)">移除</el-button>
          </article>
        </div>
      </div>

      <div v-loading="libraryLoading" class="library-grid">
        <template v-if="libraryFileList.length">
          <article
            v-for="item in libraryFileList"
            :key="item.url"
            class="library-card"
            :class="{ 'is-selected': selectedLibraryUrls.includes(item.url) }"
            @click="toggleLibrarySelect(item.url)"
          >
            <div class="library-card__media">
              <el-checkbox
                class="library-card__check"
                :model-value="selectedLibraryUrls.includes(item.url)"
                @click.stop
                @change="(checked) => toggleLibrarySelect(item.url, Boolean(checked))"
              />
              <el-image class="library-card__image" :src="item.url" fit="cover" />
            </div>
            <div class="library-card__body">
              <div class="library-card__name">{{ item.filename || '未命名图片' }}</div>
              <div class="library-card__meta">
                {{ String(item.ext || 'image').toUpperCase() }} · {{ formatFileSize(item.size) }}
              </div>
            </div>
          </article>
        </template>

        <el-empty v-else description="当前筛选结果没有可加入的图片" />
      </div>

      <PagePagination
        v-model:current-page="libraryQuery.pageNum"
        v-model:page-size="libraryQuery.pageSize"
        :total="libraryTotal"
        @size-change="handleLibrarySizeChange"
        @current-change="handleLibraryCurrentChange"
      />

      <div v-if="isMobile && selectedLibraryItems.length" class="library-mobile-bar">
        <div class="library-mobile-bar__meta">
          <div class="library-mobile-bar__title">已选 {{ selectedLibraryItems.length }} 张</div>
          <div class="library-mobile-bar__desc">跨页选择会继续保留，可直接加入当前相册</div>
        </div>
        <div class="library-mobile-bar__previews">
          <el-image
            v-for="item in recentSelectedLibraryItems"
            :key="item.url"
            class="library-mobile-bar__preview"
            :src="item.url"
            fit="cover"
          />
        </div>
        <div class="library-mobile-bar__actions">
          <el-button plain @click="clearLibrarySelection">清空</el-button>
          <el-button type="primary" :loading="librarySubmitLoading" @click="confirmLibraryImport">
            加入相册
          </el-button>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="libraryDialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="librarySubmitLoading" @click="confirmLibraryImport">
            加入当前相册
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-image-viewer v-if="openPreview" :url-list="previewList" @close="closeViewer" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Notification, PictureFilled, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import UploadImage from '@/components/Upload/Image.vue'
import { getFileListApi } from '@/api/file'
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
  },
  albumName: {
    type: String,
    default: ''
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
const photoUploading = ref(false)
const libraryLoading = ref(false)
const libraryTotal = ref(0)
const librarySubmitLoading = ref(false)
const photoFormRef = ref<FormInstance>()
const libraryFileList = ref<any[]>([])
const selectedLibraryUrls = ref<string[]>([])
const selectedLibraryItems = ref<any[]>([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  albumId: 0
})

const libraryQuery = reactive({
  pageNum: 1,
  pageSize: 24,
  filename: ''
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

const libraryDialog = reactive({
  visible: false
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

const IMAGE_EXTENSIONS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'avif'])

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
  photoUploading.value = false
}

const handlePhotoUploadingChange = (uploading: boolean) => {
  photoUploading.value = uploading
}

const getList = async () => {
  if (!props.albumId) {
    photoList.value = []
    total.value = 0
    selectedIds.value = []
    return
  }

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

const getLibraryList = async () => {
  libraryLoading.value = true
  try {
    const { data } = await getFileListApi(libraryQuery)
    const records = data.records || []
    libraryFileList.value = records.filter((item: any) => IMAGE_EXTENSIONS.has(String(item.ext || '').toLowerCase()))
    libraryTotal.value = data.total || 0
  } finally {
    libraryLoading.value = false
  }
}

const formatFileSize = (size: number | string) => `${(Number(size || 0) / 1024).toFixed(1)} KB`

const buildLibrarySelectionItem = (item: any) => ({
  url: item.url,
  filename: item.filename || '',
  extLabel: String(item.ext || 'image').toUpperCase(),
  sizeLabel: formatFileSize(item.size)
})

const handleOpenLibraryDialog = () => {
  libraryDialog.visible = true
  selectedLibraryUrls.value = []
  selectedLibraryItems.value = []
  libraryQuery.pageNum = 1
  getLibraryList()
}

const toggleLibrarySelect = (url: string, checked?: boolean) => {
  const exists = selectedLibraryUrls.value.includes(url)
  const nextChecked = checked ?? !exists
  const matchedItem = libraryFileList.value.find((item: any) => item.url === url)

  if (nextChecked) {
    if (!exists) {
      selectedLibraryUrls.value.push(url)
    }
    if (matchedItem && !selectedLibraryItems.value.some(item => item.url === url)) {
      selectedLibraryItems.value.push(buildLibrarySelectionItem(matchedItem))
    }
    return
  }

  selectedLibraryUrls.value = selectedLibraryUrls.value.filter(item => item !== url)
  selectedLibraryItems.value = selectedLibraryItems.value.filter(item => item.url !== url)
}

const isLibraryCurrentPageSelected = computed(() => {
  return libraryFileList.value.length > 0
    && libraryFileList.value.every((item: any) => selectedLibraryUrls.value.includes(item.url))
})

const handleLibrarySelectCurrentPage = () => {
  const currentPageUrls = libraryFileList.value.map((item: any) => item.url)
  if (!currentPageUrls.length) return

  if (isLibraryCurrentPageSelected.value) {
    selectedLibraryUrls.value = selectedLibraryUrls.value.filter(url => !currentPageUrls.includes(url))
    return
  }

  const next = new Set(selectedLibraryUrls.value)
  currentPageUrls.forEach((url: string) => next.add(url))
  selectedLibraryUrls.value = Array.from(next)
  libraryFileList.value.forEach((item: any) => {
    if (!selectedLibraryItems.value.some(selected => selected.url === item.url)) {
      selectedLibraryItems.value.push(buildLibrarySelectionItem(item))
    }
  })
}

const clearLibrarySelection = () => {
  selectedLibraryUrls.value = []
  selectedLibraryItems.value = []
}

const removeLibrarySelection = (url: string) => {
  selectedLibraryUrls.value = selectedLibraryUrls.value.filter(item => item !== url)
  selectedLibraryItems.value = selectedLibraryItems.value.filter(item => item.url !== url)
}

const recentSelectedLibraryItems = computed(() => {
  return selectedLibraryItems.value.slice(-3).reverse()
})

const handleLibraryQuery = () => {
  libraryQuery.pageNum = 1
  getLibraryList()
}

const resetLibraryQuery = () => {
  libraryQuery.filename = ''
  libraryQuery.pageNum = 1
  getLibraryList()
}

const handleLibrarySizeChange = (val: number) => {
  libraryQuery.pageSize = val
  getLibraryList()
}

const handleLibraryCurrentChange = (val: number) => {
  libraryQuery.pageNum = val
  getLibraryList()
}

const confirmLibraryImport = async () => {
  if (!selectedLibraryUrls.value.length) {
    ElMessage.warning('请先选择要加入相册的图片')
    return
  }

  const existingUrls = new Set(photoList.value.map(item => item.url))
  const urlsToImport = selectedLibraryUrls.value.filter(url => !existingUrls.has(url))

  if (!urlsToImport.length) {
    ElMessage.warning('选中的图片当前页已经存在，无需重复加入')
    return
  }

  librarySubmitLoading.value = true
  try {
    const nextSort = photoList.value.reduce((max, item) => Math.max(max, Number(item.sort || 0)), 0) + 1
    await Promise.all(
      urlsToImport.map((url, index) => addPhotoApi({
        albumId: props.albumId,
        url,
        description: '',
        recordTime: '',
        sort: nextSort + index
      }))
    )

    const skippedCount = selectedLibraryUrls.value.length - urlsToImport.length
    ElMessage.success(
      skippedCount > 0
        ? `已加入 ${urlsToImport.length} 张图片，跳过 ${skippedCount} 张重复图片`
        : `已加入 ${urlsToImport.length} 张图片`
    )
    libraryDialog.visible = false
    selectedLibraryUrls.value = []
    selectedLibraryItems.value = []
    getList()
  } finally {
    librarySubmitLoading.value = false
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

  if (photoUploading.value) {
    ElMessage.warning('请等待图片上传完成后再保存')
    return
  }

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
  if (!val) {
    libraryDialog.visible = false
    selectedLibraryUrls.value = []
    selectedLibraryItems.value = []
  }
  emit('update:openPhotos', val)
}

watch(
  () => props.openPhotos,
  (visible) => {
    if (!visible || !props.albumId) return
    queryParams.pageNum = 1
    getAlbumList()
    getList()
  }
)

watch(
  () => props.albumId,
  (albumId) => {
    if (!props.openPhotos || !albumId) return
    queryParams.pageNum = 1
    resetForm()
    getList()
  }
)

watch(
  () => libraryDialog.visible,
  (visible) => {
    if (visible) return
    selectedLibraryUrls.value = []
    selectedLibraryItems.value = []
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

.photo-upload-hint--warning {
  color: var(--el-color-warning);
}

.library-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.library-toolbar :deep(.el-input) {
  flex: 1;
}

.library-summary {
  margin-bottom: 14px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.library-summary__actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 14px;
  flex-wrap: wrap;
}

.library-selection-tray {
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(64, 158, 255, 0.05), rgba(255, 255, 255, 0.98));
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.library-selection-tray__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.library-selection-tray__title {
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.library-selection-tray__count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.library-selection-tray__list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.library-selection-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
}

.library-selection-chip__image {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--el-fill-color);
}

.library-selection-chip__body {
  min-width: 0;
  flex: 1;
}

.library-selection-chip__name {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  word-break: break-word;
}

.library-selection-chip__meta {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.library-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.library-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  overflow: hidden;
  background: var(--el-bg-color);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
  }

  &.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.16);
  }
}

.library-card__media {
  position: relative;
}

.library-card__check {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  padding: 4px 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
}

.library-card__image {
  display: block;
  width: 100%;
  aspect-ratio: 1;
  background: var(--el-fill-color);
}

.library-card__body {
  padding: 12px;
}

.library-card__name {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
  word-break: break-word;
}

.library-card__meta {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.library-mobile-bar {
  display: none;
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

  .library-toolbar {
    flex-wrap: wrap;
  }

  .library-toolbar :deep(.el-input) {
    width: 100%;
    flex-basis: 100%;
  }

  .library-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .library-selection-tray {
    padding: 12px;
  }

  .library-selection-tray__header {
    flex-direction: column;
    align-items: flex-start;
  }

  .library-selection-tray__list {
    grid-template-columns: 1fr;
  }

  .library-summary__actions {
    align-items: stretch;

    :deep(.el-button) {
      flex: 1 1 auto;
      margin-left: 0;
    }
  }

  .library-mobile-bar {
    position: sticky;
    bottom: -1px;
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin: 14px -4px -4px;
    padding: 12px 14px calc(12px + env(safe-area-inset-bottom));
    border-top: 1px solid var(--el-border-color-lighter);
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(14px);
    box-shadow: 0 -12px 24px rgba(15, 23, 42, 0.08);
  }

  .library-mobile-bar__meta {
    min-width: 0;
    flex: 1;
  }

  .library-mobile-bar__previews {
    display: flex;
    align-items: center;
    margin-left: auto;
    padding-right: 4px;
  }

  .library-mobile-bar__preview {
    width: 34px;
    height: 34px;
    flex: 0 0 34px;
    border: 2px solid rgba(255, 255, 255, 0.96);
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 8px 16px rgba(15, 23, 42, 0.12);

    & + & {
      margin-left: -10px;
    }
  }

  .library-mobile-bar__title {
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
    line-height: 1.4;
  }

  .library-mobile-bar__desc {
    margin-top: 2px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.4;
  }

  .library-mobile-bar__actions {
    display: flex;
    gap: 8px;
    flex: 0 0 auto;
  }

  .library-mobile-bar__actions :deep(.el-button) {
    min-width: 88px;
  }

  .library-mobile-bar {
    flex-wrap: wrap;
  }

  .library-mobile-bar__meta {
    flex-basis: 100%;
  }

  .library-mobile-bar__previews {
    margin-left: 0;
    padding-right: 0;
  }

  :deep(.photo-library-dialog .el-dialog__footer) {
    display: none;
  }

  :deep(.photo-library-dialog) {
    border-radius: 0 !important;
  }
}
</style>
