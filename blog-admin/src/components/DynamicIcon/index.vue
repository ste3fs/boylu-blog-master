<template>
  <component :is="iconComponent" v-if="iconComponent" />
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { shallowRef, watch } from 'vue'
import { resolveElementPlusIcon } from '@/utils/element-icons'

const props = withDefaults(defineProps<{
  name?: string
}>(), {
  name: ''
})

const iconComponent = shallowRef<Component | null>(null)

watch(
  () => props.name,
  async (iconName) => {
    iconComponent.value = await resolveElementPlusIcon(iconName)
  },
  { immediate: true }
)
</script>
