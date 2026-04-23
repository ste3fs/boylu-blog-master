<template>
  <div class="card-header">
    <div class="page-toolbar__left">
      <slot />
    </div>
    <div v-if="$slots.right" class="page-toolbar__right">
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped>
.page-toolbar__left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.page-toolbar__right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
