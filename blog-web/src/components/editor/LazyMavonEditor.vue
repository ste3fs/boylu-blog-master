<template>
  <div class="markdown-editor">
    <div class="markdown-editor__toolbar">
      <button type="button" title="加粗" @click="wrapSelection('**', '**', '加粗文字')">
        <strong>B</strong>
      </button>
      <button type="button" title="斜体" @click="wrapSelection('*', '*', '斜体文字')">
        <em>I</em>
      </button>
      <button type="button" title="标题" @click="insertLinePrefix('## ')">H</button>
      <button type="button" title="引用" @click="insertLinePrefix('> ')">”</button>
      <button type="button" title="代码块" @click="insertCodeBlock">{"</button>
      <button type="button" title="链接" @click="insertLink">↗</button>
      <button type="button" title="图片" @click="openImagePicker">▧</button>
      <span class="markdown-editor__spacer"></span>
      <button
        type="button"
        :class="{ active: mode === 'write' }"
        title="编辑"
        @click="mode = 'write'"
      >
        写
      </button>
      <button
        type="button"
        :class="{ active: mode === 'split' }"
        title="分屏"
        @click="mode = 'split'"
      >
        分
      </button>
      <button
        type="button"
        :class="{ active: mode === 'preview' }"
        title="预览"
        @click="mode = 'preview'"
      >
        预
      </button>
      <input ref="imageInput" type="file" accept="image/*" @change="handleImageSelected" />
    </div>

    <div class="markdown-editor__body" :class="`mode-${mode}`">
      <textarea
        v-show="mode !== 'preview'"
        ref="textarea"
        class="markdown-editor__input"
        :value="value"
        :placeholder="placeholder"
        spellcheck="false"
        @input="handleInput"
      ></textarea>
      <div
        v-show="mode !== 'write'"
        class="markdown-editor__preview markdown-body"
        v-html="renderedHtml"
      ></div>
    </div>
  </div>
</template>

<script>
import { renderMarkdown } from '@/utils/markdown'

export default {
  name: 'LazyMavonEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      mode: 'split',
      uploadSeq: 0,
      uploadTokens: Object.create(null)
    }
  },
  computed: {
    renderedHtml() {
      return renderMarkdown(this.value || '')
    },
    d_render() {
      return this.renderedHtml
    }
  },
  methods: {
    handleInput(event) {
      this.$emit('input', event.target.value)
    },
    updateValue(nextValue, selectionStart, selectionEnd) {
      this.$emit('input', nextValue)
      this.$nextTick(() => {
        const textarea = this.$refs.textarea
        if (!textarea || selectionStart === undefined || selectionEnd === undefined) {
          return
        }
        textarea.focus()
        textarea.setSelectionRange(selectionStart, selectionEnd)
      })
    },
    getSelection() {
      const textarea = this.$refs.textarea
      const value = this.value || ''
      if (!textarea) {
        return { start: value.length, end: value.length, selected: '' }
      }
      const start = textarea.selectionStart
      const end = textarea.selectionEnd
      return {
        start,
        end,
        selected: value.slice(start, end)
      }
    },
    replaceSelection(text, cursorOffset = text.length) {
      const value = this.value || ''
      const { start, end } = this.getSelection()
      const nextValue = value.slice(0, start) + text + value.slice(end)
      const cursor = start + cursorOffset
      this.updateValue(nextValue, cursor, cursor)
    },
    wrapSelection(before, after, fallback) {
      const value = this.value || ''
      const { start, end, selected } = this.getSelection()
      const content = selected || fallback
      const replacement = `${before}${content}${after}`
      const nextValue = value.slice(0, start) + replacement + value.slice(end)
      const nextStart = start + before.length
      const nextEnd = nextStart + content.length
      this.updateValue(nextValue, nextStart, nextEnd)
    },
    insertLinePrefix(prefix) {
      const value = this.value || ''
      const { start } = this.getSelection()
      const lineStart = value.lastIndexOf('\n', Math.max(0, start - 1)) + 1
      const nextValue = value.slice(0, lineStart) + prefix + value.slice(lineStart)
      const cursor = start + prefix.length
      this.updateValue(nextValue, cursor, cursor)
    },
    insertCodeBlock() {
      this.replaceSelection('```js\n\n```', 6)
    },
    insertLink() {
      this.replaceSelection('[链接文字](https://)', 1)
    },
    openImagePicker() {
      if (this.$refs.imageInput) {
        this.$refs.imageInput.click()
      }
    },
    handleImageSelected(event) {
      const file = event.target.files && event.target.files[0]
      event.target.value = ''
      if (!file) {
        return
      }
      const id = `image-${Date.now()}-${++this.uploadSeq}`
      const token = `boylu-upload://${id}`
      this.uploadTokens[id] = token
      this.replaceSelection(`![${file.name || 'image'}](${token})`)
      this.$emit('imgAdd', [id], file)
    },
    $img2Url(pos, url) {
      const id = Array.isArray(pos) ? pos[0] : pos
      const token = this.uploadTokens[id]
      if (!token) {
        return
      }
      const nextValue = (this.value || '').replace(token, url || '')
      delete this.uploadTokens[id]
      this.updateValue(nextValue)
    }
  }
}
</script>

<style lang="scss" scoped>
.markdown-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 500px;
  overflow: hidden;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 10px;
  background: var(--card-bg, #fff);
}

.markdown-editor__toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--border-color, #e5e7eb);
  background: rgba(248, 250, 252, 0.86);
}

.markdown-editor__toolbar button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 6px;
  color: var(--text-primary, #111827);
  background: transparent;
  cursor: pointer;
}

.markdown-editor__toolbar button:hover,
.markdown-editor__toolbar button.active {
  background: rgba(64, 158, 255, 0.12);
  color: var(--theme-color, #409eff);
}

.markdown-editor__toolbar input {
  display: none;
}

.markdown-editor__spacer {
  flex: 1;
}

.markdown-editor__body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 0;
  flex: 1;
}

.markdown-editor__body.mode-write,
.markdown-editor__body.mode-preview {
  grid-template-columns: 1fr;
}

.markdown-editor__input,
.markdown-editor__preview {
  min-width: 0;
  min-height: 0;
  padding: 18px;
  overflow: auto;
}

.markdown-editor__input {
  resize: none;
  border: 0;
  outline: none;
  color: var(--text-primary, #111827);
  background: transparent;
  font: 14px/1.7 ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.markdown-editor__preview {
  border-left: 1px solid var(--border-color, #e5e7eb);
  color: var(--text-primary, #111827);
  line-height: 1.75;
}

.mode-preview .markdown-editor__preview {
  border-left: 0;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.markdown-body :deep(pre) {
  overflow: auto;
  padding: 14px;
  border-radius: 8px;
  background: #111827;
}

.markdown-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

@media (max-width: 768px) {
  .markdown-editor__body {
    grid-template-columns: 1fr;
  }

  .markdown-editor__preview {
    border-left: 0;
    border-top: 1px solid var(--border-color, #e5e7eb);
  }
}
</style>
