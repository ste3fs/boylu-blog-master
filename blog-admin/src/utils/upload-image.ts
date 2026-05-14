export const DEFAULT_IMAGE_UPLOAD_LIMIT_MB = 30

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])

export const validateImageFile = (file: File, maxSizeMB = DEFAULT_IMAGE_UPLOAD_LIMIT_MB) => {
  if (!ALLOWED_TYPES.has(file.type)) {
    return {
      valid: false,
      message: '只能上传 jpg、png、gif、webp 格式的图片'
    }
  }

  const isLtSize = file.size / 1024 / 1024 <= maxSizeMB
  if (!isLtSize) {
    return {
      valid: false,
      message: `上传图片大小不能超过 ${maxSizeMB}MB`
    }
  }

  return { valid: true, message: '' }
}

export const compressImageBeforeUpload = async (file: File) => {
  // 恢复旧上传方式：保持原始文件和原始格式，不在前端压缩或转 webp。
  return file
}

export const prepareImageFileForUpload = async (file: File, maxSizeMB = DEFAULT_IMAGE_UPLOAD_LIMIT_MB) => {
  const validation = validateImageFile(file, maxSizeMB)
  if (!validation.valid) {
    throw new Error(validation.message)
  }

  return compressImageBeforeUpload(file)
}
