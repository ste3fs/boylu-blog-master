<template>
  <el-container class="layout-container" :class="{ mobile: isMobile }">
    <el-aside
      v-if="!isMobile"
      :width="isCollapse ? '64px' : '220px'"
      class="transition-width"
    >
      <Sidebar :is-collapse="isCollapse" />
    </el-aside>

    <transition name="mobile-sidebar">
      <div
        v-if="isMobile && mobileMenuVisible"
        class="mobile-sidebar-mask"
        @click="closeMobileMenu"
      >
        <div class="mobile-sidebar-panel" @click.stop>
          <Sidebar :is-collapse="false" />
        </div>
      </div>
    </transition>

    <el-container class="content-shell">
      <el-header class="header">
        <Navbar
          :is-collapse="isCollapse"
          :is-mobile="isMobile"
          @toggle-collapse="handleToggleCollapse"
          @lock="handleLock"
          @theme-click="drawerVisible = true"
        />
      </el-header>

      <tags-view v-if="settingsStore.showTags && !isMobile" />

      <el-main class="main-container">
        <router-view v-slot="{ Component }">
          <transition
            :name="settingsStore.pageAnimation"
            mode="out-in"
            appear
          >
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="$route.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>

      <div class="theme-icon-container" @click="handleThemeClick">
        <el-icon class="theme-icon">
          <Setting />
        </el-icon>
      </div>

      <Footer v-if="settingsStore.showFooter" />
    </el-container>
  </el-container>

  <setting-drawer
    v-model:visible="drawerVisible"
    v-model:isCollapse="isCollapse"
  />

  <lock-screen ref="lockScreenRef" />

  <Watermark />
</template>

<script setup lang="ts">
import { useRoute } from "vue-router";
import TagsView from "@/components/TagsView/index.vue";
import SettingDrawer from "@/components/SettingDrawer/index.vue";
import Navbar from "@/layouts/components/Navbar/index.vue";
import Sidebar from "./components/Sidebar/index.vue";
import LockScreen from "@/components/LockScreen/index.vue";
import Watermark from "@/components/Watermark/index.vue";
import Footer from "@/components/Footer/index.vue";
import { Setting } from "@element-plus/icons-vue";

import { useSettingsStore } from "@/store";
import { useTagsViewStore } from "@/store/modules/tagsView";

const settingsStore = useSettingsStore();
const tagsViewStore = useTagsViewStore();
const route = useRoute();

const isCollapse = ref<boolean>(false);
const drawerVisible = ref(false);
const lockScreenRef = ref();
const isMobile = ref(false);
const mobileMenuVisible = ref(false);

const cachedViews = computed(() => tagsViewStore.cachedViews);

const checkMobile = () => {
  const mobile = window.innerWidth <= 768;
  isMobile.value = mobile;
  if (!mobile) {
    mobileMenuVisible.value = false;
    return;
  }
  isCollapse.value = false;
};

const handleToggleCollapse = () => {
  if (isMobile.value) {
    mobileMenuVisible.value = !mobileMenuVisible.value;
    return;
  }
  isCollapse.value = !isCollapse.value;
};

const closeMobileMenu = () => {
  mobileMenuVisible.value = false;
};

const handleThemeClick = () => {
  drawerVisible.value = true;
};

const handleLock = () => {
  lockScreenRef.value?.lock();
};

watch(
  () => route.fullPath,
  () => {
    if (isMobile.value) {
      mobileMenuVisible.value = false;
    }
  }
);

onMounted(() => {
  tagsViewStore.initTags();
  checkMobile();
  window.addEventListener("resize", checkMobile);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", checkMobile);
});
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
}

.content-shell {
  min-width: 0;
}

.transition-width {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.el-aside {
  background-color: #304156;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: var(--el-bg-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.main-container {
  padding: 16px;
  overflow-y: auto;
  background-color: var(--el-bg-color);
}

.theme-icon-container {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  background-color: var(--el-color-primary);
  padding: 10px;
  cursor: pointer;
  text-align: center;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
  z-index: 1000;
}

.theme-icon {
  font-size: 20px;
  color: #fff;
}

.mobile-sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(15, 23, 42, 0.45);
}

.mobile-sidebar-panel {
  width: min(82vw, 280px);
  height: 100vh;
  background: #304156;
  box-shadow: 12px 0 30px rgba(15, 23, 42, 0.22);
}

.mobile-sidebar-enter-active,
.mobile-sidebar-leave-active {
  transition: opacity 0.25s ease;
}

.mobile-sidebar-enter-active .mobile-sidebar-panel,
.mobile-sidebar-leave-active .mobile-sidebar-panel {
  transition: transform 0.25s ease;
}

.mobile-sidebar-enter-from,
.mobile-sidebar-leave-to {
  opacity: 0;
}

.mobile-sidebar-enter-from .mobile-sidebar-panel,
.mobile-sidebar-leave-to .mobile-sidebar-panel {
  transform: translateX(-100%);
}

@media (max-width: 768px) {
  .header {
    padding: 0 12px;
  }

  .main-container {
    padding: 12px;
  }

  .theme-icon-container {
    top: auto;
    bottom: 96px;
    transform: none;
    padding: 8px;
  }

  .theme-icon {
    font-size: 18px;
  }
}
</style>
