<template>
  <div :class="groupClass">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  kind?: 'main' | 'danger' | 'utility'
}>(), {
  kind: 'main'
})

const groupClass = computed(() => [
  'page-toolbar-group',
  `page-toolbar-group--${props.kind}`
])
</script>

<style scoped>
.page-toolbar-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  flex: 0 0 auto;
}

.page-toolbar-group--danger {
  margin-left: 4px;
}

.page-toolbar-group--utility {
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-toolbar-group {
    flex-wrap: nowrap;
  }

  .page-toolbar-group--danger {
    margin-left: 0;
  }
}
</style>
