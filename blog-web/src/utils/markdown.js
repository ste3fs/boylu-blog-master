import { marked } from 'marked'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import go from 'highlight.js/lib/languages/go'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import php from 'highlight.js/lib/languages/php'
import python from 'highlight.js/lib/languages/python'
import scss from 'highlight.js/lib/languages/scss'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'

const registeredLanguages = [
  ['bash', bash, ['shell', 'sh', 'powershell', 'ps1']],
  ['css', css],
  ['go', go, ['golang']],
  ['java', java],
  ['javascript', javascript, ['js', 'jsx']],
  ['json', json],
  ['markdown', markdown, ['md']],
  ['php', php],
  ['python', python, ['py']],
  ['scss', scss, ['sass']],
  ['sql', sql],
  ['typescript', typescript, ['ts', 'tsx']],
  ['xml', xml, ['html', 'vue']],
  ['yaml', yaml, ['yml']]
]

registeredLanguages.forEach(([name, language, aliases]) => {
  hljs.registerLanguage(name, language)
  if (aliases) {
    hljs.registerAliases(aliases, { languageName: name })
  }
})

hljs.configure({
  ignoreUnescapedHTML: true
})

function resolveLanguage(language) {
  if (!language) {
    return ''
  }

  const normalized = String(language).trim().toLowerCase()
  return hljs.getLanguage(normalized) ? normalized : ''
}

marked.setOptions({
  breaks: true,
  highlight(code, language) {
    const resolvedLanguage = resolveLanguage(language)
    if (resolvedLanguage) {
      return hljs.highlight(code, {
        language: resolvedLanguage,
        ignoreIllegals: true
      }).value
    }

    return hljs.highlightAuto(code).value
  }
})

export function renderMarkdown(content) {
  return marked.parse(content || '')
}

export function highlightCodeBlocks(root = document) {
  root.querySelectorAll('pre code').forEach((block) => {
    hljs.highlightElement(block)
  })
}

export { hljs, marked }
