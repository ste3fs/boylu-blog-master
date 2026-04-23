<template>
  <el-button
    link
    size="small"
    v-bind="$attrs"
    :type="type"
    :icon="icon"
    @click="emit('click', $event)"
  >
    <slot />
  </el-button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  type?: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger'
  icon?: any
}>(), {
  type: 'primary',
  icon: undefined
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()
</script>
