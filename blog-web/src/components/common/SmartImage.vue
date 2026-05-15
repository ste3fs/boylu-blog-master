<template>
  <picture
    class="smart-image"
    :class="{ 'is-loaded': loaded, 'is-error': failed }"
    :style="wrapperStyle"
  >
    <source
      v-if="!failed && !fallbackSrc && avifSrcset"
      type="image/avif"
      :srcset="avifSrcset"
      :sizes="sizes"
    >
    <source
      v-if="!failed && !fallbackSrc && webpSrcset"
      type="image/webp"
      :srcset="webpSrcset"
      :sizes="sizes"
    >
    <source
      v-if="!failed && !fallbackSrc && jpgSrcset"
      type="image/jpeg"
      :srcset="jpgSrcset"
      :sizes="sizes"
    >
    <img
      v-if="blurDataURL && !loaded && !failed"
      class="smart-image__blur"
      :src="blurDataURL"
      alt=""
      aria-hidden="true"
    >
    <img
      class="smart-image__img"
      :src="imgSrc"
      :alt="imageAlt"
      :width="imageWidth"
      :height="imageHeight"
      :loading="priority ? 'eager' : 'lazy'"
      :fetchpriority="priority ? 'high' : null"
      decoding="async"
      @load="handleLoad"
      @error="handleError"
    >
  </picture>
</template>

<script>
import {
  buildLocalImageSrcset,
  buildLocalImageStyleUrl,
  isStylableImageUrl,
  resolveImageUrl,
} from '@/utils/image'

const DEFAULT_FALLBACK = '/images/boylu-image-loading-fallback.webp'
const LOADED_CACHE_PREFIX = 'boylu:smart-image:loaded:'
const LOADED_CACHE_TTL = 20 * 24 * 60 * 60 * 1000

export default {
  name: 'SmartImage',
  props: {
    image: {
      type: Object,
      required: true
    },
    sizes: {
      type: String,
      default: '(max-width: 768px) 100vw, 33vw'
    },
    priority: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loaded: false,
      failed: false,
      fallbackSrc: '',
      currentPrimarySrc: ''
    }
  },
  computed: {
    imageAlt() {
      return (this.image && this.image.alt) || ''
    },
    imageWidth() {
      return Number(this.image && this.image.width) || 1600
    },
    imageHeight() {
      return Number(this.image && this.image.height) || 900
    },
    blurDataURL() {
      return (this.image && this.image.blurDataURL) || ''
    },
    dominantColor() {
      return (this.image && this.image.dominantColor) || '#eef4ff'
    },
    wrapperStyle() {
      return {
        backgroundColor: this.dominantColor,
        aspectRatio: `${this.imageWidth} / ${this.imageHeight}`
      }
    },
    avifSrcset() {
      return this.buildSrcset(this.image && this.image.variants && this.image.variants.avif)
    },
    webpSrcset() {
      const styled = this.buildStyledSrcset('webp')
      if (styled) {
        return styled
      }
      return this.buildSrcset(this.image && this.image.variants && this.image.variants.webp)
    },
    jpgSrcset() {
      const styled = this.buildStyledSrcset('jpg')
      if (styled) {
        return styled
      }
      return this.buildSrcset(this.image && this.image.variants && this.image.variants.jpg)
    },
    primarySrc() {
      const styledDefault = this.resolveStyledDefault(this.image)
      if (styledDefault) {
        return styledDefault
      }
      return this.resolveBestFallback(this.image) || DEFAULT_FALLBACK
    },
    imgSrc() {
      return this.fallbackSrc || this.primarySrc
    }
  },
  watch: {
    primarySrc: {
      immediate: true,
      handler(nextSrc, prevSrc) {
        if (nextSrc === prevSrc && this.currentPrimarySrc) {
          return
        }
        this.currentPrimarySrc = nextSrc || ''
        this.fallbackSrc = ''
        this.failed = false
        this.loaded = this.hasLoadedRecently(nextSrc)
      }
    }
  },
  methods: {
    buildSrcset(variants) {
      if (!variants || typeof variants !== 'object') {
        return ''
      }

      return Object.keys(variants)
        .map(width => Number(width))
        .filter(width => Number.isFinite(width) && variants[width])
        .sort((a, b) => a - b)
        .map(width => `${resolveImageUrl(variants[width])} ${width}w`)
        .join(', ')
    },
    buildStyledSrcset(format) {
      const source = this.resolveStyleSource(this.image)
      return source ? buildLocalImageSrcset(source, format) : ''
    },
    resolveStyleSource(image) {
      if (!image) {
        return ''
      }

      const variants = image.variants || {}
      const candidates = [
        image.styleSource,
        image.source,
        image.fallback,
        this.pickVariant(variants.jpg),
        this.pickVariant(variants.webp),
        this.pickVariant(variants.avif)
      ]

      for (const candidate of candidates) {
        const normalized = resolveImageUrl(candidate)
        if (normalized && isStylableImageUrl(normalized)) {
          return normalized
        }
      }
      return ''
    },
    resolveStyledDefault(image) {
      const source = this.resolveStyleSource(image)
      if (!source) {
        return ''
      }
      return buildLocalImageStyleUrl(source, this.priority ? 960 : 640, 'webp')
    },
    resolveBestFallback(image) {
      const directFallback = resolveImageUrl(image && image.fallback)
      if (directFallback && directFallback !== DEFAULT_FALLBACK) {
        return directFallback
      }

      const variants = (image && image.variants) || {}
      const groups = [variants.jpg, variants.webp, variants.avif]
      for (const group of groups) {
        const src = this.pickVariant(group)
        if (src) {
          return src
        }
      }
      return directFallback || ''
    },
    pickVariant(variants) {
      if (!variants || typeof variants !== 'object') {
        return ''
      }
      const widths = Object.keys(variants)
        .map(width => Number(width))
        .filter(width => Number.isFinite(width) && variants[width])
        .sort((a, b) => Math.abs(a - 960) - Math.abs(b - 960))

      return widths.length ? resolveImageUrl(variants[widths[0]]) : ''
    },
    cacheKey(src) {
      if (!src || typeof window === 'undefined') {
        return ''
      }
      return `${LOADED_CACHE_PREFIX}${encodeURIComponent(src)}`
    },
    hasLoadedRecently(src) {
      if (!src || typeof window === 'undefined' || !window.localStorage) {
        return false
      }
      try {
        const value = Number(window.localStorage.getItem(this.cacheKey(src)) || 0)
        return value > 0 && Date.now() - value < LOADED_CACHE_TTL
      } catch (error) {
        return false
      }
    },
    rememberLoaded(src) {
      if (!src || typeof window === 'undefined' || !window.localStorage) {
        return
      }
      try {
        window.localStorage.setItem(this.cacheKey(src), String(Date.now()))
      } catch (error) {
        // Ignore storage failures; image loading should keep working.
      }
    },
    forgetLoaded(src) {
      if (!src || typeof window === 'undefined' || !window.localStorage) {
        return
      }
      try {
        window.localStorage.removeItem(this.cacheKey(src))
      } catch (error) {
        // Ignore storage failures; image loading should keep working.
      }
    },
    handleLoad(event) {
      this.loaded = true
      if (!this.fallbackSrc) {
        this.failed = false
      }
      const targetSrc = event && event.target ? (event.target.currentSrc || event.target.src || '') : ''
      this.rememberLoaded(targetSrc)
      this.rememberLoaded(this.primarySrc)
    },
    handleError(event) {
      const directFallback = this.resolveBestFallback(this.image)
      const currentSrc = event && event.target ? (event.target.currentSrc || event.target.src || '') : ''
      this.forgetLoaded(currentSrc)
      this.forgetLoaded(this.primarySrc)

      if (!this.fallbackSrc && directFallback && currentSrc !== directFallback) {
        this.failed = true
        this.loaded = this.hasLoadedRecently(directFallback)
        this.fallbackSrc = directFallback
        if (event && event.target) {
          event.target.src = directFallback
        }
        return
      }

      if (this.fallbackSrc === DEFAULT_FALLBACK) {
        return
      }
      this.failed = true
      this.fallbackSrc = DEFAULT_FALLBACK
      if (event && event.target) {
        event.target.src = DEFAULT_FALLBACK
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.smart-image {
  position: relative;
  display: block;
  width: 100%;
  overflow: hidden;
}

.smart-image__img,
.smart-image__blur {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.smart-image__img {
  opacity: 0;
  transition: opacity 0.28s ease, transform 0.35s ease;
}

.smart-image__blur {
  filter: blur(14px);
  transform: scale(1.08);
}

.smart-image.is-loaded .smart-image__img,
.smart-image.is-error .smart-image__img {
  opacity: 1;
}
</style>
