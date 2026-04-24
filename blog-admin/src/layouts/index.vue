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
          <div class="mobile-sidebar-header">
            <div class="mobile-sidebar-title">导航菜单</div>
            <el-button circle text class="mobile-sidebar-close" @click="closeMobileMenu">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
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

      <div v-if="!isMobile" class="theme-icon-container" @click="handleThemeClick">
        <el-icon class="theme-icon">
          <Setting />
        </el-icon>
      </div>

      <Footer v-if="settingsStore.showFooter && !isMobile" />
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
import { Close, Setting } from "@element-plus/icons-vue";

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
let mobileTableObserver: MutationObserver | null = null;
let mobileTableFrame = 0;

const cachedViews = computed(() => tagsViewStore.cachedViews);

const updateMobileTables = () => {
  const tables = document.querySelectorAll<HTMLElement>(".main-container .el-table");

  tables.forEach((table) => {
    if (!isMobile.value) {
      table.classList.remove("admin-mobile-card-table");
      table
        .querySelectorAll<HTMLElement>("[data-mobile-label]")
        .forEach((cell) => {
          cell.removeAttribute("data-mobile-label");
          cell.classList.remove("is-mobile-action-cell", "is-mobile-select-cell");
        });
      return;
    }

    table.classList.add("admin-mobile-card-table");

    const headerCells = Array.from(
      table.querySelectorAll<HTMLElement>(".el-table__header-wrapper th")
    );
    const labels = headerCells.map((header, index) => {
      const text = header.innerText.trim();
      if (text) return text;
      if (header.classList.contains("el-table-column--selection")) return "选择";
      if (index === 0) return "选择";
      return "内容";
    });

    table
      .querySelectorAll<HTMLElement>(".el-table__body-wrapper tbody tr")
      .forEach((row) => {
        Array.from(row.children).forEach((cell, index) => {
          const tableCell = cell as HTMLElement;
          const label = labels[index] || "内容";

          tableCell.setAttribute("data-mobile-label", label);
          tableCell.classList.toggle("is-mobile-action-cell", label.includes("操作"));
          tableCell.classList.toggle("is-mobile-select-cell", label.includes("选择"));
        });
      });
  });
};

const scheduleMobileTableUpdate = () => {
  if (mobileTableFrame) {
    window.cancelAnimationFrame(mobileTableFrame);
  }

  mobileTableFrame = window.requestAnimationFrame(() => {
    mobileTableFrame = 0;
    updateMobileTables();
  });
};

const checkMobile = () => {
  const mobile = window.innerWidth <= 768;
  isMobile.value = mobile;
  if (!mobile) {
    mobileMenuVisible.value = false;
    nextTick(scheduleMobileTableUpdate);
    return;
  }
  isCollapse.value = false;
  nextTick(scheduleMobileTableUpdate);
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
    nextTick(scheduleMobileTableUpdate);
  }
);

onMounted(() => {
  tagsViewStore.initTags();
  checkMobile();
  window.addEventListener("resize", checkMobile);
  mobileTableObserver = new MutationObserver(scheduleMobileTableUpdate);
  const mainContainer = document.querySelector(".main-container");
  if (mainContainer) {
    mobileTableObserver.observe(mainContainer, {
      childList: true,
      subtree: true,
    });
  }
  nextTick(scheduleMobileTableUpdate);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", checkMobile);
  mobileTableObserver?.disconnect();
  if (mobileTableFrame) {
    window.cancelAnimationFrame(mobileTableFrame);
  }
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
  overflow: hidden;
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
  width: min(68vw, 236px);
  max-width: calc(100vw - 104px);
  height: 100vh;
  background: #304156;
  box-shadow: 12px 0 30px rgba(15, 23, 42, 0.22);
  display: flex;
  flex-direction: column;
}

.mobile-sidebar-header {
  height: 56px;
  padding: 0 12px 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(10px);
}

.mobile-sidebar-title {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.mobile-sidebar-close {
  color: rgba(255, 255, 255, 0.86);
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
  .layout-container {
    height: 100dvh;
  }

  .header {
    height: 56px;
    padding: 0 10px 0 12px;
  }

  .main-container {
    padding: 10px;
    overscroll-behavior: contain;
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
