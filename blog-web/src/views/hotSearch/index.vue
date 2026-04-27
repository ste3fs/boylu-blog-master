<template>
  <div class="hot-search-page">
    <section class="hot-search-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">Trending Radar</span>
        <h1>热搜聚合</h1>
        <p class="hero-summary">
          一页查看微博、知乎、头条、百度和 CSDN 的实时热榜，顺手还能切换常用搜索引擎继续搜。
        </p>

        <div class="hero-stats">
          <article class="hero-stat">
            <strong>{{ currentHotList.length }}</strong>
            <span>当前榜单条目</span>
          </article>
          <article class="hero-stat">
            <strong>{{ hotTabs.length }}</strong>
            <span>支持榜单来源</span>
          </article>
          <article class="hero-stat">
            <strong>{{ searchEngines.length }}</strong>
            <span>快捷搜索引擎</span>
          </article>
        </div>
      </div>

      <div class="hero-panel">
        <div class="search-box">
          <label class="search-label">继续搜索</label>
          <div class="search-input">
            <input
              v-model="searchText"
              :placeholder="`在 ${currentEngineMeta.label} 中搜索你关心的话题`"
              @keyup.enter="handleSearch"
            >
            <button class="search-btn" type="button" @click="handleSearch">
              <i class="fas fa-search"></i>
            </button>
          </div>
          <div class="engine-list">
            <button
              v-for="engine in searchEngines"
              :key="engine.name"
              type="button"
              class="engine-chip"
              :class="{ active: currentEngine === engine.name }"
              @click="currentEngine = engine.name"
            >
              <svg-icon :icon-class="engine.icon"></svg-icon>
              <span>{{ engine.label }}</span>
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="hot-search-board">
      <div class="board-header">
        <div>
          <p class="board-eyebrow">实时榜单</p>
          <h2>{{ currentTabMeta.label }}</h2>
        </div>
        <button class="refresh-btn" type="button" :disabled="loading" @click="fetchHotList(currentTab, true)">
          <i class="fas fa-rotate-right"></i>
          <span>{{ loading ? '刷新中' : '刷新榜单' }}</span>
        </button>
      </div>

      <div class="tab-list">
        <button
          v-for="tab in hotTabs"
          :key="tab.type"
          type="button"
          class="tab-chip"
          :class="{ active: currentTab === tab.type }"
          @click="currentTab = tab.type"
        >
          <i :class="tab.icon"></i>
          <span>{{ tab.label }}</span>
        </button>
      </div>

      <div v-if="errorMessage" class="status-panel status-panel--error">
        <i class="fas fa-circle-exclamation"></i>
        <div>
          <strong>热搜加载失败</strong>
          <p>{{ errorMessage }}</p>
        </div>
      </div>

      <div v-else-if="loading" class="status-panel">
        <i class="fas fa-spinner fa-spin"></i>
        <div>
          <strong>正在更新榜单</strong>
          <p>正在获取 {{ currentTabMeta.label }} 的最新内容</p>
        </div>
      </div>

      <div v-else-if="!currentHotList.length" class="status-panel">
        <i class="fas fa-signal"></i>
        <div>
          <strong>当前暂无热搜数据</strong>
          <p>这个榜单暂时没返回内容，稍后再刷新看看。</p>
        </div>
      </div>

      <div v-else class="hot-list">
        <article
          v-for="(item, index) in currentHotList"
          :key="`${currentTab}-${item.keyword}-${index}`"
          class="hot-card"
          :class="{ 'hot-card--top': index < 3 }"
          @click="handleHotItemClick(item)"
        >
          <div class="hot-rank" :class="`rank-${Math.min(index + 1, 4)}`">
            {{ index + 1 }}
          </div>

          <div class="hot-main">
            <div class="hot-title-row">
              <h3 class="hot-title">{{ item.keyword }}</h3>
              <span v-if="item.tag" class="hot-tag">{{ item.tag }}</span>
            </div>

            <p v-if="item.summary" class="hot-summary">
              {{ item.summary }}
            </p>

            <div class="hot-meta">
              <span v-if="item.hotValue" class="hot-value">
                <i class="fas fa-fire"></i>
                {{ formatHotValue(item.hotValue) }}
              </span>
              <span v-if="item.type" class="hot-type">{{ item.type }}</span>
              <span class="hot-trend" :class="getTrendClass(item.trend)">
                <i :class="getTrendIcon(item.trend)"></i>
                {{ getTrendLabel(item.trend) }}
              </span>
            </div>
          </div>

          <div class="hot-link">
            <i class="fas fa-arrow-up-right-from-square"></i>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script>
import { getHotListApi } from '@/api/hot-search'

export default {
  name: 'HotSearch',
  data() {
    return {
      searchText: '',
      currentEngine: 'baidu',
      currentTab: 'weibo',
      loading: false,
      errorMessage: '',
      searchEngines: [
        { name: 'baidu', label: '百度', icon: 'baidu', url: 'https://www.baidu.com/s?wd=' },
        { name: 'google', label: 'Google', icon: 'chrome', url: 'https://www.google.com/search?q=' },
        { name: 'gitee', label: 'Gitee', icon: 'gitee', url: 'https://search.gitee.com/?q=' },
        { name: 'github', label: 'GitHub', icon: 'github', url: 'https://github.com/search?q=' }
      ],
      hotTabs: [
        { type: 'weibo', label: '微博热搜', icon: 'fab fa-weibo' },
        { type: 'zhihu', label: '知乎热榜', icon: 'fab fa-zhihu' },
        { type: 'toutiao', label: '头条热点', icon: 'fas fa-bolt' },
        { type: 'baidu', label: '百度热搜', icon: 'fas fa-chart-line' },
        { type: 'csdn', label: 'CSDN 热榜', icon: 'fas fa-code' }
      ],
      hotLists: {
        weibo: [],
        zhihu: [],
        toutiao: [],
        baidu: [],
        csdn: []
      }
    }
  },
  computed: {
    currentEngineMeta() {
      return this.searchEngines.find(engine => engine.name === this.currentEngine) || this.searchEngines[0]
    },
    currentTabMeta() {
      return this.hotTabs.find(tab => tab.type === this.currentTab) || this.hotTabs[0]
    },
    currentHotList() {
      return this.hotLists[this.currentTab] || []
    }
  },
  watch: {
    currentTab: {
      immediate: true,
      handler(tab) {
        this.fetchHotList(tab)
      }
    }
  },
  methods: {
    normalizeHotItems(payload = {}) {
      const rawList = Array.isArray(payload?.data)
        ? payload.data
        : Array.isArray(payload?.list)
          ? payload.list
          : Array.isArray(payload)
            ? payload
            : []

      return rawList
        .map((item, index) => {
          const keyword = String(item.keyword || item.title || item.word || '').trim()
          if (!keyword) {
            return null
          }

          return {
            keyword,
            url: item.url || this.buildFallbackUrl(keyword),
            summary: item.summary || item.desc || '',
            tag: item.tag || item.label || '',
            hotValue: Number(item.hotValue || item.hot || item.num || 0) || 0,
            trend: item.trend || item.status || '',
            type: item.type || item.channel || '',
            rank: item.rank || index + 1
          }
        })
        .filter(Boolean)
    },
    buildFallbackUrl(keyword) {
      const tabSearchMap = {
        weibo: 'https://s.weibo.com/weibo?q=',
        zhihu: 'https://www.zhihu.com/search?type=content&q=',
        toutiao: 'https://so.toutiao.com/search?keyword=',
        baidu: 'https://www.baidu.com/s?wd=',
        csdn: 'https://so.csdn.net/so/search?q='
      }

      return `${tabSearchMap[this.currentTab] || tabSearchMap.baidu}${encodeURIComponent(keyword)}`
    },
    async fetchHotList(type, forceRefresh = false) {
      if (!forceRefresh && this.hotLists[type]?.length) {
        this.errorMessage = ''
        return
      }

      this.loading = true
      this.errorMessage = ''

      try {
        const res = await getHotListApi(type)
        const normalized = this.normalizeHotItems(res?.data)
        this.$set(this.hotLists, type, normalized)
      } catch (error) {
        this.$set(this.hotLists, type, [])
        this.errorMessage = error.message || '接口暂时不可用，请稍后重试'
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      const keyword = this.searchText.trim()
      if (!keyword) {
        this.$message.warning('请输入你想搜索的内容')
        return
      }

      window.open(`${this.currentEngineMeta.url}${encodeURIComponent(keyword)}`, '_blank', 'noopener')
    },
    handleHotItemClick(item) {
      if (!item?.url) {
        return
      }
      window.open(item.url, '_blank', 'noopener')
    },
    formatHotValue(value) {
      const numericValue = Number(value || 0)
      if (!numericValue) {
        return '热度更新中'
      }
      if (numericValue >= 100000000) {
        return `${(numericValue / 100000000).toFixed(1)}亿`
      }
      if (numericValue >= 10000) {
        return `${(numericValue / 10000).toFixed(1)}万`
      }
      return numericValue
    },
    getTrendIcon(trend) {
      const text = String(trend || '').toLowerCase()
      if (text.includes('升') || text.includes('up')) {
        return 'fas fa-arrow-trend-up'
      }
      if (text.includes('降') || text.includes('down')) {
        return 'fas fa-arrow-trend-down'
      }
      return 'fas fa-wave-square'
    },
    getTrendClass(trend) {
      const text = String(trend || '').toLowerCase()
      if (text.includes('升') || text.includes('up')) {
        return 'is-up'
      }
      if (text.includes('降') || text.includes('down')) {
        return 'is-down'
      }
      return 'is-flat'
    },
    getTrendLabel(trend) {
      const text = String(trend || '').trim()
      if (!text) {
        return '趋势平稳'
      }
      return text
    }
  }
}
</script>

<style lang="scss" scoped>
.hot-search-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 20px 84px;
}

.hot-search-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 28px;
  margin-bottom: 28px;
}

.hero-copy,
.hero-panel,
.hot-search-board {
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(245, 247, 255, 0.88)),
    rgba(255, 255, 255, 0.7);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.hero-copy {
  padding: 34px;
}

.hero-eyebrow,
.board-eyebrow,
.search-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #6366f1;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1,
.board-header h2 {
  margin: 14px 0 14px;
  color: var(--text-primary);
  font-size: clamp(32px, 6vw, 54px);
  line-height: 1.02;
  font-weight: 800;
}

.hero-summary {
  max-width: 560px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.hero-stat {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(148, 163, 184, 0.14);

  strong {
    display: block;
    margin-bottom: 8px;
    color: #312e81;
    font-size: 28px;
    line-height: 1;
    font-weight: 800;
  }

  span {
    color: #64748b;
    font-size: 13px;
    line-height: 1.6;
  }
}

.hero-panel {
  padding: 28px;
}

.search-box {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.search-input {
  position: relative;

  input {
    width: 100%;
    height: 58px;
    padding: 0 68px 0 20px;
    border: 1px solid rgba(148, 163, 184, 0.24);
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.94);
    color: var(--text-primary);
    font-size: 15px;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;

    &:focus {
      outline: none;
      border-color: rgba(99, 102, 241, 0.42);
      box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.08);
    }
  }
}

.search-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 14px 24px rgba(99, 102, 241, 0.22);
  }
}

.engine-list,
.tab-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.engine-chip,
.tab-chip,
.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.engine-chip,
.tab-chip {
  padding: 11px 16px;
  font-size: 14px;
  font-weight: 600;
}

.engine-chip.active,
.tab-chip.active {
  border-color: transparent;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  box-shadow: 0 16px 28px rgba(99, 102, 241, 0.2);
}

.hot-search-board {
  padding: 28px;
}

.board-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  margin-bottom: 18px;

  h2 {
    margin: 10px 0 0;
    font-size: clamp(24px, 4vw, 34px);
  }
}

.refresh-btn {
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 600;

  &:hover:not(:disabled) {
    color: #4338ca;
    border-color: rgba(99, 102, 241, 0.24);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
}

.status-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding: 18px 20px;
  border: 1px dashed rgba(148, 163, 184, 0.28);
  border-radius: 20px;
  color: #64748b;

  i {
    font-size: 20px;
    color: #6366f1;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    color: var(--text-primary);
  }

  p {
    margin: 0;
    font-size: 14px;
    line-height: 1.6;
  }
}

.status-panel--error {
  border-style: solid;
  border-color: rgba(239, 68, 68, 0.18);
  background: rgba(254, 242, 242, 0.7);

  i,
  strong {
    color: #dc2626;
  }
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.hot-card {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) 28px;
  gap: 14px;
  align-items: start;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(99, 102, 241, 0.18);
    box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
  }
}

.hot-card--top {
  background:
    linear-gradient(135deg, rgba(238, 242, 255, 0.92), rgba(255, 255, 255, 0.96)),
    rgba(255, 255, 255, 0.92);
}

.hot-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: rgba(99, 102, 241, 0.1);
  color: #4338ca;
  font-size: 20px;
  font-weight: 800;

  &.rank-1 {
    background: linear-gradient(135deg, #ef4444, #f97316);
    color: #fff;
  }

  &.rank-2 {
    background: linear-gradient(135deg, #3b82f6, #6366f1);
    color: #fff;
  }

  &.rank-3 {
    background: linear-gradient(135deg, #f59e0b, #f97316);
    color: #fff;
  }
}

.hot-main {
  min-width: 0;
}

.hot-title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.hot-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.6;
  font-weight: 700;
}

.hot-tag,
.hot-type {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.1);
  color: #4338ca;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.hot-summary {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.hot-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
}

.hot-value,
.hot-trend {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.hot-value {
  color: #ef4444;
  font-weight: 700;
}

.hot-trend.is-up {
  color: #16a34a;
}

.hot-trend.is-down {
  color: #dc2626;
}

.hot-trend.is-flat {
  color: #f59e0b;
}

.hot-link {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
}

@media (max-width: 1024px) {
  .hot-search-hero,
  .hot-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hot-search-page {
    padding: 14px 12px 64px;
  }

  .hero-copy,
  .hero-panel,
  .hot-search-board {
    border-radius: 24px;
    padding: 20px 16px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }

  .board-header {
    flex-direction: column;
    align-items: stretch;
  }

  .refresh-btn {
    justify-content: center;
  }

  .hot-card {
    grid-template-columns: 44px minmax(0, 1fr);
  }

  .hot-link {
    display: none;
  }
}
</style>
