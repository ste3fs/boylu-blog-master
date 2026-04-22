<template>
  <div
    v-show="visible"
    class="context-menu"
    :class="{ 'menu-show': visible }"
    :style="{ left: adjustedX + 'px', top: adjustedY + 'px' }"
  >
    <div class="menu-item" @click="handleRefresh">
      <i class="fas fa-sync" style="color: #409eff"></i>
      刷新页面
    </div>
    <div class="menu-item" @click="handleBack">
      <i class="fas fa-arrow-left" style="color: #67c23a"></i>
      返回上一页
    </div>
    <div class="menu-item" @click="handleForward">
      <i class="fas fa-arrow-right" style="color: #e6a23c"></i>
      前进下一页
    </div>
    <div class="menu-item" @click="handleCopyUrl">
      <i class="fas fa-copy" style="color: #909399"></i>
      复制链接
    </div>
    <div class="divider"></div>
    <div class="menu-item" @click="toggleTheme">
      <i
        :class="['fas', isDark ? 'fa-sun' : 'fa-moon']"
        :style="{ color: isDark ? '#e6a23c' : '#409eff' }"
      ></i>
      {{ isDark ? '浅色模式' : '深色模式' }}
    </div>
  </div>
</template>

<script>
import { copyText } from '@/utils/contact'
import { getThemeMode, setThemeMode } from '@/utils/theme'

export default {
  name: 'ContextMenu',
  data() {
    return {
      visible: false,
      x: 0,
      y: 0,
      menuWidth: 0,
      menuHeight: 0,
      isDark: false,
      excludeSelectors: ['.chat-messages', '.image-preview']
    }
  },
  computed: {
    adjustedX() {
      const windowWidth = window.innerWidth
      return this.x + this.menuWidth > windowWidth ? this.x - this.menuWidth : this.x
    },
    adjustedY() {
      const windowHeight = window.innerHeight
      return this.y + this.menuHeight > windowHeight ? this.y - this.menuHeight : this.y
    }
  },
  created() {
    this.$nextTick(() => {
      this.isDark = getThemeMode() === 'dark'
    })
  },
  methods: {
    show(event) {
      const isInExcludeArea = this.excludeSelectors.some(selector => {
        const element = event.target.closest(selector)
        return element !== null
      })

      if (isInExcludeArea) {
        return
      }

      event.preventDefault()
      this.x = event.clientX
      this.y = event.clientY
      this.visible = true

      this.$nextTick(() => {
        this.menuWidth = this.$el.offsetWidth
        this.menuHeight = this.$el.offsetHeight
        this.x = event.clientX
        this.y = event.clientY
      })
    },
    hide() {
      this.visible = false
    },
    handleRefresh() {
      window.location.reload()
      this.hide()
    },
    handleBack() {
      this.$router.back()
      this.hide()
    },
    handleForward() {
      this.$router.forward()
      this.hide()
    },
    async handleCopyUrl() {
      const copied = await copyText(window.location.href)
      if (copied) {
        this.$message.success('链接已复制到剪贴板')
      } else {
        this.$message.error('复制失败，请手动复制')
      }
      this.hide()
    },
    toggleTheme() {
      this.isDark = !this.isDark
      const mode = this.isDark ? 'dark' : 'light'
      setThemeMode(mode)
      this.hide()
    }
  }
}
</script>

<style scoped>
.context-menu {
  position: fixed;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 5px 0;
  z-index: 9999;
  opacity: 0;
  transform: scale(0.95);
  transform-origin: top left;
}

.menu-show {
  animation: showMenu 0.2s ease forwards;
}

@keyframes showMenu {
  from {
    opacity: 0;
    transform: scale(0.95);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}

.menu-item:hover {
  background-color: var(--hover-bg);
}

.menu-item i {
  width: 14px;
}

.divider {
  height: 1px;
  background-color: var(--border-color);
  margin: 5px 0;
}
</style>
