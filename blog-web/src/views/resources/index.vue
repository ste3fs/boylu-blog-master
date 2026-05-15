<template>
  <div class="resources-page">
    <section class="resources-hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">Resource Hub</span>
        <h1>资源中心</h1>
        <p class="hero-summary">
          收集好用的教程、工具和素材。没有登录也能正常打开页面，有公开资源就能直接浏览详情。
        </p>

        <div class="hero-stats">
          <article class="hero-stat">
            <strong>{{ total }}</strong>
            <span>公开资源</span>
          </article>
          <article class="hero-stat">
            <strong>{{ categoryOptions.length || 1 }}</strong>
            <span>分类筛选</span>
          </article>
          <article class="hero-stat">
            <strong>{{ resourceList.length }}</strong>
            <span>当前页条目</span>
          </article>
        </div>
      </div>

      <div class="hero-actions">
        <div class="search-card">
          <label class="search-label">搜索资源</label>
          <div class="search-bar">
            <el-input
              v-model="params.name"
              placeholder="输入资源名后回车搜索"
              clearable
              @keyup.enter.native="handleSearch"
            />
            <el-button type="primary" @click="handleSearch">
              <i class="fas fa-search"></i>
              搜索
            </el-button>
          </div>
        </div>

        <div class="hero-actions__footer">
          <el-button type="primary" icon="el-icon-upload" @click="showUploadDialog">
            上传资源
          </el-button>
          <el-button plain @click="handleReset">
            重置筛选
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="categoryOptions.length" class="category-strip">
      <button
        type="button"
        class="category-chip"
        :class="{ active: !params.category }"
        @click="selectCategory('')"
      >
        全部
      </button>
      <button
        v-for="category in categoryOptions"
        :key="category.value"
        type="button"
        class="category-chip"
        :class="{ active: params.category === category.value }"
        @click="selectCategory(category.value)"
      >
        <svg-icon v-if="category.remark" :icon-class="category.remark"></svg-icon>
        <span>{{ category.label }}</span>
      </button>
    </section>

    <section class="resources-board">
      <div class="board-header">
        <div>
          <p class="board-eyebrow">公开资源</p>
          <h2>{{ currentBoardTitle }}</h2>
        </div>
        <div class="board-meta">
          <span v-if="dictLoadFailed" class="meta-tip">
            分类接口未开放，当前已自动切到公开资源模式
          </span>
        </div>
      </div>

      <div v-if="errorMessage" class="status-panel status-panel--error">
        <i class="fas fa-circle-exclamation"></i>
        <div>
          <strong>资源加载失败</strong>
          <p>{{ errorMessage }}</p>
        </div>
      </div>

      <div v-else-if="loading" class="status-panel">
        <i class="fas fa-spinner fa-spin"></i>
        <div>
          <strong>资源加载中</strong>
          <p>正在整理最新公开资源</p>
        </div>
      </div>

      <div v-else-if="!resourceList.length" class="empty-panel">
        <div class="empty-illustration">
          <i class="fas fa-box-open"></i>
        </div>
        <h3>这里暂时还没有公开资源</h3>
        <p>资源页已经可以正常访问了，只是当前接口返回的数据为空。后续上传并审核通过后，这里会直接显示出来。</p>
        <el-button type="primary" @click="showUploadDialog">上传第一个资源</el-button>
      </div>

      <div v-else class="resource-grid">
        <article
          v-for="resource in resourceList"
          :key="resource.id"
          class="resource-card"
          @click="handleResourceClick(resource)"
        >
          <div class="resource-card__icon">
            <img
              v-if="resource.cover"
              :src="resolveResourceCover(resource.cover)"
              class="resource-cover-thumb"
              :alt="resource.name"
            >
            <svg-icon v-else :icon-class="resolveCategoryIcon(resource.category)"></svg-icon>
          </div>

          <div class="resource-card__content">
            <div class="resource-card__top">
              <h3 class="resource-card__title" :title="resource.name">{{ resource.name }}</h3>
              <el-tag size="mini" :type="Number(resource.isFree) === 1 ? 'success' : 'warning'" effect="plain">
                {{ Number(resource.isFree) === 1 ? '免费' : '付费' }}
              </el-tag>
            </div>

            <p class="resource-card__description">
              {{ resource.description || '暂未填写资源描述，点开详情可继续获取下载方式。' }}
            </p>

            <div class="resource-card__footer">
              <div class="resource-owner">
                <img :src="resolveAvatar(resource.avatar)" class="uploader-avatar" alt="avatar">
                <span>{{ resource.nickname || '匿名分享者' }}</span>
              </div>

              <div class="resource-stats">
                <span><i class="el-icon-time"></i>{{ formatRelative(resource.createTime) }}</span>
                <span><i class="el-icon-download"></i>{{ resource.downloads || 0 }}</span>
                <span><i class="el-icon-view"></i>{{ resource.views || 0 }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > params.pageSize" class="pagination-box">
        <el-pagination
          :current-page.sync="params.pageNum"
          :page-size="params.pageSize"
          layout="prev, pager, next"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <Add :visible.sync="uploadDialogVisible" :categories="uploadCategories" @success="handleUploadSuccess" />

    <el-dialog
      title="资源详情"
      :visible.sync="detailDialogVisible"
      :width="isMobile ? '94%' : '640px'"
      top="6vh"
      class="resource-detail-dialog"
      :close-on-click-modal="false"
    >
      <div v-if="currentResource" class="resource-detail">
        <div class="detail-header">
          <div class="detail-header__icon">
            <img
              v-if="currentResource.cover"
              :src="resolveResourceCover(currentResource.cover)"
              class="resource-detail-cover"
              :alt="currentResource.name"
            >
            <svg-icon v-else :icon-class="resolveCategoryIcon(currentResource.category)"></svg-icon>
          </div>
          <div class="detail-header__copy">
            <h3>{{ currentResource.name }}</h3>
            <p>{{ currentResource.description || '当前资源暂未填写描述。' }}</p>
          </div>
          <el-tag size="small" :type="Number(currentResource.isFree) === 1 ? 'success' : 'warning'" effect="plain">
            {{ Number(currentResource.isFree) === 1 ? '免费' : '付费' }}
          </el-tag>
        </div>

        <div class="detail-meta">
          <article class="detail-meta__item">
            <span>上传者</span>
            <strong>{{ currentResource.nickname || '匿名分享者' }}</strong>
          </article>
          <article class="detail-meta__item">
            <span>上传时间</span>
            <strong>{{ formatDate(currentResource.createTime) || '未知' }}</strong>
          </article>
          <article class="detail-meta__item">
            <span>下载次数</span>
            <strong>{{ currentResource.downloads || 0 }}</strong>
          </article>
        </div>

        <div v-if="!showVerifyCode && !currentResource.panPath" class="get-link-section">
          <el-button type="primary" :loading="downloading" @click="handleGetLink">
            {{ isCurrentResourceFree ? '登录后获取下载链接' : '获取下载方式' }}
          </el-button>
          <p v-if="isCurrentResourceFree">免费资源登录后即可下载，不需要扫码。</p>
          <p v-else>付费资源需要完成验证码校验后获取下载方式。</p>
        </div>

        <div v-if="!isCurrentResourceFree && showVerifyCode && !currentResource.panPath" class="verify-section">
          <div class="qr-code">
            <img v-lazy="scanPlaceholderUrl" :key="scanPlaceholderUrl" alt="扫码占位图">
            <p class="scan-tip">
              如未配置公众号二维码，请联系站长微信 <span class="code-tip">a3453619783</span>
            </p>
          </div>
          <el-form :model="verifyForm" class="verify-form">
            <el-form-item>
              <el-input v-model="verifyForm.code" placeholder="请输入验证码" maxlength="6">
                <template slot="append">
                  <el-button type="primary" :loading="verifying" @click="handleVerify">
                    验证
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="currentResource.panPath" class="download-link-section">
          <div class="link-item">
            <span class="label">网盘链接</span>
            <el-input :value="currentResource.panPath" readonly>
              <el-button slot="append" @click="copyText(currentResource.panPath)">
                复制
              </el-button>
            </el-input>
          </div>
          <div v-if="currentResource.panCode" class="link-item">
            <span class="label">提取码</span>
            <el-input :value="currentResource.panCode" readonly>
              <el-button slot="append" @click="copyText(currentResource.panCode)">
                复制
              </el-button>
            </el-input>
          </div>
        </div>
      </div>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleCloseDetail">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Tag from 'element-ui/lib/tag'
import 'element-ui/lib/theme-chalk/tag.css'
import Add from './components/add.vue'
import { getDictDataApi } from '@/api/dict'
import { downloadResourceApi, getResourcesApi, verifyCodeApi } from '@/api/resources'
import { copyText as copyPlainText } from '@/utils/contact'
import { getToken } from '@/utils/cookie'
import { resolveImageUrl } from '@/utils/image'
import { formatDate, formatDateTime } from '@/utils/time'

const CATEGORY_ICON_MAP = {
  code: 'code',
  source: 'code',
  blog: 'book',
  book: 'book',
  design: 'picture',
  image: 'picture',
  tool: 'tool',
  software: 'tool',
  note: 'document',
  tutorial: 'education'
}

export default {
  name: 'ResourcesView',
  components: {
    ElTag: Tag,
    Add
  },
  data() {
    return {
      loading: false,
      dictLoadFailed: false,
      categoryOptions: [],
      resourceList: [],
      total: 0,
      uploadDialogVisible: false,
      params: {
        pageNum: 1,
        pageSize: 8,
        category: '',
        name: ''
      },
      detailDialogVisible: false,
      currentResource: null,
      showVerifyCode: false,
      verifying: false,
      downloading: false,
      errorMessage: '',
      scanPlaceholderUrl: new URL('../../assets/scan-placeholder.svg', import.meta.url).href,
      verifyForm: {
        code: ''
      },
      isMobile: typeof window !== 'undefined' ? window.innerWidth <= 768 : false
    }
  },
  computed: {
    currentBoardTitle() {
      if (!this.params.category) {
        return '全部公开资源'
      }

      const matched = this.categoryOptions.find(item => item.value === this.params.category)
      return matched?.label || '分类资源'
    },
    uploadCategories() {
      return this.categoryOptions.length
        ? this.categoryOptions
        : [{ id: 'fallback', label: '默认分类', value: 'default', remark: 'folder-open' }]
    },
    isCurrentResourceFree() {
      return Number(this.currentResource?.isFree) === 1
    }
  },
  created() {
    this.bootstrap()
  },
  mounted() {
    window.addEventListener('resize', this.syncViewport)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
  },
  methods: {
    async bootstrap() {
      await this.loadCategories()
      await this.fetchResources()
    },
    syncViewport() {
      this.isMobile = window.innerWidth <= 768
    },
    async loadCategories() {
      try {
        const res = await getDictDataApi('sys_resource_category')
        this.categoryOptions = Array.isArray(res?.data) ? res.data : []
      } catch (error) {
        this.categoryOptions = []
        this.dictLoadFailed = true
      }
    },
    async fetchResources() {
      this.loading = true
      this.errorMessage = ''

      try {
        const res = await getResourcesApi(this.params)
        this.resourceList = Array.isArray(res?.data?.records) ? res.data.records : []
        this.total = Number(res?.data?.total || 0)
      } catch (error) {
        this.resourceList = []
        this.total = 0
        this.errorMessage = error.message || '资源接口暂时不可用，请稍后重试'
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.params.pageNum = 1
      this.fetchResources()
    },
    handleReset() {
      this.params.pageNum = 1
      this.params.name = ''
      this.params.category = ''
      this.fetchResources()
    },
    selectCategory(value) {
      this.params.category = value
      this.params.pageNum = 1
      this.fetchResources()
    },
    handlePageChange(page) {
      this.params.pageNum = page
      this.fetchResources()
    },
    handleUploadSuccess() {
      this.params.pageNum = 1
      this.fetchResources()
    },
    handleResourceClick(resource) {
      this.currentResource = { ...resource }
      this.detailDialogVisible = true
      this.showVerifyCode = false
      this.verifyForm.code = ''
    },
    showUploadDialog() {
      if (!this.$store.state.userInfo) {
        this.$message.warning('请先登录')
        return
      }
      this.uploadDialogVisible = true
    },
    async handleGetLink() {
      if (!getToken()) {
        this.$message.warning('请先登录后下载')
        this.$router.push({
          path: '/login',
          query: {
            redirect: this.$route.fullPath || '/resources'
          }
        }).catch(() => {})
        return
      }

      if (!this.isCurrentResourceFree) {
        this.showVerifyCode = true
        return
      }

      this.downloading = true
      try {
        const res = await downloadResourceApi(this.currentResource.id)
        this.currentResource = {
          ...this.currentResource,
          downloads: res.data.downloads,
          panPath: res.data.panPath,
          panCode: res.data.panCode
        }
        this.resourceList = this.resourceList.map(item => (
          item.id === this.currentResource.id
            ? { ...item, downloads: this.currentResource.downloads }
            : item
        ))
      } catch (error) {
        this.$message.error(error.message || '获取下载链接失败')
      } finally {
        this.downloading = false
      }
    },
    async handleVerify() {
      if (!this.verifyForm.code) {
        this.$message.warning('请输入验证码')
        return
      }

      this.verifying = true
      try {
        const res = await verifyCodeApi({
          id: this.currentResource.id,
          code: this.verifyForm.code
        })
        this.currentResource = {
          ...this.currentResource,
          panPath: res.data.panPath,
          panCode: res.data.panCode
        }
      } catch (error) {
        this.$message.error(error.message || '验证码校验失败')
      } finally {
        this.verifying = false
      }
    },
    copyText(text) {
      copyPlainText(text).then(copied => {
        if (copied) {
          this.$message.success('复制成功')
          return
        }
        this.$message.error('复制失败')
      }).catch(() => {
        this.$message.error('复制失败')
      })
    },
    handleCloseDetail() {
      this.detailDialogVisible = false
      this.showVerifyCode = false
      this.verifyForm.code = ''
      this.currentResource = null
    },
    resolveCategoryIcon(category) {
      const key = String(category || '').toLowerCase()
      return CATEGORY_ICON_MAP[key] || 'folder-open'
    },
    resolveResourceCover(url) {
      return resolveImageUrl(url, this.$store.state.defaultImage)
    },
    resolveAvatar(url) {
      return resolveImageUrl(url, this.$store.state.defaultImage)
    },
    formatDate(value) {
      return formatDate(value)
    },
    formatRelative(value) {
      const dateText = formatDateTime(value)
      return dateText || '刚刚'
    }
  }
}
</script>

<style scoped lang="scss">
.resources-page {
  max-width: 1240px;
  margin: 0 auto;
  padding: 24px 20px 84px;
}

.resources-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 24px;
  margin-bottom: 22px;
}

.hero-copy,
.hero-actions,
.resources-board {
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(245, 248, 255, 0.9)),
    rgba(255, 255, 255, 0.86);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.hero-copy {
  padding: 34px;
}

.hero-eyebrow,
.board-eyebrow,
.search-label {
  display: inline-flex;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1,
.board-header h2 {
  margin: 14px 0 14px;
  color: var(--text-primary);
  font-size: clamp(32px, 6vw, 52px);
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
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(148, 163, 184, 0.14);

  strong {
    display: block;
    margin-bottom: 8px;
    color: #1d4ed8;
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

.hero-actions {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
}

.search-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.search-bar {
  display: flex;
  gap: 10px;

  :deep(.el-input__inner) {
    height: 52px;
    border-radius: 16px;
  }

  :deep(.el-button) {
    min-width: 112px;
    border-radius: 16px;
  }
}

.hero-actions__footer {
  display: flex;
  gap: 12px;

  :deep(.el-button) {
    flex: 1;
    border-radius: 16px;
  }
}

.category-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    border-color: transparent;
    background: linear-gradient(135deg, #2563eb, #4f46e5);
    color: #fff;
    box-shadow: 0 16px 28px rgba(37, 99, 235, 0.18);
  }
}

.resources-board {
  padding: 28px;
}

.board-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 18px;

  h2 {
    margin: 10px 0 0;
    font-size: clamp(24px, 4vw, 32px);
  }
}

.meta-tip {
  display: inline-flex;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 13px;
  line-height: 1.5;
}

.status-panel,
.empty-panel {
  padding: 22px;
  border-radius: 22px;
  border: 1px dashed rgba(148, 163, 184, 0.24);
}

.status-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  color: #64748b;

  i {
    font-size: 20px;
    color: #2563eb;
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

.empty-panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;

  h3 {
    margin: 0;
    color: var(--text-primary);
    font-size: 24px;
    font-weight: 800;
  }

  p {
    max-width: 540px;
    margin: 0;
    color: var(--text-secondary);
    line-height: 1.8;
  }

  :deep(.el-button) {
    margin-top: 8px;
    border-radius: 16px;
  }
}

.empty-illustration {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 24px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 28px;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.resource-card {
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(37, 99, 235, 0.18);
    box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
  }
}

.resource-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(79, 70, 229, 0.14));
  color: #2563eb;
  font-size: 28px;
}

.resource-cover-thumb,
.resource-detail-cover {
  width: 100%;
  height: 100%;
  border-radius: inherit;
  object-fit: cover;
  display: block;
}

.resource-card__content {
  min-width: 0;
}

.resource-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.resource-card__title {
  margin: 0;
  color: var(--text-primary);
  font-size: 17px;
  line-height: 1.5;
  font-weight: 700;
}

.resource-card__description {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.75;
}

.resource-card__footer {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  margin-top: 16px;
}

.resource-owner,
.resource-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.resource-stats {
  flex-wrap: wrap;
  justify-content: flex-end;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.uploader-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}

.pagination-box {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.resource-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-header {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
}

.detail-header__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.12), rgba(79, 70, 229, 0.14));
  color: #2563eb;
  font-size: 28px;
}

.detail-header__copy {
  min-width: 0;

  h3 {
    margin: 0 0 8px;
    color: var(--text-primary);
    font-size: 24px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: var(--text-secondary);
    line-height: 1.75;
  }
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-meta__item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);

  span {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 13px;
  }

  strong {
    color: var(--text-primary);
    font-size: 15px;
    line-height: 1.5;
  }
}

.get-link-section,
.verify-section,
.download-link-section {
  padding: 20px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.get-link-section {
  text-align: center;

  p {
    margin: 12px 0 0;
    color: #64748b;
    font-size: 13px;
  }
}

.verify-section {
  .qr-code {
    text-align: center;
    margin-bottom: 18px;

    img {
      width: 200px;
      height: 200px;
      border-radius: 20px;
      object-fit: cover;
      margin-bottom: 12px;
    }
  }

  .scan-tip {
    margin: 0;
    color: #64748b;
    line-height: 1.7;
  }

  .code-tip {
    color: #2563eb;
    font-weight: 700;
  }

  .verify-form {
    max-width: 320px;
    margin: 0 auto;
  }
}

.download-link-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.label {
  display: inline-block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .resources-hero,
  .resource-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .resources-page {
    padding: 14px 12px 64px;
  }

  .hero-copy,
  .hero-actions,
  .resources-board {
    border-radius: 24px;
    padding: 20px 16px;
  }

  .hero-stats,
  .detail-meta {
    grid-template-columns: 1fr;
  }

  .search-bar,
  .hero-actions__footer,
  .detail-header,
  .resource-card,
  .resource-card__footer {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }

  .board-header {
    flex-direction: column;
    align-items: stretch;
  }

  .resource-owner,
  .resource-stats {
    justify-content: flex-start;
  }

  .detail-header__icon {
    margin-bottom: 4px;
  }
}
</style>
