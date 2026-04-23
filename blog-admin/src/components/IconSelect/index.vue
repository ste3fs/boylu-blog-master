<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择图标"
    width="800px"
    append-to-body
    top="5vh"
  >
    <div class="icon-container">
      <div class="search-bar">
        <el-input
          v-model="searchText"
          placeholder="搜索图标"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <el-scrollbar height="400px" v-loading="loading">
        <div class="icon-list">
          <div
            v-for="(component, name) in filteredIcons"
            :key="name"
            class="icon-item"
            :class="{ active: modelValue === name }"
            @click="selectIcon(name)"
          >
            <el-icon>
              <component :is="component" />
            </el-icon>
            <span class="icon-name">{{ name }}</span>
          </div>
        </div>
      </el-scrollbar>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { loadElementPlusIcons } from '@/utils/element-icons'

const props = defineProps<{
  modelValue: string
  visible: boolean
}>()

const emit = defineEmits(['update:modelValue', 'update:visible'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

const searchText = ref('')
const loading = ref(false)
const loaded = ref(false)
const icons = ref<Record<string, any>>({})

const ensureIconsLoaded = async () => {
  if (loaded.value || loading.value) {
    return
  }

  loading.value = true
  try {
    icons.value = await loadElementPlusIcons()
    loaded.value = true
  } finally {
    loading.value = false
  }
}

const filteredIcons = computed(() => {
  const iconEntries = Object.entries(icons.value)
  if (!searchText.value) return icons.value

  return Object.fromEntries(
    iconEntries.filter(([name]) =>
      name.toLowerCase().includes(searchText.value.toLowerCase())
    )
  )
})

const selectIcon = (iconName: string) => {
  emit('update:modelValue', iconName)
  emit('update:visible', false)
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      ensureIconsLoaded()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.icon-container {
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
}

.icon-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  padding: 12px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid transparent;
}

.icon-item:hover {
  background-color: #ecf5ff;
  border-color: #409EFF;
}

.icon-item.active {
  background-color: #ecf5ff;
  border-color: #409EFF;
  color: #409EFF;
}

.icon-item .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.icon-name {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
  text-align: center;
}

.icon-item.active .icon-name {
  color: #409EFF;
}
</style>
