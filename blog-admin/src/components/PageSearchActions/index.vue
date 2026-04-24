<template>
  <div class="page-search-actions">
    <el-button :type="searchType" :icon="Search" @click="emit('search')">
      {{ searchText }}
    </el-button>
    <el-button :icon="Refresh" @click="emit('reset')">
      {{ resetText }}
    </el-button>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { Refresh, Search } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  searchText?: string
  resetText?: string
  searchType?: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger'
}>(), {
  searchText: '搜索',
  resetText: '重置',
  searchType: 'primary'
})

const emit = defineEmits<{
  search: []
  reset: []
}>()
</script>

<style scoped>
.page-search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .page-search-actions {
    width: 100%;
  }

  .page-search-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
    min-height: 38px;
  }
}
</style>
