<template>
  <div class="photos-container">
    <section class="photos-hero">
      <div class="hero-copy">
        <div class="title-group">
          <span class="eyebrow">Photo Sphere</span>
          <h1>相册</h1>
          <div class="decorative-line"></div>
        </div>
        <p class="subtitle">每一张照片，都是生活中美好的一次记忆。</p>
        <p class="hero-note">拖动图片星球浏览相册，点击任意封面进入对应相册。</p>
      </div>

      <SphereImageGrid
        v-if="sphereImages.length"
        class="album-sphere"
        :images="sphereImages"
        :container-size="sphereSize"
        :sphere-radius="sphereRadius"
        :min-items="sphereMinItems"
        :base-image-scale="0.13"
        :auto-rotate="true"
        :auto-rotate-speed="0.13"
        @select="openAlbumFromSphere"
      />

      <div v-else class="sphere-empty">
        <i class="fas fa-images"></i>
        <span>相册图片添加后，这里会自动生成图片星球</span>
      </div>

      <div class="header-background">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
      </div>
    </section>

    <div class="photos-grid">
      <div v-for="(album, index) in albums" :key="index" class="album-card" @click="openAlbum(album)">
        <div class="album-cover">
          <img v-lazy="resolveAlbumCover(album.cover)" :key="album.cover" :alt="album.name">
          <div v-if="album.isLock === 1" class="lock-icon">
            <i class="fas fa-lock"></i>
          </div>
        </div>
        <div class="album-info">
          <h3>{{ album.name }}</h3>
          <p>{{ album.description }}</p>
          <span class="photo-count">{{ album.photoNum }}张照片</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getAlbumListApi } from '@/api/album'
import SphereImageGrid from '@/components/common/SphereImageGrid.vue'
import { resolveImageUrl } from '@/utils/image'

export default {
  name: 'Photos',
  components: {
    SphereImageGrid
  },
  data() {
    return {
      albums: [],
      viewportWidth: typeof window === 'undefined' ? 1200 : window.innerWidth
    }
  },
  computed: {
    sphereSize() {
      if (this.viewportWidth <= 480) {
        return 360
      }
      if (this.viewportWidth <= 768) {
        return 430
      }
      return 540
    },
    sphereRadius() {
      return Math.round(this.sphereSize * 0.38)
    },
    sphereMinItems() {
      if (this.viewportWidth <= 480) {
        return 20
      }
      if (this.viewportWidth <= 768) {
        return 26
      }
      return 32
    },
    sphereImages() {
      return this.albums
        .filter(album => album && album.cover)
        .map(album => ({
          id: `album-${album.id}`,
          src: this.resolveAlbumCover(album.cover),
          alt: album.name || '相册封面',
          title: album.name,
          description: album.description,
          album
        }))
    }
  },
  created() {
    this.getAlbumList()
  },
  mounted() {
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
  },
  methods: {
    /**
     * 获取相册列表
     */
    getAlbumList() {
      getAlbumListApi().then(res => {
        this.albums = res.data
      })
    },
    /**
     * 打开相册详情
     */
    openAlbum(album) {
      this.$router.push({
        name: 'PhotoDetail',
        params: { id: album.id }
      })
    },
    openAlbumFromSphere(image) {
      if (image && image.album) {
        this.openAlbum(image.album)
      }
    },
    resolveAlbumCover(url) {
      return resolveImageUrl(url, this.$store.state.defaultImage)
    },
    handleResize() {
      this.viewportWidth = window.innerWidth
    }
  }
}
</script>

<style lang="scss" scoped>
.photos-container {
  padding: 20px 20px 80px;
  max-width: 1280px;
  margin: 0 auto;
}

.photos-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(420px, 1.1fr);
  align-items: center;
  gap: 40px;
  min-height: 590px;
  margin-bottom: 44px;
  padding: 42px 36px 34px;
  overflow: hidden;
  border-radius: 34px;
  background:
    radial-gradient(circle at 82% 16%, rgba(236, 72, 153, 0.12), transparent 30%),
    radial-gradient(circle at 10% 0%, rgba(99, 102, 241, 0.12), transparent 28%),
    linear-gradient(135deg, rgba(248, 250, 252, 0.74), rgba(255, 247, 250, 0.74));

  .hero-copy,
  .album-sphere,
  .sphere-empty {
    position: relative;
    z-index: 2;
  }

  .title-group {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 20px;
  }

  .eyebrow {
    margin-bottom: 12px;
    color: #8b5cf6;
    font-size: 0.82em;
    font-weight: 700;
    letter-spacing: 0.18em;
    text-transform: uppercase;
  }

  h1 {
    font-size: clamp(3em, 7vw, 6em);
    line-height: 0.95;
    font-weight: 800;
    margin-bottom: 15px;
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #ec4899 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    letter-spacing: 2px;
  }

  .decorative-line {
    width: 60px;
    height: 4px;
    background: linear-gradient(90deg, #6366f1, #8b5cf6);
    border-radius: 2px;
    margin-top: -5px;
  }

  .subtitle {
    max-width: 430px;
    font-size: 1.24em;
    color: var(--text-secondary);
    font-weight: 500;
    letter-spacing: 0.5px;
    opacity: 0.8;
    line-height: 1.8;
  }

  .hero-note {
    margin-top: 22px;
    color: #64748b;
    font-size: 0.95em;
    line-height: 1.7;
  }

  .sphere-empty {
    height: 420px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 16px;
    color: #94a3b8;
    border: 1px dashed rgba(148, 163, 184, 0.36);
    border-radius: 50%;

    i {
      font-size: 2.6em;
      color: #a78bfa;
    }
  }

  .header-background {
    position: absolute;
    inset: 0;
    z-index: 1;
    overflow: hidden;
  }

  .circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;
  }

  .circle-1 {
    width: 220px;
    height: 220px;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    top: -100px;
    left: -50px;
  }

  .circle-2 {
    width: 170px;
    height: 170px;
    background: linear-gradient(135deg, #8b5cf6, #ec4899);
    bottom: -50px;
    right: -30px;
  }
}

.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.album-card {
  background: var(--surface);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);

    .album-cover img {
      transform: scale(1.05);
    }
  }

  .album-cover {
    height: 200px;
    overflow: hidden;
    position: relative;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }

    .lock-icon {
      position: absolute;
      top: 10px;
      right: 10px;
      width: 32px;
      height: 32px;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 14px;
    }
  }

  .album-info {
    padding: 20px;

    h3 {
      margin: 0 0 10px;
      font-size: 1.2em;
      color: var(--text-primary);
    }

    p {
      color: var(--text-secondary);
      margin: 0 0 15px;
      font-size: 0.9em;
      line-height: 1.5;
    }

    .photo-count {
      display: inline-block;
      padding: 4px 12px;
      background: rgba(99, 102, 241, 0.1);
      color: #6366f1;
      border-radius: 20px;
      font-size: 0.8em;
      font-weight: 500;
    }
  }
}

@media (max-width: 900px) {
  .photos-hero {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: 32px 18px;
    margin-bottom: 40px;
    text-align: center;

    .title-group {
      align-items: center;
    }

    .subtitle,
    .hero-note {
      margin-left: auto;
      margin-right: auto;
    }
  }
}

@media (max-width: 768px) {
  .photos-grid {
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 15px;
  }
}

@media (max-width: 480px) {
  .photos-container {
    padding: 14px 12px 60px;
  }

  .photos-hero {
    padding: 25px 10px;
    border-radius: 24px;

    h1 {
      font-size: 2.6em;
    }

    .subtitle {
      font-size: 1em;
    }

    .circle-1 {
      width: 150px;
      height: 150px;
    }

    .circle-2 {
      width: 100px;
      height: 100px;
    }
  }

  .photos-grid {
    grid-template-columns: 1fr;
  }
}
</style>
