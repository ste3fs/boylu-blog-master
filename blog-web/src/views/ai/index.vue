<template>
  <div class="ai-page">
    <div class="ai-mobile-bar">
      <button class="ai-mobile-bar__btn" @click="goBack">
        <i class="fas fa-chevron-left"></i>
      </button>
      <div class="ai-mobile-bar__title">AI 助手</div>
      <div class="ai-mobile-bar__actions">
        <button class="ai-mobile-bar__btn" @click="goHome">
          <i class="fas fa-house"></i>
        </button>
        <button class="ai-mobile-bar__btn" @click="openMenu">
          <i class="fas fa-bars"></i>
        </button>
      </div>
    </div>

    <section class="ai-page__shell">
      <AiConversation ref="conversationRef" theme="minimal-light" />
    </section>
  </div>
</template>

<script>
import AiConversation from '@/components/ai/AiConversation.vue'

export default {
  name: 'AiAssistantPage',
  components: {
    AiConversation
  },
  methods: {
    goBack() {
      if (window.history.length > 1) {
        this.$router.back()
        return
      }
      this.$router.push('/')
    },
    goHome() {
      this.$router.push('/')
    },
    openMenu() {
      if (window.innerWidth <= 768 && this.$refs.conversationRef?.toggleMobileSessionPanel) {
        this.$refs.conversationRef.toggleMobileSessionPanel()
        return
      }
      this.$store.commit('SET_MOBILE_MENU_VISIBLE', true)
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-page {
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100dvh;
  min-height: 100dvh;
  min-width: 0;
  padding: 18px;
  overflow: hidden;
  background: #ffffff;
}

.ai-mobile-bar {
  display: none;
}

.ai-page__shell {
  flex: 1;
  height: 100%;
  min-height: 0;
  width: 100%;
  min-width: 0;
  overflow: hidden;
}

.ai-page :deep(.ai-conversation) {
  height: 100%;
  min-height: 0;
}

@media (max-width: 768px) {
  .ai-page {
    padding:
      calc(10px + env(safe-area-inset-top, 0px))
      10px
      calc(10px + env(safe-area-inset-bottom, 0px));
  }

  .ai-mobile-bar {
    display: grid;
    grid-template-columns: 40px minmax(0, 1fr) auto;
    align-items: center;
    column-gap: 10px;
    margin-bottom: 8px;
    padding: 2px 0 4px;
  }

  .ai-mobile-bar__title {
    width: 100%;
    text-align: center;
    color: #0f172a;
    font-size: 16px;
    font-weight: 700;
    letter-spacing: -0.02em;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .ai-mobile-bar__actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 6px;
    min-width: 0;
  }

  .ai-mobile-bar__btn {
    width: 40px;
    height: 40px;
    border: 0;
    border-radius: 14px;
    background: #ffffff;
    color: #334155;
    box-shadow:
      inset 0 0 0 1px rgba(15, 23, 42, 0.08),
      0 10px 20px rgba(15, 23, 42, 0.05);
    cursor: pointer;
  }
}
</style>
