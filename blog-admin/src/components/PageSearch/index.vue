<template>
  <div class="search-wrapper" :class="{ 'search-wrapper--collapsed': isMobile && collapsed }">
    <button v-if="isMobile" type="button" class="search-toggle" @click="collapsed = !collapsed">
      <div class="search-toggle__left">
        <el-icon><Filter /></el-icon>
        <span>{{ collapsed ? "展开筛选" : "收起筛选" }}</span>
      </div>
      <el-icon class="search-toggle__arrow" :class="{ 'is-open': !collapsed }">
        <ArrowDown />
      </el-icon>
    </button>

    <div v-show="!isMobile || !collapsed" class="search-content">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, Filter } from "@element-plus/icons-vue";

const isMobile = ref(false);
const collapsed = ref(false);

const syncViewport = () => {
  isMobile.value = window.innerWidth <= 768;
  if (!isMobile.value) {
    collapsed.value = false;
    return;
  }

  if (!collapsed.value) {
    collapsed.value = true;
  }
};

onMounted(() => {
  syncViewport();
  window.addEventListener("resize", syncViewport);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", syncViewport);
});
</script>

<style scoped>
.search-toggle {
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.search-toggle__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-toggle__arrow {
  transition: transform 0.25s ease;
}

.search-toggle__arrow.is-open {
  transform: rotate(180deg);
}

.search-content {
  margin-top: 14px;
}

@media (min-width: 769px) {
  .search-content {
    margin-top: 0;
  }
}
</style>
