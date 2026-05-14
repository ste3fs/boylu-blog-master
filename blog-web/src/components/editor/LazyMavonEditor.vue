<template>
  <component
    :is="editorComponent"
    v-if="editorComponent"
    ref="innerEditor"
    v-bind="$attrs"
    v-on="$listeners"
    :value="value"
    @input="$emit('input', $event)"
  />
  <div v-else class="lazy-editor-skeleton" aria-hidden="true">
    <div class="lazy-editor-skeleton__toolbar"></div>
    <div class="lazy-editor-skeleton__body"></div>
  </div>
</template>

<script>
let editorLoader = null

function loadEditor() {
  if (!editorLoader) {
    editorLoader = Promise.all([
      import('mavon-editor'),
      import('mavon-editor/dist/css/index.css')
    ]).then(([module]) => module.mavonEditor)
  }
  return editorLoader
}

export default {
  name: 'LazyMavonEditor',
  inheritAttrs: false,
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      editorComponent: null
    }
  },
  computed: {
    d_render() {
      return this.$refs.innerEditor ? this.$refs.innerEditor.d_render : ''
    }
  },
  mounted() {
    loadEditor()
      .then(component => {
        this.editorComponent = component
      })
      .catch(() => {
        this.$emit('load-error')
      })
  },
  methods: {
    $img2Url(...args) {
      if (this.$refs.innerEditor && typeof this.$refs.innerEditor.$img2Url === 'function') {
        this.$refs.innerEditor.$img2Url(...args)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.lazy-editor-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 500px;
}

.lazy-editor-skeleton__toolbar,
.lazy-editor-skeleton__body {
  border-radius: 12px;
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08) 25%, rgba(64, 158, 255, 0.18) 37%, rgba(64, 158, 255, 0.08) 63%);
  background-size: 400% 100%;
  animation: lazyEditorShimmer 1.4s ease infinite;
}

.lazy-editor-skeleton__toolbar {
  height: 48px;
}

.lazy-editor-skeleton__body {
  flex: 1;
  min-height: 440px;
}

@keyframes lazyEditorShimmer {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
