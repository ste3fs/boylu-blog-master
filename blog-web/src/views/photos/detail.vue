<template>
  <div class="album-detail-page">
    <div v-if="isAuthenticated">
      <section class="album-hero">
        <div class="album-hero__back" @click="$router.push('/photos')">
          <i class="fas fa-arrow-left"></i>
          <span>返回相册</span>
        </div>

        <div class="album-hero__content">
          <div class="album-hero__copy">
            <span class="album-hero__eyebrow">Album Detail</span>
            <h1>{{ album.name || '未命名相册' }}</h1>
            <p class="album-hero__description">
              {{ album.description || '这本相册还没有补充更多说明，先看看里面收录的照片。' }}
            </p>

            <div class="album-hero__meta">
              <span>
                <i class="fas fa-calendar-alt"></i>
                {{ formatDate(album.createTime) || '最近更新' }}
              </span>
              <span>
                <i class="fas fa-image"></i>
                {{ photos.length }} 张照片
              </span>
              <span v-if="album.isLock === 1">
                <i class="fas fa-lock"></i>
                加密相册
              </span>
            </div>

            <div class="album-hero__actions">
              <button v-if="photos.length" type="button" class="hero-btn hero-btn--primary" @click="previewImage(0)">
                预览第一张
              </button>
              <button type="button" class="hero-btn" @click="$router.push('/photos')">
                回到相册列表
              </button>
            </div>
          </div>

          <div class="album-hero__cover">
            <SmartImage
              class="album-hero__image"
              :image="buildPhotoImage(heroCover, album.name || 'album cover', 1600, 1000)"
              priority
              sizes="(max-width: 900px) 100vw, 560px"
            />
          </div>
        </div>
      </section>

      <section class="photo-board">
        <div class="board-header">
          <div>
            <p class="board-eyebrow">Photo Collection</p>
            <h2>照片列表</h2>
          </div>
        </div>

        <div v-if="photos.length" class="photo-grid">
          <article v-for="(photo, index) in photos" :key="photo.url || index" class="photo-item">
            <button type="button" class="photo-card" @click="previewImage(index)">
              <SmartImage
                class="photo-card__image"
                :image="buildPhotoImage(resolvePhotoUrl(photo.url), photo.description || `photo-${index + 1}`, 1200, 1200)"
                :priority="index < 6"
                sizes="(max-width: 768px) 50vw, 33vw"
              />
              <div class="photo-card__overlay">
                <div class="photo-card__info">
                  <h3>{{ photo.description || `照片 ${index + 1}` }}</h3>
                  <div class="photo-card__meta">
                    <span v-if="photo.recordTime">
                      <i class="fas fa-calendar"></i>
                      {{ photo.recordTime }}
                    </span>
                    <span v-if="photo.location">
                      <i class="fas fa-location-dot"></i>
                      {{ photo.location }}
                    </span>
                  </div>
                </div>
              </div>
            </button>
          </article>
        </div>

        <div v-else class="empty-state">
          <i class="fas fa-image"></i>
          <div>
            <strong>这个相册还没有照片</strong>
            <p>当前详情页已经改成更稳的结构，等后台继续往这个相册里补图后，这里会直接展示出来。</p>
          </div>
        </div>
      </section>

      <mj-image-preview ref="imagePreview" />
    </div>

    <div v-else class="loading-container">
      <div class="loading-spinner">
        <i class="fas fa-spinner fa-spin"></i>
      </div>
    </div>

    <album-password-dialog
      ref="passwordDialog"
      @submit="handlePasswordSubmit"
      @cancel="handlePasswordCancel"
    />
  </div>
</template>

<script>
import { getAlbumPhotosApi, verifyAlbumPasswordApi, getAlbumDetailApi } from '@/api/album'
import AlbumPasswordDialog from '@/views/photos/components/password.vue'
import SmartImage from '@/components/common/SmartImage.vue'
import { resolveImageUrl } from '@/utils/image'
import { formatDate } from '@/utils/time'

export default {
  name: 'AlbumDetail',
  components: {
    AlbumPasswordDialog,
    SmartImage
  },
  data() {
    return {
      album: {
        id: this.$route.params.id,
        name: '',
        description: '',
        createTime: '',
        isLock: 0,
        cover: ''
      },
      photos: [],
      images: [],
      isAuthenticated: false
    }
  },
  computed: {
    heroCover() {
      const firstPhoto = this.photos[0]?.url
      return resolveImageUrl(firstPhoto || this.album.cover, this.$store.state.defaultImage)
    }
  },
  mounted() {
    this.checkAlbumAuth()
  },
  methods: {
    formatDate(value) {
      return formatDate(value)
    },
    resolvePhotoUrl(url) {
      return resolveImageUrl(url, this.album.cover || this.$store.state.defaultImage)
    },
    buildPhotoImage(url, alt, width, height) {
      return {
        alt,
        width,
        height,
        dominantColor: '#eef4ff',
        fallback: resolveImageUrl(url, this.$store.state.defaultImage)
      }
    },
    previewImage(index) {
      if (!this.images.length) {
        return
      }
      this.$refs.imagePreview.show(this.images, index)
    },
    async checkAlbumAuth() {
      try {
        const res = await getAlbumDetailApi(this.album.id)
        this.album = res.data
        if (this.album.isLock === 1) {
          this.$refs.passwordDialog.show()
          return
        }
        this.getAlbumPhotos()
      } catch (error) {
        this.$message.error('获取相册信息失败')
        this.$router.push('/photos')
      }
    },
    getAlbumPhotos() {
      getAlbumPhotosApi(this.album.id).then(res => {
        this.photos = Array.isArray(res.data) ? res.data : []
        this.images = this.photos.map(item => this.resolvePhotoUrl(item.url))
      }).finally(() => {
        this.isAuthenticated = true
      })
    },
    async handlePasswordSubmit(password, callback) {
      try {
        await verifyAlbumPasswordApi(this.album.id, password)
        this.getAlbumPhotos()
        callback()
      } catch (error) {
        this.$message.error(error.message || '密码错误')
        this.$refs.passwordDialog.loading = false
      }
    },
    handlePasswordCancel() {
      this.$router.push('/photos')
    }
  }
}
</script>

<style lang="scss" scoped>
.album-detail-page {
  padding: 24px 20px 84px;
}

.album-hero,
.photo-board {
  max-width: 1240px;
  margin: 0 auto 24px;
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 30px;
  background:
    radial-gradient(circle at 80% 10%, rgba(236, 72, 153, 0.08), transparent 28%),
    radial-gradient(circle at 0% 0%, rgba(99, 102, 241, 0.08), transparent 26%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 255, 0.9));
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.album-hero {
  position: relative;
  padding: 28px;
}

.album-hero__back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: #4338ca;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 16px 28px rgba(67, 56, 202, 0.12);
  }
}

.album-hero__content {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 0.95fr);
  gap: 22px;
  align-items: center;
}

.album-hero__eyebrow,
.board-eyebrow {
  display: inline-flex;
  color: #7c3aed;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.album-hero__copy h1,
.board-header h2 {
  margin: 14px 0 14px;
  color: var(--text-primary);
  font-size: clamp(34px, 6vw, 54px);
  line-height: 1.02;
  font-weight: 800;
}

.album-hero__description {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.85;
}

.album-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 9px 12px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.78);
    color: #64748b;
    font-size: 13px;
    font-weight: 600;
  }
}

.album-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 138px;
  height: 46px;
  padding: 0 18px;
  border: 1px solid rgba(99, 102, 241, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  color: #4338ca;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.hero-btn--primary {
  border-color: transparent;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
}

.album-hero__cover {
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  aspect-ratio: 16 / 10;
  background: rgba(255, 255, 255, 0.72);

  .album-hero__image {
    width: 100%;
    height: 100%;
    display: block;
  }
}

.photo-board {
  padding: 28px;
}

.board-header {
  margin-bottom: 18px;

  h2 {
    margin: 10px 0 0;
    font-size: clamp(24px, 4vw, 32px);
  }
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.photo-item {
  min-width: 0;
}

.photo-card {
  position: relative;
  display: block;
  width: 100%;
  overflow: hidden;
  border: none;
  border-radius: 24px;
  padding: 0;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
  cursor: pointer;

  .photo-card__image {
    width: 100%;
    aspect-ratio: 1;
    display: block;
  }
}

.photo-card__overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: end;
  padding: 18px;
  background: linear-gradient(to top, rgba(15, 23, 42, 0.76), rgba(15, 23, 42, 0.06) 62%);
}

.photo-card__info {
  color: #fff;
  text-align: left;

  h3 {
    margin: 0 0 8px;
    font-size: 16px;
    font-weight: 700;
    line-height: 1.5;
  }
}

.photo-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  color: rgba(226, 232, 240, 0.88);
  font-size: 12px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
}

.empty-state {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 24px;
  border-radius: 24px;
  border: 1px dashed rgba(148, 163, 184, 0.24);
  color: #64748b;

  i {
    color: #8b5cf6;
    font-size: 24px;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    color: var(--text-primary);
  }

  p {
    margin: 0;
    line-height: 1.75;
  }
}

.loading-container {
  height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  color: #6366f1;
  font-size: 40px;
}

@media (max-width: 1024px) {
  .album-hero__content,
  .photo-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .album-detail-page {
    padding: 14px 12px 64px;
  }

  .album-hero,
  .photo-board {
    border-radius: 24px;
    padding: 20px 16px;
  }

  .album-hero__meta,
  .album-hero__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-btn {
    width: 100%;
  }

  .photo-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
