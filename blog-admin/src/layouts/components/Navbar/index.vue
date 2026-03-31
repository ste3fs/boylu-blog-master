<template>
  <div class="navbar-container">
    <div class="navbar-left">
      <el-icon class="collapse-btn" @click="toggleCollapse">
        <Fold v-if="!isCollapse" />
        <Expand v-else />
      </el-icon>

      <Breadcrumb v-if="!isMobile" />
    </div>

    <div class="navbar-right">
      <global-search v-if="!isMobile" />

      <el-tooltip v-if="!isMobile" content="全屏切换" placement="bottom">
        <el-icon class="setting-icon" @click="toggleFullscreen">
          <FullScreen v-if="!isFullscreen" />
          <svg-icon name="exitFullscreen" v-else />
        </el-icon>
      </el-tooltip>

      <el-tooltip v-if="!isMobile" content="通知中心" placement="bottom">
        <notification />
      </el-tooltip>

      <user-tool @lock="handleLock" />
    </div>

    <lock-screen ref="lockScreenRef" />
  </div>
</template>

<script setup lang="ts">
import screenfull from "screenfull";
import Breadcrumb from "./Breadcrumb/index.vue";
import GlobalSearch from "@/components/GlobalSearch/index.vue";
import UserTool from "./UserTool/index.vue";
import LockScreen from "@/components/LockScreen/index.vue";
import Notification from "./Notification/index.vue";
import { Expand, Fold, FullScreen } from "@element-plus/icons-vue";

defineProps({
  isCollapse: {
    type: Boolean,
    required: true,
  },
  isMobile: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["toggle-collapse"]);

const lockScreenRef = ref();
const isFullscreen = ref(false);

const toggleCollapse = () => {
  emit("toggle-collapse");
};

const handleLock = () => {
  lockScreenRef.value?.lock();
};

const toggleFullscreen = () => {
  if (screenfull.isEnabled) {
    screenfull.toggle();
    isFullscreen.value = !isFullscreen.value;
  }
};
</script>

<style lang="scss" scoped>
.navbar-container {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.navbar-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  margin-right: 16px;
  color: #606266;
  transition: all 0.3s;

  &:hover {
    color: var(--el-color-primary);
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  min-width: 0;
}

.setting-icon {
  font-size: 20px !important;
  cursor: pointer;
  padding: 6px;
  border-radius: 50%;
  transition: all 0.3s;
  color: #606266;

  &:hover {
    background-color: rgba(64, 158, 255, 0.1);
    color: var(--el-color-primary);
  }
}

@media (max-width: 768px) {
  .navbar-container {
    gap: 8px;
  }

  .collapse-btn {
    margin-right: 0;
  }

  .navbar-right {
    gap: 4px;
  }
}
</style>
