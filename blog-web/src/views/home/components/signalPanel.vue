<template>
  <section class="signal-panel" :class="{ 'sidebar-mode': sidebarMode }">
    <article class="panel-card status-card">
      <div class="status-head">
        <div>
          <span class="eyebrow">Today</span>
          <h2>今日状态</h2>
        </div>
        <span class="live-pill">
          <span class="live-dot"></span>
          实时
        </span>
      </div>

      <div class="metric-grid">
        <div class="metric-box">
          <span class="metric-label">{{ visitorLabel }}</span>
          <strong>{{ visitorCount }}</strong>
        </div>
        <div class="metric-box">
          <span class="metric-label">{{ viewLabel }}</span>
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

      <GlobeWeather />
    </article>
  </section>
</template>

<script>
import GlobeWeather from "@/components/common/GlobeWeather.vue";

export default {
  name: "SignalPanel",
  components: {
    GlobeWeather,
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
      clockTimer: null,
    };
  },
  computed: {
    hasDailyStats() {
      return this.$store.state.dailyVisitorAccess !== null
        && this.$store.state.dailySiteAccess !== null;
    },
    visitorLabel() {
      return this.hasDailyStats ? "今日访客" : "总访客";
    },
    viewLabel() {
      return this.hasDailyStats ? "今日浏览" : "总浏览";
    },
    visitorCount() {
      return this.hasDailyStats
        ? (this.$store.state.dailyVisitorAccess || 0)
        : (this.$store.state.visitorAccess || 0);
    },
    viewCount() {
      return this.hasDailyStats
        ? (this.$store.state.dailySiteAccess || 0)
        : (this.$store.state.siteAccess || 0);
    },
  },
  created() {
    this.updateClock();
  },
  mounted() {
    this.clockTimer = setInterval(this.updateClock, 1000);
  },
  beforeDestroy() {
    clearInterval(this.clockTimer);
  },
  methods: {
    updateClock() {
      const now = new Date();
      const pad = (value) => String(value).padStart(2, "0");
      this.liveClock = `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(
        now.getSeconds()
      )}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.signal-panel {
  display: grid;
  grid-template-columns: 1fr;
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.panel-card {
  position: relative;
  overflow: hidden;
  border-radius: 26px;
  padding: 22px;
  background:
    radial-gradient(circle at 85% 10%, rgba(59, 130, 246, 0.13), transparent 32%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(248, 250, 252, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.status-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;

  h2 {
    margin: 6px 0 0;
    color: #0f172a;
    font-size: 24px;
    line-height: 1.2;
  }
}

.eyebrow {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.live-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
  font-size: 12px;
  font-weight: 700;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 6px rgba(16, 185, 129, 0.12);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-box {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.08);
}

.metric-label {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.metric-box strong {
  color: #0f172a;
  font-size: 21px;
}

.signal-panel.sidebar-mode {
  grid-template-columns: 1fr;
}

@include responsive(md) {
  .signal-panel {
    display: none;
  }
}
</style>
