<template>
  <div class="pagination-container">
    <el-pagination
      v-bind="attrs"
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      :background="background"
      :layout="layout"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup lang="ts">
import { useAttrs } from 'vue'

const props = withDefaults(defineProps<{
  currentPage: number
  pageSize: number
  total: number
  pageSizes?: number[]
  background?: boolean
  layout?: string
}>(), {
  pageSizes: () => [10, 20, 30, 50],
  background: true,
  layout: 'total, sizes, prev, pager, next, jumper'
})

const emit = defineEmits<{
  'update:currentPage': [value: number]
  'update:pageSize': [value: number]
  sizeChange: [value: number]
  currentChange: [value: number]
}>()

const attrs = useAttrs()

const handleSizeChange = (value: number) => {
  emit('update:pageSize', value)
  emit('sizeChange', value)
}

const handleCurrentChange = (value: number) => {
  emit('update:currentPage', value)
  emit('currentChange', value)
}
</script>
