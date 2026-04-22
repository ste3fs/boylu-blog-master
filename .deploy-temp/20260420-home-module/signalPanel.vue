<template>
  <section class="signal-panel" :class="{ 'sidebar-mode': sidebarMode }">
    <article class="panel-card hero-card">
      <div class="hero-copy">
        <span class="eyebrow">BOYLU LAB</span>
        <h2>{{ phrases[phraseIndex] }}</h2>
        <p>{{ summaryText }}</p>
        <div class="chip-row">
          <span v-for="chip in chips" :key="chip" class="chip">{{ chip }}</span>
        </div>
        <div class="action-row">
          <button class="primary-btn" @click="go('/about')">关于我</button>
          <button class="ghost-btn" @click="go('/login')">去登录</button>
          <button class="ghost-btn" @click="go('/resources')">资源库</button>
        </div>
      </div>
      <div class="hero-visual">
        <div class="hero-visual__glow hero-visual__glow--a"></div>
        <div class="hero-visual__glow hero-visual__glow--b"></div>
        <StackedPanelsGallery embedded />
        <div class="hero-avatar-badge">
          <span class="hero-avatar-badge__label">Online</span>
          <div class="hero-avatar-badge__avatar">
            <img :src="avatarSrc" alt="boylu avatar" />
          </div>
        </div>
      </div>
    </article>

    <article class="panel-card status-card">
      <div class="card-title">
        <span class="live-dot"></span>
        今日状态
      </div>
      <div class="metric-grid">
        <div class="metric-box">
          <span class="metric-label">访客</span>
          <strong>{{ visitorCount }}</strong>
        </div>
        <div class="metric-box">
          <span class="metric-label">浏览</span>
          <strong>{{ viewCount }}</strong>
        </div>
        <div class="metric-box">
          <span class="metric-label">分类</span>
          <strong>{{ categoryCount }}</strong>
        </div>
        <div class="metric-box">
          <span class="metric-label">北京时间</span>
          <strong>{{ liveClock }}</strong>
        </div>
      </div>
      <div class="ticker-row">
        <span class="ticker-tag">今日提示</span>
        <p>{{ tickerText }}</p>
      </div>
    </article>

    <article class="panel-card wechat-card">
      <div class="card-title">公众号扫码登录</div>
      <ol class="wechat-steps">
        <li>打开门户登录页，切换到“扫码登录”。</li>
        <li>微信扫描你的公众号二维码并先关注公众号。</li>
        <li>向公众号发送页面上的 <code>DLxxxx</code> 登录码。</li>
        <li>页面会自动轮询并完成登录，不需要手动刷新。</li>
      </ol>
      <div class="wechat-footer">
        <div class="account-box">
          <span class="account-label">微信号</span>
          <strong>{{ wechatValue || "待配置" }}</strong>
        </div>
        <div class="wechat-actions">
          <button class="primary-btn" @click="go('/login')">打开登录页</button>
          <button
            class="ghost-btn"
            :disabled="!wechatValue"
            @click="copyValue('微信号', wechatValue)"
          >
            复制微信号
          </button>
        </div>
      </div>
    </article>
  </section>
</template>

<script>
import StackedPanelsGallery from "@/components/common/StackedPanelsGallery.vue";
import { copyText } from "@/utils/contact";
import { resolveImageUrl } from "@/utils/image";

export default {
  name: "SignalPanel",
  components: {
    StackedPanelsGallery,
  },
  props: {
    categoryCount: {
      type: Number,
      default: 0,
    },
    sidebarMode: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      liveClock: "",
      phraseIndex: 0,
      clockTimer: null,
      phraseTimer: null,
      phrases: [
        "主页不只是文章列表，也该带一点个人气味。",
        "把技术栈、状态感和一点小趣味摆到台前。",
        "欢迎来到 boylu 的实验台，慢慢逛，随手聊。",
      ],
      chips: ["Vue 3", "Java", "Linux", "AI", "WeChat"],
    };
  },
  computed: {
    siteInfo() {
      return this.$store.state.webSiteInfo || {};
    },
    avatarSrc() {
      return this.resolveSafeAvatar([
        this.siteInfo.authorAvatar,
        this.siteInfo.touristAvatar,
      ]);
    },
    summaryText() {
      return (
        this.siteInfo.summary ||
        this.siteInfo.authorInfo ||
        "记录开发、部署、折腾和持续优化的过程。"
      );
    },
    visitorCount() {
      return this.$store.state.visitorAccess || 0;
    },
    viewCount() {
      return this.$store.state.siteAccess || 0;
    },
    tickerText() {
      return `欢迎来到 ${this.siteInfo.name || "boylu博客"}，这里会持续更新实战记录、部署笔记和新点子。`;
    },
    wechatValue() {
      return this.siteInfo.wechat || "";
    },
  },
  created() {
    this.updateClock();
  },
  mounted() {
    this.clockTimer = setInterval(this.updateClock, 1000);
    this.phraseTimer = setInterval(() => {
      this.phraseIndex = (this.phraseIndex + 1) % this.phrases.length;
    }, 4200);
  },
  beforeDestroy() {
    clearInterval(this.clockTimer);
    clearInterval(this.phraseTimer);
  },
  methods: {
    go(path) {
      this.$router.push(path);
    },
    updateClock() {
      const now = new Date();
      const pad = (value) => String(value).padStart(2, "0");
      this.liveClock = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(
        now.getSeconds()
      )}`;
    },
    resolveSafeAvatar(candidates = []) {
      const match = candidates
        .map((url) => (url || "").toString().trim())
        .find(Boolean);

      return resolveImageUrl(match, this.siteInfo.logo || this.$store.state.defaultImage);
    },
    async copyValue(label, value) {
      if (!value) {
        this.$message.warning(`${label}还没有配置`);
        return;
      }
      const copied = await copyText(value);
      if (copied) {
        this.$message.success(`${label}已复制`);
      } else {
        this.$message.error(`复制${label}失败`);
      }
    },
  },
};
</script>

<style lang="scss" scoped>
.signal-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 1fr);
  gap: $spacing-lg;
  margin-bottom: $spacing-xl;
}

.panel-card {
  position: relative;
  overflow: hidden;
  border-radius: 26px;
  padding: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.hero-card {
  min-height: 300px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(260px, 360px);
  gap: 24px;
  background:
    radial-gradient(circle at top left, rgba(56, 189, 248, 0.22), transparent 36%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.18), transparent 28%),
    linear-gradient(135deg, rgba(15, 23, 42, 0.95), rgba(37, 99, 235, 0.9));
  color: #f8fafc;
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  letter-spacing: 0.16em;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h2 {
  margin: 18px 0 12px;
  font-size: clamp(26px, 3vw, 38px);
  line-height: 1.2;
}

.hero-copy p {
  max-width: 640px;
  color: rgba(248, 250, 252, 0.82);
  font-size: 15px;
  line-height: 1.75;
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 20px 0 26px;
}

.chip {
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 13px;
  color: #e2e8f0;
}

.action-row,
.wechat-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.primary-btn,
.ghost-btn {
  border: none;
  border-radius: 14px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, background 0.25s ease;
}

.primary-btn {
  background: linear-gradient(135deg, #38bdf8, #2563eb);
  color: #fff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.25);
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.12);
  color: inherit;
}

.ghost-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.primary-btn:hover,
.ghost-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.hero-visual {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: center;
  min-height: 100%;
}

.hero-visual__glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(26px);
  pointer-events: none;
}

.hero-visual__glow--a {
  top: 8%;
  left: -8%;
  width: 120px;
  height: 120px;
  background: rgba(#38bdf8, 0.22);
}

.hero-visual__glow--b {
  right: -6%;
  bottom: 2%;
  width: 140px;
  height: 140px;
  background: rgba(#8b5cf6, 0.18);
}

.hero-visual .stacked-panels-scene {
  height: 100%;
}

.hero-visual .stacked-panels-scene--embedded {
  width: 100%;
}

.hero-avatar-badge {
  position: absolute;
  right: 18px;
  bottom: 18px;
  z-index: 2;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.48);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.2);
  backdrop-filter: blur(14px);
}

.hero-avatar-badge__label {
  color: rgba(248, 250, 252, 0.82);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-avatar-badge__avatar {
  width: 42px;
  height: 42px;
  padding: 3px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(#38bdf8, 0.9), rgba(#8b5cf6, 0.9));
}

.hero-avatar-badge__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.status-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.live-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 8px rgba(16, 185, 129, 0.12);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-box {
  padding: 16px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(241, 245, 249, 0.9));
}

.metric-label {
  display: block;
  margin-bottom: 10px;
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.metric-box strong {
  font-size: 22px;
  color: #0f172a;
}

.ticker-row {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(14, 165, 233, 0.08));
}

.ticker-tag {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.ticker-row p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.wechat-card {
  grid-column: 1 / -1;
  background:
    radial-gradient(circle at top right, rgba(34, 197, 94, 0.14), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
}

.wechat-steps {
  margin: 0;
  padding-left: 20px;
  color: var(--text-secondary);
  line-height: 1.9;
}

.wechat-steps code {
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.06);
  color: #0f172a;
  font-weight: 700;
}

.wechat-footer {
  margin-top: 18px;
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
}

.account-box {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.account-label {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.account-box strong {
  font-size: 20px;
  color: #0f172a;
}

.signal-panel.sidebar-mode {
  grid-template-columns: 1fr;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.signal-panel.sidebar-mode .panel-card {
  padding: 20px;
  border-radius: 22px;
}

.signal-panel.sidebar-mode .hero-card {
  min-height: auto;
  grid-template-columns: 1fr;
}

.signal-panel.sidebar-mode .hero-copy h2 {
  font-size: 28px;
}

.signal-panel.sidebar-mode .hero-copy p {
  font-size: 14px;
}

.signal-panel.sidebar-mode .hero-visual {
  min-height: 200px;
}

.signal-panel.sidebar-mode .wechat-card {
  grid-column: auto;
}

@include responsive(lg) {
  .signal-panel {
    grid-template-columns: 1fr;
  }

  .hero-card {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    min-height: 240px;
  }

  .wechat-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}

@include responsive(sm) {
  .panel-card {
    padding: 20px;
    border-radius: 22px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    min-height: 188px;
  }

  .hero-avatar-badge {
    right: 12px;
    bottom: 12px;
    padding: 8px 12px;
  }

  .hero-avatar-badge__avatar {
    width: 36px;
    height: 36px;
  }
}

@include responsive(md) {
  .signal-panel {
    display: none;
  }
}
</style>

