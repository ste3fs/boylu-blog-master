<template>
  <div class="article-list-component" v-loading="loading">
    <transition-group name="post-list" tag="div" class="posts-list">
      <article v-for="(post, index) in visibleArticles" :key="post.id" class="post-item">
        <div class="post-content">
          <div class="post-main">
            <div class="post-text">
              <h3>
                <span v-if="post.isStick" class="stick-tag">
                  <i class="fas fa-thumbtack"></i>
                  置顶
                </span>
                <span class="post-title underline" @click="$emit('article-click', post.id)">{{ post.title }}</span>
              </h3>
              <p class="post-excerpt">{{ post.excerpt || post.summary }}</p>
            </div>
            <div class="post-image" @click="$emit('article-click', post.id)">
              <SmartImage
                class="post-cover"
                :image="resolvePostImage(post)"
                :priority="isPriorityImage(index)"
                sizes="(max-width: 576px) 92vw, (max-width: 1024px) 44vw, 220px"
              />
            </div>
          </div>

          <div class="post-footer">
            <div class="footer-left">
              <div class="author-info">
                <img
                  class="author-avatar"
                  :src="resolveAuthorAvatar(post)"
                  :alt="post.nickname || 'boylu'"
                  @error="handleAuthorAvatarError"
                >
                <span class="author-name">{{ post.nickname || 'boylu' }}</span>
              </div>
              <div class="post-date">
                <i class="far fa-calendar"></i>
                {{ formatPublishTime(post.createdAt || post.createTime) }}
              </div>
              <div class="post-view">
                <i class="far fa-eye"></i>
                {{ resolvePostViews(post) }}
              </div>
            </div>
            <div class="footer-right">
              <span class="category-tag">{{ post.category || post.categoryName }}</span>
              <span class="read-time">
                <i class="far fa-clock"></i>
                {{ getReadTime(post) }}分钟阅读
              </span>
            </div>
          </div>
        </div>
      </article>
    </transition-group>
    <div v-if="hasMore" ref="loadMoreSentinel" class="load-more-sentinel" aria-hidden="true"></div>

    <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />

    <div class="pagination-box">
      <el-pagination 
        background 
        v-if="articles.length" 
        @current-change="$emit('page-change', $event)"
        :current-page="params.pageNum" 
        :page-size="params.pageSize" 
        layout="prev, pager, next" 
        :total="total">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { formatPublishTime } from '@/utils/time'
import { estimateReadMinutes } from '@/utils/readTime'
import { resolveImageUrl } from '@/utils/image'
import SmartImage from '@/components/common/SmartImage.vue'

export default {
  name: 'ArticleList',
  components: {
    SmartImage
  },
  props: {
    articles: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    total: {
      type: Number,
      default: 0
    },
    params: {
      type: Object,
      default: {
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  data() {
    return {
      displayedCount: 0,
      firstScreenLimit: 4,
      loadBatchSize: 3,
      loadMoreObserver: null
    }
  },
  computed: {
    visibleArticles() {
      return this.articles.slice(0, this.displayedCount)
    },
    hasMore() {
      return this.displayedCount < this.articles.length
    }
  },
  watch: {
    articles: {
      immediate: true,
      handler() {
        this.resetVisibleList()
      }
    },
    'params.pageNum'() {
      this.resetVisibleList()
    }
  },
  methods: {
    resetVisibleList() {
      const firstPage = Number((this.params && this.params.pageNum) || 1) === 1
      const initialLimit = firstPage ? this.firstScreenLimit : this.firstScreenLimit + this.loadBatchSize
      this.displayedCount = Math.min(this.articles.length, initialLimit)
      this.$nextTick(() => {
        this.setupLoadMoreObserver()
      })
    },
    loadMoreArticles() {
      if (!this.hasMore) {
        return
      }
      this.displayedCount = Math.min(this.articles.length, this.displayedCount + this.loadBatchSize)
      if (!this.hasMore && this.loadMoreObserver) {
        this.loadMoreObserver.disconnect()
        this.loadMoreObserver = null
      }
    },
    setupLoadMoreObserver() {
      if (this.loadMoreObserver) {
        this.loadMoreObserver.disconnect()
        this.loadMoreObserver = null
      }
      if (!this.hasMore || !this.$refs.loadMoreSentinel || typeof IntersectionObserver === 'undefined') {
        return
      }
      this.loadMoreObserver = new IntersectionObserver(entries => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            this.loadMoreArticles()
          }
        })
      }, {
        rootMargin: '320px 0px',
        threshold: 0.01
      })
      this.loadMoreObserver.observe(this.$refs.loadMoreSentinel)
    },
    formatPublishTime(time) {
      return formatPublishTime(time)
    },
    getReadTime(post) {
      const backendReadingTime = Number(post && post.readingTime)
      if (Number.isFinite(backendReadingTime) && backendReadingTime > 0) {
        return backendReadingTime
      }
      const source = post.contentMd || post.content || post.summary || post.excerpt || ''
      return estimateReadMinutes(source, {
        summaryOnly: !post.contentMd && !post.content && !!(post.summary || post.excerpt)
      })
    },
    resolveImage(url) {
      return resolveImageUrl(url, this.$store.state.defaultImage)
    },
    resolveAuthorFallback() {
      const siteInfo = this.$store.state.webSiteInfo || {}
      return resolveImageUrl(siteInfo.authorAvatar || siteInfo.touristAvatar || '/boylu-avatar.jpg', '/boylu-avatar.jpg')
    },
    resolveAuthorAvatar(post) {
      return resolveImageUrl(post && post.avatar, this.resolveAuthorFallback())
    },
    handleAuthorAvatarError(event) {
      if (!event || !event.target) {
        return
      }
      event.target.onerror = null
      event.target.src = this.resolveAuthorFallback()
    },
    resolvePostImage(post) {
      if (post.coverImage) {
        const coverFallback = this.resolveImage(post.coverImage.fallback || post.coverImage.source)
        const legacyCover = this.resolveImage(post.cover)
        const fallback = this.isDefaultCover(coverFallback) && legacyCover
          ? legacyCover
          : (coverFallback || legacyCover)
        return {
          ...post.coverImage,
          alt: post.coverImage.alt || post.title,
          fallback,
          styleSource: fallback
        }
      }
      return {
        alt: post.title,
        width: 1600,
        height: 900,
        dominantColor: '#eef4ff',
        fallback: this.resolveImage(post.cover)
      }
    },
    isDefaultCover(url) {
      const defaultImage = this.resolveImage(this.$store.state.defaultImage)
      return !!url && !!defaultImage && url === defaultImage
    },
    resolvePostViews(post) {
      return post.views || post.quantity || 0
    },
    isPriorityImage(index) {
      return Number((this.params && this.params.pageNum) || 1) === 1 && index < 2
    }
  },
  beforeDestroy() {
    if (this.loadMoreObserver) {
      this.loadMoreObserver.disconnect()
      this.loadMoreObserver = null
    }
  }
}
</script>

<style lang="scss" scoped>
.posts-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.post-item {
  @include card;
  background: var(--card-bg);
  padding: $spacing-lg;
  border: 1px solid rgba(var(--border-color-rgb), 0.08);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  width: 100%;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(64, 158, 255, 0.18);
    box-shadow: 0 16px 40px rgba(15, 23, 42, 0.10);
  }
}

.post-main {
  display: grid;
  grid-template-columns: 1fr 200px;
  gap: $spacing-lg;
  margin-bottom: $spacing-md;
  align-items: stretch;

  .post-text {
    h3 {
      font-size: 1.22em;
      margin-bottom: $spacing-md;
      line-height: 1.45;
      color: var(--text-primary);
      cursor: pointer;
      .stick-tag {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 0.6em;
        background: linear-gradient(135deg, $secondary, darken($secondary, 10%));
        color: white;
        padding: 3px 8px;
        border-radius: 4px;
        margin-right: $spacing-sm;
        
        i {
          transform: rotate(45deg);
        }
      }
    }

    .post-title {
      &:hover {
        color: $primary;
      }
    }

    .post-excerpt {
      color: var(--text-secondary);
      line-height: 1.7;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .post-image {
    position: relative;
    aspect-ratio: 16 / 9;
    height: auto;
    min-width: 0;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    background: var(--hover-bg);

    .post-cover {
      height: 100%;
      min-height: 100%;
      transform: translateZ(0);
    }

    &:hover :deep(.smart-image__img) {
      transform: scale(1.035);
    }
  }
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: $spacing-md;
  border-top: 1px solid var(--border-color);
  color: var(--text-secondary);
  .fa-calendar{
    color: $primary;
  }
  .fa-eye{
    color: #67c23a;
  }
  .fa-clock{
    color: #2fa9e1;
  }
  .footer-left {
    display: flex;
    align-items: center;
    gap: $spacing-lg;

      .author-info {
        display: flex;
        align-items: center;
        gap: $spacing-sm;

      .author-avatar {
        width: 24px;
        height: 24px;
        flex: 0 0 24px;
        border-radius: 50%;
        object-fit: cover;
        background: var(--hover-bg);
      }

      .author-name {
        font-weight: 500;
        color: $primary;
      }
    }

    .post-date {
      color: var(--text-secondary);
      font-size: 0.9em;
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }
  }

  .footer-right {
    display: flex;
    align-items: center;
    gap: $spacing-lg;

    .category-tag {
      padding: 4px 12px;
      background: var(--hover-bg);
      border-radius: 20px;
      font-size: 0.9em;
      color: var(--text-secondary);
    }

    .read-time {
      color: var(--text-secondary);
      font-size: 0.9em;
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }
  }
}

@include responsive(lg) {
  .post-main {
    grid-template-columns: 1fr;
    
    .post-image {
      height: auto;
      order: -1;
    }
  }

  .post-footer {
    flex-direction: column;
    align-items: stretch;
    gap: $spacing-md;

    .footer-left,
    .footer-right {
      width: 100%;
      justify-content: space-between;
      flex-wrap: wrap;
      row-gap: $spacing-sm;
    }
  }
}

@include responsive(sm) {
  .post-item {
    padding: $spacing-md;
    border-radius: $border-radius-md;
  }

  .post-main {
    gap: $spacing-md;

    .post-image {
      height: auto;
    }
  }

  .post-footer {
    .footer-left,
    .footer-right {
      width: 100%;
      gap: $spacing-sm;
    }
  }
}

// 保持动画效果不变
.post-list-enter-active {
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  transition-delay: calc(0.1s * var(--index));
}

.post-list-leave-active {
  transition: all 0.6s ease;
}

.post-list-enter,
.post-list-leave-to {
  opacity: 0;
  transform: translateY(30px);
}

.load-more-sentinel {
  width: 100%;
  height: 1px;
}
</style>
