<template>
  <div class="page-toolbar">
    <div class="page-toolbar__left">
      <slot />
    </div>
    <div v-if="$slots.right" class="page-toolbar__right">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.page-toolbar__left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.page-toolbar__right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
  margin-left: auto;
}

@media (max-width: 768px) {
  .page-toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .page-toolbar__left,
  .page-toolbar__right {
    width: 100%;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 2px;
    scrollbar-width: none;
  }

  .page-toolbar__left::-webkit-scrollbar,
  .page-toolbar__right::-webkit-scrollbar {
    display: none;
  }

  .page-toolbar__left :deep(.el-button),
  .page-toolbar__right :deep(.el-button) {
    flex: 0 0 auto;
    min-height: 36px;
    padding-inline: 14px;
  }
}
</style>
