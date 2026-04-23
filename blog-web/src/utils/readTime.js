export const READING_CONFIG = {
  chineseCharsPerMinute: 400,
  englishWordsPerMinute: 220,
  codeLinesPerMinute: 25,
  imageMinutes: 0.25,
  tableRowsPerMinute: 12,
  defaultCodeComplexity: 1.0,
  defaultArticleComplexity: 1.1,
}

function normalizeContent(content = '') {
  return String(content || '')
}

function extractMarkdownCodeBlocks(content = '') {
  return normalizeContent(content).match(/```[\s\S]*?```/g) || []
}

function extractHtmlCodeBlocks(content = '') {
  return normalizeContent(content).match(/<pre[\s\S]*?<\/pre>/gi) || []
}

function countMarkdownCodeLines(block = '') {
  return String(block)
    .replace(/^```[a-zA-Z0-9_-]*\r?\n?/, '')
    .replace(/\r?\n?```$/, '')
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0)
    .length
}

function countHtmlCodeLines(block = '') {
  return String(block)
    .replace(/<pre[^>]*>/gi, '')
    .replace(/<\/pre>/gi, '')
    .replace(/<code[^>]*>/gi, '')
    .replace(/<\/code>/gi, '')
    .split(/\r?\n/)
    .filter((line) => line.replace(/<[^>]+>/g, '').trim().length > 0)
    .length
}

function countCodeLines(content = '') {
  const markdownBlocks = extractMarkdownCodeBlocks(content)
  const htmlBlocks = extractHtmlCodeBlocks(content)

  const markdownLines = markdownBlocks.reduce(
    (total, block) => total + countMarkdownCodeLines(block),
    0
  )
  const htmlLines = htmlBlocks.reduce(
    (total, block) => total + countHtmlCodeLines(block),
    0
  )

  return markdownLines + htmlLines
}

function stripCodeBlocks(content = '') {
  return normalizeContent(content)
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/<pre[\s\S]*?<\/pre>/gi, ' ')
}

function countImages(content = '') {
  const source = normalizeContent(content)
  const markdownImages = source.match(/!\[[^\]]*]\([^)]+\)/g) || []
  const htmlImages = source.match(/<img\b[^>]*>/gi) || []
  return markdownImages.length + htmlImages.length
}

function countTableRows(content = '') {
  const source = normalizeContent(content)
  const markdownRows = source
    .split(/\r?\n/)
    .filter((line) => /^\s*\|.+\|\s*$/.test(line))
    .filter((line) => !/^\s*\|?\s*:?-+:?\s*(\|\s*:?-+:?\s*)+\|?\s*$/.test(line))
    .length

  const htmlRows = (source.match(/<tr\b[^>]*>[\s\S]*?<\/tr>/gi) || []).length
  return markdownRows + htmlRows
}

function stripTextContent(content = '') {
  return stripCodeBlocks(content)
    .replace(/!\[([^\]]*)]\([^)]+\)/g, ' $1 ')
    .replace(/<img[^>]*alt="([^"]*)"[^>]*>/gi, ' $1 ')
    .replace(/<img[^>]*>/gi, ' ')
    .replace(/\[([^\]]*)]\([^)]+\)/g, ' $1 ')
    .replace(/<a[^>]*>([\s\S]*?)<\/a>/gi, ' $1 ')
    .replace(/https?:\/\/[^\s)]+/gi, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;|&amp;|&quot;|&#39;|&lt;|&gt;/gi, ' ')
    .replace(/[#>*_`~\-+=]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function countChineseChars(text = '') {
  return (String(text).match(/[\u4e00-\u9fa5]/g) || []).length
}

function countEnglishWords(text = '') {
  return (
    String(text)
      .replace(/[\u4e00-\u9fa5]/g, ' ')
      .match(/[a-zA-Z]+(?:'[a-zA-Z]+)?/g) || []
  ).length
}

export function estimateReadingTime(content = '', options = {}) {
  const config = {
    ...READING_CONFIG,
    ...options,
  }

  const source = normalizeContent(content)
  const textOnly = stripTextContent(source)

  if (!textOnly && !countCodeLines(source) && !countImages(source) && !countTableRows(source)) {
    return {
      minutes: 1,
      rawMinutes: 0,
      chineseChars: 0,
      englishWords: 0,
      codeLines: 0,
      images: 0,
      tableRows: 0,
      textMinutes: 0,
      codeMinutes: 0,
      imageMinutes: 0,
      tableMinutes: 0,
    }
  }

  const chineseChars = countChineseChars(textOnly)
  const englishWords = countEnglishWords(textOnly)
  const codeLines = countCodeLines(source)
  const images = countImages(source)
  const tableRows = countTableRows(source)

  const textMinutes =
    chineseChars / config.chineseCharsPerMinute +
    englishWords / config.englishWordsPerMinute

  const codeMinutes =
    (codeLines / config.codeLinesPerMinute) *
    (config.codeComplexity || config.defaultCodeComplexity)

  const imageMinutes = images * config.imageMinutes
  const tableMinutes = tableRows / config.tableRowsPerMinute

  const rawMinutes =
    (textMinutes + codeMinutes + imageMinutes + tableMinutes) *
    (config.articleComplexity || config.defaultArticleComplexity)

  const minutes = Math.max(1, Math.ceil(rawMinutes))

  return {
    minutes,
    rawMinutes,
    chineseChars,
    englishWords,
    codeLines,
    images,
    tableRows,
    textMinutes,
    codeMinutes,
    imageMinutes,
    tableMinutes,
  }
}

export function estimateReadMinutes(content = '', options = {}) {
  const { summaryOnly = false, minMinutes = 1, ...rest } = options
  const result = estimateReadingTime(content, rest)
  const minutes = Math.max(minMinutes, result.minutes)

  if (summaryOnly) {
    return Math.max(minMinutes, Math.min(3, minutes))
  }

  return minutes
}
