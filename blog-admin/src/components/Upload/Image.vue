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
      可上传不超过 {{ fileSize }}MB 的 jpg/png/gif/webp 图片，保持原图原格式上传
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
import {
  abortChunkUploadApi,
  completeChunkUploadApi,
  deleteFileApi,
  initChunkUploadApi,
  uploadApi,
  uploadChunkPartApi
} from '@/api/file'
import { reportPerfApi } from '@/api/perf'
import { getImageName, normalizeImageList, normalizeImageUrl } from '@/utils/image'
import {
  DEFAULT_IMAGE_UPLOAD_LIMIT_MB,
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

const emit = defineEmits(['update:modelValue', 'uploading-change', 'metadata-change'])

const fileList = ref<UploadUserFile[]>([])
const previewVisible = ref(false)
const previewUrl = ref('')

const shouldEmitArrayValue = () => props.multiple || props.limit > 1 || Array.isArray(props.modelValue)

const isTemporaryUrl = (url: string) => url.startsWith('blob:') || url.startsWith('data:')

const pickVariantUrl = (variants: Record<string, Record<string | number, string>> = {}) => {
  const groups = [variants.jpg, variants.webp, variants.avif]
  for (const group of groups) {
    if (!group) {
      continue
    }
    const widths = Object.keys(group)
      .map(width => Number(width))
      .filter(width => Number.isFinite(width) && group[width])
      .sort((a, b) => Math.abs(a - 960) - Math.abs(b - 960))
    if (widths.length) {
      return group[widths[0]]
    }
  }
  return ''
}

const resolveUploadPayload = (payload: any) => {
  return payload && typeof payload === 'object' && 'data' in payload ? payload.data : payload
}

const extractUploadUrl = (payload: any) => {
  const data = resolveUploadPayload(payload)
  if (typeof data === 'string') {
    return normalizeImageUrl(data)
  }
  if (data && typeof data === 'object') {
    return normalizeImageUrl(String(data.fallback || pickVariantUrl(data.variants) || ''))
  }
  return ''
}

const extractUploadMetadata = (payload: any) => {
  const data = resolveUploadPayload(payload)
  return data && typeof data === 'object' ? data : null
}

const resolvePersistedUrl = (item: Partial<UploadFile> & { response?: any; url?: string }) => {
  const responseUrl = extractUploadUrl(item.response)
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

const CHUNK_THRESHOLD = 2 * 1024 * 1024
const DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024

const computeFileHash = async (file: File) => {
  if (!(window.crypto && window.crypto.subtle)) {
    return ''
  }
  const buffer = await file.arrayBuffer()
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hashBuffer))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

const uploadSingleRequest = async (file: File, options: UploadRequestOptions) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await uploadApi(formData, props.source, {
    onUploadProgress: (event: ProgressEvent) => {
      if (!event.total) {
        return
      }
      const percent = Math.min(100, Math.max(0, Math.round((event.loaded / event.total) * 100)))
      options.onProgress({ percent } as any)
    }
  })
  return response
}

const uploadByChunk = async (file: File, options: UploadRequestOptions) => {
  const fileHash = await computeFileHash(file)
  const roughTotalChunks = Math.max(1, Math.ceil(file.size / DEFAULT_CHUNK_SIZE))
  const initRes = await initChunkUploadApi({
    fileName: file.name,
    totalSize: file.size,
    totalChunks: roughTotalChunks,
    fileHash,
    source: props.source
  })
  const initData = initRes?.data || {}
  const uploadId = String(initData.uploadId || '')
  if (!uploadId) {
    throw new Error('初始化分片上传失败')
  }

  const chunkSize = Number(initData.chunkSize) > 0 ? Number(initData.chunkSize) : DEFAULT_CHUNK_SIZE
  const concurrency = Math.min(5, Math.max(3, Number(initData.concurrency) || 4))
  const totalChunks = Math.max(1, Math.ceil(file.size / chunkSize))
  const uploadedChunks = new Set<number>((initData.uploadedChunks || []).map((item: any) => Number(item)))

  let completed = uploadedChunks.size
  options.onProgress({ percent: Math.min(99, Math.round((completed / totalChunks) * 100)) } as any)

  const chunkIndexes = Array.from({ length: totalChunks }, (_, index) => index)
    .filter((index) => !uploadedChunks.has(index))

  let failed = false
  const queue = [...chunkIndexes]

  const worker = async () => {
    while (queue.length && !failed) {
      const chunkIndex = queue.shift()
      if (chunkIndex === undefined) {
        return
      }
      const start = chunkIndex * chunkSize
      const end = Math.min(file.size, start + chunkSize)
      const blob = file.slice(start, end)
      const formData = new FormData()
      formData.append('uploadId', uploadId)
      formData.append('chunkIndex', String(chunkIndex))
      formData.append('file', blob, `${file.name}.part-${chunkIndex}`)
      try {
        await uploadChunkPartApi(formData)
        completed += 1
        options.onProgress({ percent: Math.min(99, Math.round((completed / totalChunks) * 100)) } as any)
      } catch (error) {
        failed = true
        throw error
      }
    }
  }

  try {
    await Promise.all(Array.from({ length: concurrency }, () => worker()))
    const completeRes = await completeChunkUploadApi(uploadId)
    options.onProgress({ percent: 100 } as any)
    return completeRes
  } catch (error) {
    await abortChunkUploadApi(uploadId).catch(() => undefined)
    throw error
  }
}

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
  emit('metadata-change', null)
}

const handleSuccess = (response: any, uploadFile: UploadFile, uploadFiles: UploadFiles) => {
  const currentUrl = extractUploadUrl(response) || normalizeImageUrl(String(uploadFile.url || ''))
  const metadata = extractUploadMetadata(response)

  if (!currentUrl) {
    ElMessage.error('上传成功，但未获取到图片地址')
    return
  }

  uploadFile.url = currentUrl
  uploadFile.name = getImageName(currentUrl) || uploadFile.name
  uploadFile.response = metadata ? { data: metadata } : response

  fileList.value = uploadFiles
    .map((item) => ({
      ...item,
      name: item.name || getImageName(String(item.url || (item.response as any)?.data || '')),
      url: resolvePersistedUrl(item) || String(item.url || '')
    }))

  syncFromFileList(fileList.value)
  emit('metadata-change', metadata)
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
  const startTime = typeof performance !== 'undefined' ? performance.now() : Date.now()
  try {
    options.onProgress({ percent: 5 } as any)
    const uploadFile = options.file as File
    const response = uploadFile.size > CHUNK_THRESHOLD
      ? await uploadByChunk(uploadFile, options)
      : await uploadSingleRequest(uploadFile, options)
    options.onSuccess(response as any)
    reportPerfApi({
      eventType: 'upload',
      durationMs: Math.max(1, Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startTime)),
      success: true
    }).catch(() => undefined)
  } catch (error: any) {
    reportPerfApi({
      eventType: 'upload',
      durationMs: Math.max(1, Math.round((typeof performance !== 'undefined' ? performance.now() : Date.now()) - startTime)),
      success: false
    }).catch(() => undefined)
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
