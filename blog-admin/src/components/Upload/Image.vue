<template>
  <div class="upload-image">
    <el-upload
      v-model:file-list="fileList"
      class="upload-image__list"
      list-type="picture-card"
      accept="image/jpeg,image/png,image/gif,image/webp"
      :multiple="multiple"
      :limit="limit"
      :http-request="handleUploadRequest"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      :on-preview="handlePreview"
      :on-exceed="handleExceed"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>

    <div class="el-upload__tip upload-image__tip">
      可上传不超过 {{ fileSize }}MB 的 jpg/png/gif/webp 图片，上传时会自动压缩后再发布
    </div>

    <el-dialog v-model="previewVisible" title="预览图片" width="min(92vw, 720px)" append-to-body>
      <img class="upload-image__preview" :src="previewUrl" alt="preview" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, type PropType } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type {
  UploadFile,
  UploadFiles,
  UploadProps,
  UploadRequestOptions,
  UploadUserFile
} from 'element-plus'
import { deleteFileApi, uploadApi } from '@/api/file'
import { getImageName, normalizeImageList, normalizeImageUrl } from '@/utils/image'
import {
  DEFAULT_IMAGE_UPLOAD_LIMIT_MB,
  compressImageBeforeUpload,
  validateImageFile
} from '@/utils/upload-image'

const props = defineProps({
  modelValue: {
    type: [String, Array] as PropType<string | string[]>,
    default: ''
  },
  limit: {
    type: Number,
    default: 1
  },
  fileSize: {
    type: Number,
    default: DEFAULT_IMAGE_UPLOAD_LIMIT_MB
  },
  multiple: {
    type: Boolean,
    default: false
  },
  source: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['update:modelValue', 'uploading-change'])

const fileList = ref<UploadUserFile[]>([])
const previewVisible = ref(false)
const previewUrl = ref('')

const shouldEmitArrayValue = () => props.multiple || props.limit > 1 || Array.isArray(props.modelValue)

const isTemporaryUrl = (url: string) => url.startsWith('blob:') || url.startsWith('data:')

const resolvePersistedUrl = (item: Partial<UploadFile> & { response?: any; url?: string }) => {
  const responseUrl = normalizeImageUrl(String(item.response?.data || ''))
  if (responseUrl && !isTemporaryUrl(responseUrl)) {
    return responseUrl
  }

  const normalizedUrl = normalizeImageUrl(String(item.url || ''))
  if (normalizedUrl && !isTemporaryUrl(normalizedUrl)) {
    return normalizedUrl
  }

  return ''
}

const buildFileList = (value: string | string[] | undefined | null) => {
  return normalizeImageList(value).map((url, index) => ({
    name: getImageName(url) || `image-${index + 1}`,
    url
  }))
}

const mergePersistedAndPendingFiles = (persistedList: UploadUserFile[]) => {
  const pendingItems = fileList.value.filter((item) => {
    const status = String(item.status || '')
    return status === 'ready' || status === 'uploading'
  })

  if (!pendingItems.length) {
    return persistedList
  }

  return [...persistedList, ...pendingItems]
}

const syncFromFileList = (list: UploadUserFile[]) => {
  const urls = list
    .map((item) => resolvePersistedUrl(item))
    .filter(Boolean)

  emit('update:modelValue', shouldEmitArrayValue() ? urls : (urls[0] || ''))
}

const pendingUploadCount = computed(() => {
  return fileList.value.filter((item) => {
    const status = String(item.status || '')
    return status === 'ready' || status === 'uploading'
  }).length
})

const handlePreview: UploadProps['onPreview'] = (file) => {
  previewUrl.value = resolvePersistedUrl(file) || String(file.url || '')
  previewVisible.value = true
}

const handleRemove: UploadProps['onRemove'] = async (file, uploadFiles) => {
  const url = resolvePersistedUrl(file)

  if (url) {
    await deleteFileApi(url).catch(() => undefined)
  }

  fileList.value = uploadFiles
    .map((item) => ({
      ...item,
      name: item.name || getImageName(resolvePersistedUrl(item)),
      url: resolvePersistedUrl(item) || String(item.url || '')
    }))

  syncFromFileList(fileList.value)
}

const handleSuccess = (response: any, uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  const currentUrl = normalizeImageUrl(String(response?.data || uploadFile.url || ''))

  if (!currentUrl) {
    ElMessage.error('上传成功，但未获取到图片地址')
    return
  }

  uploadFile.url = currentUrl
  uploadFile.name = getImageName(currentUrl) || uploadFile.name

  fileList.value = uploadFiles
    .map((item) => ({
      ...item,
      name: item.name || getImageName(String(item.url || (item.response as any)?.data || '')),
      url: resolvePersistedUrl(item) || String(item.url || '')
    }))

  syncFromFileList(fileList.value)
}

const handleExceed = () => {
  ElMessage.warning(limitMessage(props.limit))
}

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const validation = validateImageFile(rawFile, props.fileSize)
  if (!validation.valid) {
    ElMessage.error(validation.message)
    return false
  }

  return true
}

const handleUploadRequest = async (options: UploadRequestOptions) => {
  try {
    const compressedFile = await compressImageBeforeUpload(options.file as File)
    const formData = new FormData()
    formData.append('file', compressedFile)

    options.onProgress({ percent: 35 } as any)
    const response = await uploadApi(formData, props.source)
    options.onProgress({ percent: 100 } as any)
    options.onSuccess(response as any)
  } catch (error: any) {
    if (!error?.response) {
      ElMessage.error(error?.message || '上传失败')
    }
    options.onError(error)
  }
}

const limitMessage = (limit: number) => {
  if (limit <= 1) {
    return '只能上传 1 张图片'
  }
  return `最多上传 ${limit} 张图片`
}

watch(
  () => props.modelValue,
  (value) => {
    const nextList = buildFileList(value as string | string[] | undefined | null)
    fileList.value = mergePersistedAndPendingFiles(nextList)
  },
  { immediate: true, deep: true }
)

watch(
  pendingUploadCount,
  (count) => {
    emit('uploading-change', count > 0)
  },
  { immediate: true }
)
</script>

<style scoped lang="scss">
.upload-image {
  width: 100%;
}

.upload-image :deep(.el-upload--picture-card),
.upload-image :deep(.el-upload-list__item) {
  border-radius: 18px;
}

.upload-image__tip {
  margin-top: 10px;
  line-height: 1.6;
}

.upload-image__preview {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .upload-image :deep(.el-upload--picture-card),
  .upload-image :deep(.el-upload-list__item) {
    width: 88px;
    height: 88px;
  }

  .upload-image__tip {
    font-size: 12px;
  }
}
</style>
