<template>
  <div class="archives-page">
    <div class="content-layout">
      <main class="main-content">
        <section class="archive-panel">
          <header class="archive-header">
            <div>
              <span class="archive-eyebrow">ARCHIVE TREE</span>
              <h1 class="archive-title">文章归档</h1>
              <p class="archive-description">像文件树一样按年、月、日收纳文章，展开状态会自动记住。</p>
            </div>
            <div class="archive-meta" v-if="archiveSummary.totalPosts">
              <span>{{ archiveSummary.totalPosts }} 篇文章</span>
              <span>{{ archiveSummary.totalYears }} 个年份</span>
            </div>
          </header>

          <div v-if="archiveTree.length" class="tree-shell">
            <div class="tree-title">
              <i class="fas fa-folder-open"></i>
              <span>Archive</span>
            </div>

            <div class="tree-root">
              <section
                v-for="yearGroup in archiveTree"
                :key="yearGroup.year"
                class="tree-node level-0"
                v-animate-on-scroll
              >
                <button
                  type="button"
                  class="tree-row folder-row year-row"
                  @click="toggleYear(yearGroup.year)"
                >
                  <span class="tree-toggle" :class="{ 'is-open': !collapsedYears[yearGroup.year] }">
                    <i class="fas fa-chevron-right"></i>
                  </span>
                  <span class="tree-icon folder-icon">
                    <i :class="collapsedYears[yearGroup.year] ? 'fas fa-folder' : 'fas fa-folder-open'"></i>
                  </span>
                  <span class="tree-content">
                    <span class="tree-label">{{ yearGroup.year }}</span>
                    <span class="tree-desc">{{ yearGroup.totalPosts }} 篇文章 · {{ yearGroup.monthCount }} 个月</span>
                  </span>
                </button>

                <transition
                  name="expand"
                  @enter="startTransition"
                  @leave="endTransition"
                  @after-enter="resetTransitionHeight"
                  @after-leave="resetTransitionHeight"
                >
                  <div v-show="!collapsedYears[yearGroup.year]" class="tree-children">
                    <section
                      v-for="monthGroup in yearGroup.months"
                      :key="monthGroup.key"
                      class="tree-node level-1"
                    >
                      <button
                        type="button"
                        class="tree-row folder-row"
                        @click="toggleMonth(monthGroup.key)"
                      >
                        <span class="tree-toggle" :class="{ 'is-open': !collapsedMonths[monthGroup.key] }">
                          <i class="fas fa-chevron-right"></i>
                        </span>
                        <span class="tree-icon folder-icon">
                          <i :class="collapsedMonths[monthGroup.key] ? 'fas fa-folder' : 'fas fa-folder-open'"></i>
                        </span>
                        <span class="tree-content">
                          <span class="tree-label">{{ monthGroup.label }}</span>
                          <span class="tree-desc">{{ monthGroup.totalPosts }} 篇文章 · {{ monthGroup.dayCount }} 天</span>
                        </span>
                      </button>

                      <transition
                        name="expand"
                        @enter="startTransition"
                        @leave="endTransition"
                        @after-enter="resetTransitionHeight"
                        @after-leave="resetTransitionHeight"
                      >
                        <div v-show="!collapsedMonths[monthGroup.key]" class="tree-children">
                          <section
                            v-for="dayGroup in monthGroup.days"
                            :key="dayGroup.key"
                            class="tree-node level-2"
                          >
                            <button
                              type="button"
                              class="tree-row folder-row day-row"
                              @click="toggleDay(dayGroup.key)"
                            >
                              <span class="tree-toggle" :class="{ 'is-open': !collapsedDays[dayGroup.key] }">
                                <i class="fas fa-chevron-right"></i>
                              </span>
                              <span class="tree-icon calendar-icon">
                                <i :class="collapsedDays[dayGroup.key] ? 'far fa-calendar' : 'far fa-calendar-check'"></i>
                              </span>
                              <span class="tree-content">
                                <span class="tree-label">{{ dayGroup.fullLabel }}</span>
                                <span class="tree-desc">{{ dayGroup.weekday }} · {{ dayGroup.posts.length }} 篇文章</span>
                              </span>
                            </button>

                            <transition
                              name="expand"
                              @enter="startTransition"
                              @leave="endTransition"
                              @after-enter="resetTransitionHeight"
                              @after-leave="resetTransitionHeight"
                            >
                              <div v-show="!collapsedDays[dayGroup.key]" class="tree-children file-children">
                                <article
                                  v-for="post in dayGroup.posts"
                                  :key="post.id"
                                  class="tree-row file-row"
                                  @click="goToPost(post.id)"
                                >
                                  <span class="tree-toggle ghost-toggle"></span>
                                  <span class="tree-icon file-icon">
                                    <i class="far fa-file-alt"></i>
                                  </span>
                                  <span class="tree-content">
                                    <span class="tree-label">{{ post.title }}</span>
                                    <span class="tree-desc">发布时间 {{ post.displayTime }} · {{ post.fullDateLabel }}</span>
                                  </span>
                                  <span class="tree-arrow">
                                    <i class="fas fa-angle-right"></i>
                                  </span>
                                </article>
                              </div>
                            </transition>
                          </section>
                        </div>
                      </transition>
                    </section>
                  </div>
                </transition>
              </section>
            </div>
          </div>

          <div v-else class="empty-state">
            暂无归档文章
          </div>
        </section>
      </main>
      <Sidebar />
    </div>
  </div>
</template>

<script>
import Sidebar from '@/components/Sidebar/index.vue'
import { getArchivesApi } from '@/api/article'

const ARCHIVE_STORAGE_KEY = 'boylu-archive-collapse-state'

export default {
  name: 'Archives',
  components: {
    Sidebar
  },
  data() {
    return {
      archiveTree: [],
      collapsedYears: {},
      collapsedMonths: {},
      collapsedDays: {}
    }
  },
  computed: {
    archiveSummary() {
      return {
        totalYears: this.archiveTree.length,
        totalPosts: this.archiveTree.reduce((sum, yearGroup) => sum + yearGroup.totalPosts, 0)
      }
    }
  },
  methods: {
    parseArchiveDate(value) {
      if (!value) {
        return new Date()
      }

      const [datePart = '', timePart = '00:00:00'] = String(value).split(' ')
      const [year, month, day] = datePart.split('-').map(item => Number(item) || 0)
      const [hour, minute, second] = timePart.split(':').map(item => Number(item) || 0)

      return new Date(year, Math.max(month - 1, 0), day || 1, hour, minute, second)
    },
    getWeekdayLabel(date) {
      const weekdayMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return weekdayMap[date.getDay()]
    },
    formatPostTime(date) {
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      return `${hours}:${minutes}`
    },
    formatMonthLabel(monthNumber) {
      return `${monthNumber}月`
    },
    formatDayLabel(monthNumber, dayNumber) {
      return `${monthNumber}月${dayNumber}日`
    },
    formatFullDateLabel(year, paddedMonth, paddedDay) {
      return `${year}年${paddedMonth}月${paddedDay}日`
    },
    buildArchiveTree(rawArchives) {
      return (rawArchives || []).map(yearItem => {
        const posts = Array.isArray(yearItem.posts) ? yearItem.posts : []
        const monthMap = new Map()

        posts.forEach(post => {
          const parsedDate = this.parseArchiveDate(post.createTime)
          const monthNumber = parsedDate.getMonth() + 1
          const dayNumber = parsedDate.getDate()
          const paddedMonth = String(monthNumber).padStart(2, '0')
          const paddedDay = String(dayNumber).padStart(2, '0')
          const monthKey = `${yearItem.year}-${paddedMonth}`
          const dayKey = `${monthKey}-${paddedDay}`

          if (!monthMap.has(monthKey)) {
            monthMap.set(monthKey, {
              key: monthKey,
              monthNumber: paddedMonth,
              label: this.formatMonthLabel(monthNumber),
              totalPosts: 0,
              days: [],
              dayMap: new Map()
            })
          }

          const monthGroup = monthMap.get(monthKey)
          monthGroup.totalPosts += 1

          if (!monthGroup.dayMap.has(dayKey)) {
            const dayGroup = {
              key: dayKey,
              fullLabel: this.formatDayLabel(monthNumber, dayNumber),
              weekday: this.getWeekdayLabel(parsedDate),
              posts: []
            }
            monthGroup.dayMap.set(dayKey, dayGroup)
            monthGroup.days.push(dayGroup)
          }

          monthGroup.dayMap.get(dayKey).posts.push({
            ...post,
            displayTime: this.formatPostTime(parsedDate),
            fullDateLabel: this.formatFullDateLabel(yearItem.year, paddedMonth, paddedDay)
          })
        })

        const months = Array.from(monthMap.values()).map(monthGroup => ({
          key: monthGroup.key,
          monthNumber: monthGroup.monthNumber,
          label: monthGroup.label,
          totalPosts: monthGroup.totalPosts,
          dayCount: monthGroup.days.length,
          days: monthGroup.days
        }))

        return {
          year: yearItem.year,
          totalPosts: posts.length,
          monthCount: months.length,
          months
        }
      })
    },
    getStoredCollapseState() {
      if (typeof window === 'undefined') {
        return null
      }

      try {
        const raw = window.localStorage.getItem(ARCHIVE_STORAGE_KEY)
        return raw ? JSON.parse(raw) : null
      } catch (error) {
        return null
      }
    },
    saveCollapseState() {
      if (typeof window === 'undefined') {
        return
      }

      const state = {
        years: this.collapsedYears,
        months: this.collapsedMonths,
        days: this.collapsedDays
      }

      window.localStorage.setItem(ARCHIVE_STORAGE_KEY, JSON.stringify(state))
    },
    mergeCollapseState(defaultState, storedState) {
      const merged = {}
      Object.keys(defaultState).forEach(key => {
        if (storedState && Object.prototype.hasOwnProperty.call(storedState, key)) {
          merged[key] = Boolean(storedState[key])
        } else {
          merged[key] = defaultState[key]
        }
      })
      return merged
    },
    initializeCollapseState(tree) {
      const defaultYears = {}
      const defaultMonths = {}
      const defaultDays = {}
      const storedState = this.getStoredCollapseState()

      tree.forEach((yearGroup, yearIndex) => {
        const openYear = yearIndex === 0
        defaultYears[yearGroup.year] = !openYear

        yearGroup.months.forEach((monthGroup, monthIndex) => {
          const openMonth = openYear && monthIndex === 0
          defaultMonths[monthGroup.key] = !openMonth

          monthGroup.days.forEach((dayGroup, dayIndex) => {
            const openDay = openMonth && dayIndex === 0
            defaultDays[dayGroup.key] = !openDay
          })
        })
      })

      this.collapsedYears = this.mergeCollapseState(defaultYears, storedState && storedState.years)
      this.collapsedMonths = this.mergeCollapseState(defaultMonths, storedState && storedState.months)
      this.collapsedDays = this.mergeCollapseState(defaultDays, storedState && storedState.days)
      this.saveCollapseState()
    },
    toggleYear(year) {
      this.$set(this.collapsedYears, year, !this.collapsedYears[year])
      this.saveCollapseState()
    },
    toggleMonth(key) {
      this.$set(this.collapsedMonths, key, !this.collapsedMonths[key])
      this.saveCollapseState()
    },
    toggleDay(key) {
      this.$set(this.collapsedDays, key, !this.collapsedDays[key])
      this.saveCollapseState()
    },
    goToPost(id) {
      this.$router.push(`/post/${id}`)
    },
    startTransition(element) {
      element.style.height = 'auto'
      const height = element.scrollHeight
      element.style.height = '0px'
      element.offsetHeight
      element.style.height = `${height}px`
    },
    endTransition(element) {
      element.style.height = `${element.scrollHeight}px`
      element.offsetHeight
      element.style.height = '0px'
    },
    resetTransitionHeight(element) {
      element.style.height = ''
    },
    async fetchArchives() {
      const res = await getArchivesApi()
      const tree = this.buildArchiveTree(res.data)
      this.archiveTree = tree
      this.initializeCollapseState(tree)
    }
  },
  async created() {
    await this.fetchArchives()
  }
}
</script>

<style lang="scss" scoped>
.archives-page {
  max-width: 1400px;
  margin: 0 auto;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: $spacing-xl * 2;
  padding: $spacing-lg;
  min-height: calc(100vh - 80px);
  align-items: start;

  @include responsive(lg) {
    grid-template-columns: 1fr;
  }

  @include responsive(md) {
    padding: $spacing-md;
  }
}

.main-content {
  min-width: 0;
}

.archive-panel {
  padding: clamp(28px, 4vw, 48px);
  border: 1px solid rgba($primary, 0.08);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.04);
}

.archive-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 28px;
  margin-bottom: 28px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.archive-eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.16em;
  color: $primary;
  background: rgba($primary, 0.08);
}

.archive-title {
  margin: 16px 0 10px;
  font-size: clamp(30px, 4vw, 40px);
  line-height: 1.1;
  color: var(--text-primary);
}

.archive-description {
  margin: 0;
  color: var(--text-secondary);
}

.archive-meta {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;

  span {
    padding: 8px 14px;
    border-radius: 999px;
    font-size: 13px;
    color: var(--text-secondary);
    background: var(--hover-bg);
  }
}

.tree-shell {
  max-width: 720px;
  margin: 0 auto;
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: #fff;
}

.tree-title {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 8px 12px;
  margin-bottom: 8px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);

  i {
    color: $primary;
  }
}

.tree-root {
  padding: 4px 2px;
}

.tree-node,
.file-row {
  position: relative;
}

.tree-children {
  position: relative;
  margin-left: 18px;
  padding-left: 16px;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 7px;
    top: 0;
    bottom: 8px;
    border-left: 1px solid rgba(15, 23, 42, 0.1);
  }

  > .tree-node::before,
  > .file-row::before {
    content: '';
    position: absolute;
    left: -9px;
    top: 18px;
    width: 14px;
    border-top: 1px solid rgba(15, 23, 42, 0.1);
  }
}

.tree-row {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 36px;
  gap: 8px;
  padding: 7px 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  text-align: left;
  transition: background 0.18s ease, color 0.18s ease;

  &:hover {
    background: rgba(15, 23, 42, 0.045);
  }
}

.year-row {
  font-weight: 600;
}

.day-row {
  color: rgba(15, 23, 42, 0.86);
}

.tree-toggle {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 10px;
  transition: transform 0.18s ease;

  &.is-open {
    transform: rotate(90deg);
  }
}

.ghost-toggle {
  opacity: 0;
}

.tree-icon {
  width: 18px;
  height: 18px;
  flex: 0 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 15px;
}

.folder-icon {
  color: rgba($primary, 0.85);
}

.calendar-icon {
  color: rgba(56, 92, 130, 0.72);
}

.file-icon {
  color: rgba(15, 23, 42, 0.5);
}

.tree-content {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.tree-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  color: var(--text-primary);
}

.tree-desc {
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-secondary);
}

.file-row {
  padding-left: 10px;

  .tree-label {
    font-size: 14px;
    font-weight: 500;
  }

  &:hover {
    color: $primary;

    .tree-label,
    .tree-arrow {
      color: $primary;
    }
  }
}

.tree-arrow {
  width: 16px;
  text-align: center;
  color: transparent;
  transition: color 0.18s ease;
}

.empty-state {
  padding: 56px 0;
  text-align: center;
  color: var(--text-secondary);
}

.expand-enter-active,
.expand-leave-active {
  overflow: hidden;
  transition: height 0.22s ease, opacity 0.22s ease;
}

.expand-enter,
.expand-leave-to {
  opacity: 0;
}

@include responsive(md) {
  .content-layout {
    gap: $spacing-lg;
  }

  .archive-panel {
    padding: 24px 18px;
  }

  .archive-header {
    flex-direction: column;
  }

  .archive-meta {
    justify-content: flex-start;
  }

  .tree-shell {
    max-width: none;
  }
}

@include responsive(sm) {
  .archives-page {
    padding: $spacing-md;
  }

  .archive-panel {
    padding: 20px 14px;
    border-radius: 20px;
  }

  .tree-shell {
    padding: 12px;
  }

  .tree-content {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }

  .tree-desc {
    flex: auto;
  }
}
</style>
