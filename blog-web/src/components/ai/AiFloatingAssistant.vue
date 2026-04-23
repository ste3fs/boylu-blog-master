<template>
  <div v-if="visible" class="ai-floating">
    <transition name="ai-float-panel">
      <div v-if="open" class="ai-floating__panel">
        <div class="ai-floating__panel-header">
          <div>
            <p>Boylu AI</p>
            <h3>边看边问</h3>
          </div>
          <button @click="open = false">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="ai-floating__panel-body">
          <AiConversation :embedded="true" :show-sidebar="false" />
        </div>
      </div>
    </transition>

    <button class="ai-floating__launcher" :class="{ 'is-open': open }" @click="toggle">
      <i class="fas" :class="open ? 'fa-times' : 'fa-robot'"></i>
      <span>{{ open ? '收起 AI' : 'AI 提问' }}</span>
    </button>
  </div>
</template>

<script>
import AiConversation from '@/components/ai/AiConversation.vue'

export default {
  name: 'AiFloatingAssistant',
  components: {
    AiConversation
  },
  data() {
    return {
      open: false,
      isMobileViewport: false
    }
  },
  computed: {
    visible() {
      return this.isHomeRoute && !this.isMobileViewport
    },
    isHomeRoute() {
      return this.$route && this.$route.name === 'Home'
    }
  },
  watch: {
    '$route.name'(value) {
      if (value !== 'Home') {
        this.open = false
      }
    }
  },
  mounted() {
    this.syncViewportState()
    window.addEventListener('resize', this.syncViewportState, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewportState)
  },
  methods: {
    syncViewportState() {
      this.isMobileViewport = window.innerWidth <= 768
      if (this.isMobileViewport) {
        this.open = false
      }
    },
    toggle() {
      this.open = !this.open
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-floating {
  position: fixed;
  right: 20px;
  bottom: 154px;
  z-index: 1003;
}

.ai-floating__launcher {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
  box-shadow: none;
  overflow: hidden;
  isolation: isolate;
  min-width: 0;
  min-height: 0;
  height: 44px;
}

.ai-floating__launcher::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  padding: 1px;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0.2)),
    conic-gradient(from 0deg, transparent 0deg, transparent 280deg, rgba(255, 57, 110, 0.92) 330deg, transparent 360deg);
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: ai-star-orbit 3.2s linear infinite;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.12);
}

.ai-floating__launcher::after {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: inherit;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(250, 250, 252, 0.92));
  z-index: 0;
}

.ai-floating__launcher.is-open::before {
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.74), rgba(255, 255, 255, 0.2)),
    conic-gradient(from 0deg, transparent 0deg, transparent 280deg, rgba(85, 145, 255, 0.92) 330deg, transparent 360deg);
}

.ai-floating__launcher .fas,
.ai-floating__launcher span {
  position: relative;
  z-index: 1;
}

.ai-floating__launcher .fas {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #0f172a;
  font-size: 14px;
}

.ai-floating__launcher span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 18px 0 0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.ai-floating__panel {
  position: absolute;
  right: 0;
  bottom: 70px;
  width: min(440px, calc(100vw - 28px));
  height: min(700px, calc(100vh - 180px));
  border-radius: 28px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.22);
  border: 1px solid rgba(148, 163, 184, 0.18);
  backdrop-filter: blur(18px);
}

.ai-floating__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 18px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.ai-floating__panel-header p,
.ai-floating__panel-header h3 {
  margin: 0;
}

.ai-floating__panel-header p {
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #60a5fa;
}

.ai-floating__panel-header h3 {
  margin-top: 4px;
  color: #0f172a;
}

.ai-floating__panel-header button {
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.14);
  color: #334155;
  cursor: pointer;
}

.ai-floating__panel-body {
  height: calc(100% - 71px);
}

.ai-float-panel-enter-active,
.ai-float-panel-leave-active {
  transition: all 0.22s ease;
}

.ai-float-panel-enter,
.ai-float-panel-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

@keyframes ai-star-orbit {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .ai-floating {
    display: none;
  }
}
</style>
