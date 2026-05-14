<template>
  <div class="message-page">
    <section class="message-board">
      <div class="message-board__inner">
        <div class="message-board__head">
          <span class="message-board__eyebrow">Message Board</span>
          <h1>留言板</h1>
        </div>

        <div class="message-composer">
          <el-input
            v-model="content"
            class="message-composer__input"
            placeholder="说点什么吧"
            maxlength="80"
            show-word-limit
            @keyup.enter.native="addToList"
          />
          <el-button
            class="message-composer__button"
            type="primary"
            round
            :disabled="submitDisabled"
            @click="addToList"
          >
            发送
          </el-button>
        </div>

        <p class="message-board__meta">
          <span>{{ identityLabel }}</span>
          <span>{{ count ? `${count} 秒后可再次发送` : '发送后会实时加入留言墙' }}</span>
        </p>
      </div>

      <div class="message-wall">
        <div v-if="!barrageList.length" class="message-empty">
          <i class="fas fa-comment-dots"></i>
          <span>还没有留言，写下第一句吧</span>
        </div>

        <vue-danmaku
          v-else
          class="message-danmaku"
          :danmus="barrageList"
          useSlot
          :speeds="isMobile ? 95 : 120"
          :channels="isMobile ? 7 : 10"
        >
          <template v-slot:dm="{ danmu }">
            <span class="message-bubble">
              <img :src="resolveAvatar(danmu.avatar)" :alt="danmu.nickname || '游客'">
              <span class="message-bubble__main">
                <span class="message-bubble__top">
                  <strong>{{ danmu.nickname || '游客' }}</strong>
                  <time v-if="formatMessageTime(danmu.createTime)">
                    {{ formatMessageTime(danmu.createTime) }}
                  </time>
                </span>
                <span class="message-bubble__content">{{ danmu.content }}</span>
              </span>
            </span>
          </template>
        </vue-danmaku>
      </div>
    </section>
  </div>
</template>

<script>
import { getMessagesApi, addMessageApi } from '@/api/message'
import { formatDateTime } from '@/utils/time'
import { resolveImageUrl } from '@/utils/image'
import { getBrowserInfo } from '@/utils/browser'
import VueDanmaku from 'vue-danmaku'

export default {
  name: 'MessagesView',
  components: {
    VueDanmaku
  },
  data() {
    return {
      content: '',
      count: null,
      timer: null,
      barrageList: [],
      browserInfo: null,
      isMobile: typeof window !== 'undefined' ? window.innerWidth <= 768 : false
    }
  },
  computed: {
    user() {
      return this.$store.state.userInfo
    },
    submitDisabled() {
      return !this.content.trim() || Boolean(this.count)
    },
    identityLabel() {
      return this.user?.nickname || '游客'
    }
  },
  mounted() {
    this.browserInfo = getBrowserInfo()
    this.listMessage()
    window.addEventListener('resize', this.syncViewport)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  methods: {
    syncViewport() {
      this.isMobile = window.innerWidth <= 768
    },
    startCooldown() {
      const timeCount = 30
      if (this.timer) {
        return
      }

      this.count = timeCount
      this.timer = setInterval(() => {
        if (this.count > 1) {
          this.count -= 1
          return
        }

        clearInterval(this.timer)
        this.timer = null
        this.count = null
      }, 1000)
    },
    async addToList() {
      if (this.count) {
        this.$message.error('30 秒后才能再次留言')
        return
      }

      const text = this.content.trim()
      if (!text) {
        this.$message.error('留言内容不能为空')
        return
      }

      const message = {
        avatar: this.resolveAvatar(this.user ? this.user.avatar : this.$store.state.webSiteInfo.touristAvatar),
        nickname: this.user ? this.user.nickname : '游客',
        content: text,
        browser: this.browserInfo ? `${this.browserInfo.name} ${this.browserInfo.version}` : ''
      }
      const localMessage = {
        ...message,
        createTime: formatDateTime(new Date()) || new Date().toLocaleString()
      }

      this.content = ''

      try {
        await addMessageApi(message)
        this.barrageList = [...this.barrageList, localMessage]
        this.$message.success('留言成功')
        this.startCooldown()
      } catch (error) {
        this.$message.error(error.message || '留言失败')
      }
    },
    listMessage() {
      getMessagesApi().then((res) => {
        this.barrageList = Array.isArray(res.data) ? res.data : []
      }).catch(() => {
        this.barrageList = []
      })
    },
    formatMessageTime(time) {
      return formatDateTime(time)
    },
    resolveAvatar(url) {
      return resolveImageUrl(url, this.$store.state.webSiteInfo.touristAvatar || this.$store.state.defaultImage)
    }
  }
}
</script>

<style lang="scss" scoped>
.message-page {
  min-height: calc(100vh - 64px);
  background:
    radial-gradient(circle at 18% 16%, rgba(59, 130, 246, 0.34), transparent 30%),
    linear-gradient(120deg, #0f2b78 0%, #1d4ed8 48%, #36b7ef 100%);
}

.message-board {
  position: relative;
  min-height: calc(100vh - 64px);
  overflow: hidden;
  padding: 118px 24px 72px;
}

.message-board__inner {
  position: relative;
  z-index: 2;
  width: min(520px, calc(100vw - 32px));
  margin: 0 auto;
  text-align: center;
  color: #fff;
}

.message-board__eyebrow {
  display: inline-flex;
  margin-bottom: 14px;
  color: rgba(219, 234, 254, 0.82);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.message-board__head h1 {
  margin: 0 0 28px;
  color: #fff;
  font-size: clamp(32px, 4vw, 46px);
  line-height: 1;
  font-weight: 800;
  text-shadow: 0 10px 30px rgba(15, 23, 42, 0.22);
}

.message-composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 78px;
  gap: 10px;
  align-items: center;
}

.message-composer__input {
  :deep(.el-input__inner) {
    height: 46px;
    padding: 0 78px 0 18px;
    border: 1px solid rgba(255, 255, 255, 0.24);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.62);
    color: #172554;
    font-size: 14px;
    box-shadow: 0 16px 34px rgba(15, 23, 42, 0.14);

    &::placeholder {
      color: rgba(30, 58, 138, 0.48);
    }

    &:focus {
      border-color: rgba(255, 255, 255, 0.72);
      background: rgba(255, 255, 255, 0.82);
      box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
    }
  }

  :deep(.el-input__count) {
    right: 16px;
    color: rgba(30, 58, 138, 0.54);
    background: transparent;
  }
}

.message-composer__button {
  height: 46px;
  min-width: 78px;
  padding: 0 20px;
  border: 0;
  background: rgba(255, 255, 255, 0.62);
  color: #1e40af;
  font-weight: 700;
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.14);

  &:hover,
  &:focus {
    background: rgba(255, 255, 255, 0.86);
    color: #1d4ed8;
  }

  &.is-disabled {
    background: rgba(255, 255, 255, 0.42);
    color: rgba(30, 64, 175, 0.52);
  }
}

.message-board__meta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 16px 0 0;
  color: rgba(219, 234, 254, 0.84);
  font-size: 13px;

  span + span::before {
    content: '';
    display: inline-block;
    width: 4px;
    height: 4px;
    margin-right: 12px;
    border-radius: 50%;
    vertical-align: 2px;
    background: rgba(219, 234, 254, 0.6);
  }
}

.message-wall {
  position: absolute;
  z-index: 1;
  inset: 64px 0 0;
  pointer-events: none;
}

.message-danmaku {
  width: 100%;
  height: 100%;
}

.message-empty {
  position: absolute;
  left: 50%;
  top: 62%;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  transform: translateX(-50%);
  color: rgba(219, 234, 254, 0.82);
  font-size: 14px;

  i {
    font-size: 18px;
  }
}

.message-bubble {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: 460px;
  margin-top: 8px;
  padding: 8px 14px 8px 8px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.74);
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);

  img {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    flex: 0 0 36px;
    object-fit: contain;
    background: rgba(255, 255, 255, 0.95);
  }
}

.message-bubble__main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.message-bubble__top {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  strong {
    max-width: 120px;
    overflow: hidden;
    color: #fff;
    font-size: 13px;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  time {
    color: rgba(191, 219, 254, 0.76);
    font-size: 11px;
    white-space: nowrap;
  }
}

.message-bubble__content {
  color: rgba(241, 245, 249, 0.95);
  font-size: 13px;
  line-height: 1.38;
  word-break: break-word;
}

@media (max-width: 768px) {
  .message-page,
  .message-board {
    min-height: calc(100vh - 58px);
  }

  .message-board {
    padding: 118px 18px 56px;
  }

  .message-board__head h1 {
    margin-bottom: 24px;
    font-size: 34px;
  }

  .message-composer {
    grid-template-columns: minmax(0, 1fr) 64px;
    gap: 8px;
  }

  .message-composer__input {
    :deep(.el-input__inner) {
      height: 40px;
      padding: 0 58px 0 15px;
      font-size: 13px;
    }

    :deep(.el-input__count) {
      right: 12px;
      font-size: 11px;
    }
  }

  .message-composer__button {
    height: 40px;
    min-width: 64px;
    padding: 0 14px;
  }

  .message-board__meta {
    flex-wrap: wrap;
    gap: 8px;
    font-size: 12px;
  }

  .message-wall {
    inset: 58px 0 0;
  }

  .message-bubble {
    max-width: calc(100vw - 64px);
    gap: 8px;
    padding: 7px 12px 7px 7px;

    img {
      width: 31px;
      height: 31px;
      flex-basis: 31px;
    }
  }

  .message-bubble__top strong,
  .message-bubble__content {
    font-size: 12px;
  }

  .message-bubble__top time {
    font-size: 10px;
  }
}

@media (max-width: 390px) {
  .message-board {
    padding-left: 14px;
    padding-right: 14px;
  }

  .message-composer {
    grid-template-columns: 1fr;
  }

  .message-composer__button {
    width: 100%;
  }
}
</style>
