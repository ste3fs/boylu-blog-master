<template>
  <div class="moments-list" v-if="moments.length > 0">
    <div class="moments-content">
      <div class="moments-row">
        <div class="moments-header">
          <i class="fas fa-comment-dots"></i>
          <span>最新说说:</span>
        </div>
        <transition name="fade" mode="out-in">
          <div class="moment-item" :key="currentIndex">
            <span class="moment-text" @click="goToMoments" v-html="$sanitizeHtml(moments[currentIndex].content)" />
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
import { getMoments } from '@/api/moments'

export default {
  name: 'MomentsList',
  data() {
    return {
      moments: [],
      currentIndex: 0,
      timer: null
    }
  },
  created() {
    this.getMomentsList()
  },
  mounted() {
    if (this.moments.length > 1) {
      this.startRotation()
    }
  },
  beforeDestroy() {
    this.stopRotation()
  },
  methods: {
    getMomentsList() {
      getMoments({ pageSize: 5, pageNum: 1 }).then(res => {
        if (res.data && res.data.records) {
          this.moments = res.data.records
          if (this.moments.length > 1) {
            this.startRotation()
          }
        }
      })
    },
    goToMoments() {
      this.$router.push('/moments')
    },
    startRotation() {
      if (this.timer) {
        clearInterval(this.timer)
      }
      if (this.moments.length < 2) {
        return
      }
      this.timer = setInterval(() => {
        this.currentIndex = (this.currentIndex + 1) % this.moments.length
      }, 4000) // 每4秒切换一次
    },
    stopRotation() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.moments-list {
  background: var(--card-bg);
  border-radius: $border-radius-lg;
  padding: $spacing-md $spacing-lg;
  margin-bottom: $spacing-xl;
  border: 1px solid rgba(var(--border-color-rgb), 0.08);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.05);
  overflow: hidden;
}

.moments-content {
  .moments-row {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }
}

.moments-header {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
  
  i {
    color: $primary;
    font-size: 1.2em;
  }
}

.moment-item {
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 0;
  max-height: 20px;
  overflow: hidden;
  .moment-text {
    color: var(--text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    width: 100%;
    cursor: pointer;
    line-height: 1.6;
    transition: color 0.2s ease;
    &:hover {
      color: $primary;
    }

    :deep(*) {
      color: inherit !important;
      background: transparent !important;
    }
  }
}

@media (max-width: 480px) {
  .moments-list {
    padding: 14px 16px;
  }

  .moments-content .moments-row {
    gap: 10px;
  }

  .moments-header {
    flex: 0 0 auto;
  }
}

// 添加过渡动画样式
.fade-enter-active,
.fade-leave-active {
  transition: all 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

.fade-enter-to,
.fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}
</style>
