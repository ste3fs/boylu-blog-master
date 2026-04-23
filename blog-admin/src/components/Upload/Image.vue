<template>
  <div class="upload-container">
    <el-upload
      v-model:file-list="fileList"
      :action="uploadUrl"
      list-type="picture-card"
      :headers="headers"
      :multiple="multiple"
      :limit="limit"
      :on-preview="handlePreview"
      :on-remove="handleRemove"
      :on-success="handleSuccess"
      :on-exceed="handleExceed"
      :before-upload="beforeUpload"
    >
      <el-icon><Plus /></el-icon>
      <template #tip>
        <div class="upload-tip">
          只能上传 jpg/png/gif/webp 图片，且不超过 {{ fileSize }}MB
        </div>
      </template>
    </el-upload>

    <el-dialog v-model="dialogVisible" top="5vh" title="预览图片">
      <img class="preview-image" :src="dialogImageUrl" alt="Preview Image" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { UploadProps, UploadUserFile } from 'element-plus'
import { getToken } from '@/utils/auth'
import { deleteFileApi } from '@/api/file'
import { getImageName, normalizeImageList, normalizeImageUrl } from '@/utils/image'

const props = defineProps({
  modelValue: {
    type: [String, Array],
    default: ''
  },
  limit: {
    type: Number,
    default: 1
  },
  fileSize: {
    type: Number,
    default: 5
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

const emit = defineEmits(['update:modelValue'])

const uploadUrl = computed(() => {
  const source = encodeURIComponent(props.source || 'default')
  return `${import.meta.env.VITE_APP_BASE_API}/file/upload?source=${source}`
})

const headers = computed(() => ({
  Authorization: getToken()
}))

const fileList = ref<UploadUserFile[]>([])
const dialogImageUrl = ref('')
const dialogVisible = ref(false)

function toUploadFile(url: string): UploadUserFile {
  const normalizedUrl = normalizeImageUrl(url)
  return {
    name: getImageName(normalizedUrl),
    url: normalizedUrl
  }
}

function initFileList() {
  fileList.value = normalizeImageList(props.modelValue as string | string[]).map(toUploadFile)
}

const handlePreview: UploadProps['onPreview'] = (uploadFile) => {
  dialogImageUrl.value = normalizeImageUrl(uploadFile.url || '')
  dialogVisible.value = true
}

const handleRemove: UploadProps['onRemove'] = async (uploadFile) => {
  const removedUrl = normalizeImageUrl(uploadFile.url || '')
  if (!removedUrl) {
    return
  }

  await deleteFileApi(removedUrl)

  if (props.multiple) {
    const urls = normalizeImageList(props.modelValue as string[])
      .filter(url => normalizeImageUrl(url) !== removedUrl)
    emit('update:modelValue', urls)
    fileList.value = urls.map(toUploadFile)
    return
  }

  emit('update:modelValue', '')
  fileList.value = []
}

const handleSuccess: UploadProps['onSuccess'] = (response) => {
  if (response.code !== 200) {
    ElMessage.error(response.message || '上传失败')
    return
  }

  const url = normalizeImageUrl(response.data)
  if (!url) {
    ElMessage.error('上传成功，但没有返回有效图片地址')
    return
  }

  if (props.multiple) {
    const urls = normalizeImageList(props.modelValue as string[])
    urls.push(url)
    emit('update:modelValue', urls)
    fileList.value = urls.map(toUploadFile)
  } else {
    emit('update:modelValue', url)
    fileList.value = [toUploadFile(url)]
  }

  ElMessage.success('上传成功')
}

const handleExceed: UploadProps['onExceed'] = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}

const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = /^image\/(jpeg|png|gif|webp)$/.test(file.type)
  const isLt = file.size / 1024 / 1024 < props.fileSize

  if (!isImage) {
    ElMessage.error('只能上传 jpg/png/gif/webp 格式的图片')
    return false
  }
  if (!isLt) {
    ElMessage.error(`图片大小不能超过 ${props.fileSize}MB`)
    return false
  }
  return true
}

watch(() => props.modelValue, () => {
  initFileList()
}, { immediate: true })
</script>

<style scoped>
.upload-container {
  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }
}

.preview-image {
  width: 100%;
  height: 500px;
  object-fit: contain;
}

:deep(.el-upload--picture-card) {
  --el-upload-picture-card-size: 100px;
}

:deep(.el-upload-list--picture-card) {
  --el-upload-list-picture-card-size: 100px;
}
</style>
