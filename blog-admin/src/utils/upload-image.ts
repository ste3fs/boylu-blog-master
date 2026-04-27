export const DEFAULT_IMAGE_UPLOAD_LIMIT_MB = 30

const COMPRESSIBLE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp'])
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])
const COMPRESS_SKIP_SIZE = 512 * 1024
const COMPRESS_TARGET_SIZE = 5 * 1024 * 1024
const COMPRESS_MAX_DIMENSION = 2560
const COMPRESS_OUTPUT_TYPE = 'image/webp'

const renameToWebp = (name = 'image') => {
  const lastDotIndex = name.lastIndexOf('.')
  return lastDotIndex === -1 ? `${name}.webp` : `${name.slice(0, lastDotIndex)}.webp`
}

const loadImage = (file: File) => new Promise<HTMLImageElement>((resolve, reject) => {
  const url = URL.createObjectURL(file)
  const image = new Image()
  image.onload = () => {
    URL.revokeObjectURL(url)
    resolve(image)
  }
  image.onerror = () => {
    URL.revokeObjectURL(url)
    reject(new Error('图片解析失败'))
  }
  image.src = url
})

const canvasToBlob = (canvas: HTMLCanvasElement, type: string, quality: number) => {
  return new Promise<Blob | null>((resolve) => {
    canvas.toBlob((blob) => resolve(blob), type, quality)
  })
}

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
  if (!COMPRESSIBLE_TYPES.has(file.type) || file.size <= COMPRESS_SKIP_SIZE) {
    return file
  }

  const image = await loadImage(file)
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')

  if (!context) {
    return file
  }

  const longestSide = Math.max(image.width, image.height)
  const baseScale = longestSide > COMPRESS_MAX_DIMENSION ? COMPRESS_MAX_DIMENSION / longestSide : 1
  const scales = [1, 0.92, 0.84]
  const qualities = [0.86, 0.78, 0.7]
  let bestBlob: Blob | null = null

  for (const scale of scales) {
    const width = Math.max(1, Math.round(image.width * baseScale * scale))
    const height = Math.max(1, Math.round(image.height * baseScale * scale))

    canvas.width = width
    canvas.height = height
    context.clearRect(0, 0, width, height)
    context.drawImage(image, 0, 0, width, height)

    for (const quality of qualities) {
      const blob = await canvasToBlob(canvas, COMPRESS_OUTPUT_TYPE, quality)

      if (!blob) {
        continue
      }

      if (!bestBlob || blob.size < bestBlob.size) {
        bestBlob = blob
      }

      if (blob.size <= COMPRESS_TARGET_SIZE) {
        return new File([blob], renameToWebp(file.name), { type: COMPRESS_OUTPUT_TYPE })
      }
    }
  }

  if (bestBlob && bestBlob.size < file.size) {
    return new File([bestBlob], renameToWebp(file.name), { type: COMPRESS_OUTPUT_TYPE })
  }

  return file
}

export const prepareImageFileForUpload = async (file: File, maxSizeMB = DEFAULT_IMAGE_UPLOAD_LIMIT_MB) => {
  const validation = validateImageFile(file, maxSizeMB)
  if (!validation.valid) {
    throw new Error(validation.message)
  }

  return compressImageBeforeUpload(file)
}
