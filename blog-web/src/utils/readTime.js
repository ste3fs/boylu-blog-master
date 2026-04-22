function countCodeLines(blocks = []) {
  return blocks.reduce((total, block) => {
    const lines = String(block)
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean).length
    return total + lines
  }, 0)
}

function stripContent(value = '') {
  return String(value)
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/<pre[\s\S]*?<\/pre>/gi, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[([^\]]*)]\([^)]*\)/g, ' $1 ')
    .replace(/\[([^\]]*)]\([^)]*\)/g, ' $1 ')
    .replace(/<img[^>]*alt="([^"]*)"[^>]*>/gi, ' $1 ')
    .replace(/<img[^>]*>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;|&amp;|&quot;|&#39;|&lt;|&gt;/gi, ' ')
    .replace(/[#>*~_\-\n\r]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function estimateReadMinutes(content = '', options = {}) {
  const rawContent = String(content || '')
  const plainText = stripContent(rawContent)
  const {
    summaryOnly = false,
    minMinutes = 1,
  } = options

  if (!plainText) {
    return minMinutes
  }

  const markdownCodeBlocks = rawContent.match(/```[\s\S]*?```/g) || []
  const htmlCodeBlocks = rawContent.match(/<pre[\s\S]*?<\/pre>/gi) || []
  const imageCount = (
    rawContent.match(/!\[[^\]]*]\([^)]*\)|<img\b[^>]*>/gi) || []
  ).length
  const inlineCodeCount = (rawContent.match(/`[^`\n]+`/g) || []).length
  const tableRowCount = (rawContent.match(/^\s*\|.+\|\s*$/gm) || []).length
  const headingCount = (rawContent.match(/^\s{0,3}#{1,6}\s+/gm) || []).length

  const codeLineCount = countCodeLines([...markdownCodeBlocks, ...htmlCodeBlocks])
  const chineseCount = (plainText.match(/[\u4e00-\u9fff]/g) || []).length
  const latinWordCount = (
    plainText
      .replace(/[\u4e00-\u9fff]/g, ' ')
      .match(/[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*/g) || []
  ).length

  const textMinutes = chineseCount / 320 + latinWordCount / 220
  const codeMinutes = codeLineCount / 42 + inlineCodeCount / 90 + tableRowCount / 40
  const mediaMinutes = Math.min(imageCount * 0.12, 1) + headingCount * 0.02
  const totalMinutes = textMinutes + codeMinutes + mediaMinutes

  if (summaryOnly) {
    return Math.max(minMinutes, Math.min(3, Math.ceil(totalMinutes)))
  }

  return Math.max(minMinutes, Math.ceil(totalMinutes))
}
