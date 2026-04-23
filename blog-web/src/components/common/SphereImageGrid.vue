<template>
  <div
    ref="container"
    class="sphere-image-grid"
    :class="{ dragging: isDragging }"
    :style="containerStyle"
    @mousedown="handleMouseDown"
    @touchstart="handleTouchStart"
  >
    <button
      v-for="(image, index) in renderedImages"
      :key="image.renderId"
      ref="imageNodes"
      class="sphere-image"
      :class="{ hovered: hoveredIndex === index }"
      :style="imageBaseStyle"
      type="button"
      @mouseenter="hoveredIndex = index"
      @mouseleave="hoveredIndex = null"
      @click="handleImageClick(image)"
    >
      <img
        :src="image.src"
        :alt="image.alt || image.title || 'album image'"
        draggable="false"
        :loading="index < 6 ? 'eager' : 'lazy'"
        decoding="async"
      />
      <span class="image-glow"></span>
    </button>
  </div>
</template>

<script>
const degreesToRadians = (degrees) => degrees * (Math.PI / 180)

const normalizeAngle = (angle) => {
  let normalized = angle
  while (normalized > 180) normalized -= 360
  while (normalized < -180) normalized += 360
  return normalized
}

export default {
  name: 'SphereImageGrid',
  props: {
    images: {
      type: Array,
      default: () => []
    },
    containerSize: {
      type: Number,
      default: 560
    },
    sphereRadius: {
      type: Number,
      default: 210
    },
    minItems: {
      type: Number,
      default: 36
    },
    dragSensitivity: {
      type: Number,
      default: 0.45
    },
    momentumDecay: {
      type: Number,
      default: 0.95
    },
    maxRotationSpeed: {
      type: Number,
      default: 4.5
    },
    baseImageScale: {
      type: Number,
      default: 0.13
    },
    autoRotate: {
      type: Boolean,
      default: true
    },
    autoRotateSpeed: {
      type: Number,
      default: 0.16
    }
  },
  data() {
    return {
      lastPointer: {
        x: 0,
        y: 0
      },
      isDragging: false,
      hasDragged: false,
      hoveredIndex: null,
      frameId: null
    }
  },
  created() {
    this._sphereRotation = {
      x: 12,
      y: -18
    }
    this._sphereVelocity = {
      x: 0,
      y: 0
    }
    this._styleCache = []
    this._visibilityCache = []
    this._zIndexCache = []
    this._lastFrameTime = 0
      this._lastDepthSyncTime = 0
      this._isDocumentVisible = true
      this._isInViewport = true
      this._lastDragUpdateTime = 0
      this._intersectionObserver = null
  },
  computed: {
    renderedImages() {
      if (!this.images.length) {
        return []
      }

      const targetCount = Math.max(this.minItems, this.images.length)
      return Array.from({ length: targetCount }, (_, index) => {
        const source = this.images[index % this.images.length]
        return {
          ...source,
          renderId: `${source.id || 'image'}-${index}`
        }
      })
    },
    imageSize() {
      return this.containerSize * this.baseImageScale
    },
    containerStyle() {
      return {
        width: `${this.containerSize}px`,
        height: `${this.containerSize}px`
      }
    },
    imageBaseStyle() {
      return {
        width: `${this.imageSize}px`,
        height: `${this.imageSize}px`
      }
    },
    spherePositions() {
      const count = this.renderedImages.length
      if (!count) {
        return []
      }

      const goldenAngle = Math.PI * (3 - Math.sqrt(5))
      return this.renderedImages.map((_, index) => {
        const y = 1 - (index / Math.max(count - 1, 1)) * 2
        const radiusAtY = Math.sqrt(1 - y * y)
        const theta = goldenAngle * index

        return {
          x: Math.cos(theta) * radiusAtY * this.sphereRadius,
          y: y * this.sphereRadius,
          z: Math.sin(theta) * radiusAtY * this.sphereRadius
        }
      })
    }
  },
  watch: {
    renderedImages() {
      this.resetSphereCaches()
      this.$nextTick(this.updateSphereDom)
    },
    containerSize() {
      this.resetSphereCaches()
      this.$nextTick(this.updateSphereDom)
    },
    sphereRadius() {
      this.resetSphereCaches()
      this.$nextTick(this.updateSphereDom)
    },
    hoveredIndex() {
      this.updateSphereDom()
    }
  },
  mounted() {
    this.$nextTick(this.updateSphereDom)
    this.setupVisibilityGuards()
    this.startAnimation()
    document.addEventListener('mousemove', this.handleMouseMove)
    document.addEventListener('mouseup', this.handlePointerEnd)
    document.addEventListener('touchmove', this.handleTouchMove, { passive: false })
    document.addEventListener('touchend', this.handlePointerEnd)
  },
  beforeDestroy() {
    if (this.frameId) {
      cancelAnimationFrame(this.frameId)
    }
    document.removeEventListener('mousemove', this.handleMouseMove)
    document.removeEventListener('mouseup', this.handlePointerEnd)
    document.removeEventListener('touchmove', this.handleTouchMove)
    document.removeEventListener('touchend', this.handlePointerEnd)
    document.removeEventListener('visibilitychange', this.handleDocumentVisibilityChange)
    if (this._intersectionObserver) {
      this._intersectionObserver.disconnect()
    }
  },
  methods: {
    clampSpeed(speed) {
      return Math.max(-this.maxRotationSpeed, Math.min(this.maxRotationSpeed, speed))
    },
    resetSphereCaches() {
      this._styleCache = []
      this._visibilityCache = []
      this._zIndexCache = []
      this._lastDepthSyncTime = 0
    },
    setupVisibilityGuards() {
      this._isDocumentVisible = document.visibilityState !== 'hidden'
      document.addEventListener('visibilitychange', this.handleDocumentVisibilityChange)

      if ('IntersectionObserver' in window && this.$refs.container) {
        this._intersectionObserver = new IntersectionObserver((entries) => {
          const entry = entries[0]
          this._isInViewport = Boolean(entry && entry.isIntersecting)
          if (this._isInViewport) {
            this._lastFrameTime = 0
            this.updateSphereDom()
          }
        }, {
          root: null,
          threshold: 0.08
        })
        this._intersectionObserver.observe(this.$refs.container)
      }
    },
    handleDocumentVisibilityChange() {
      this._isDocumentVisible = document.visibilityState !== 'hidden'
      if (this._isDocumentVisible) {
        this._lastFrameTime = 0
        this.updateSphereDom()
      }
    },
    startAnimation() {
      const tick = (timestamp) => {
        const shouldAnimate = this._isDocumentVisible && this._isInViewport && this.renderedImages.length

        if (shouldAnimate) {
          const lastFrameTime = this._lastFrameTime || timestamp
          const frameDelta = Math.min(Math.max((timestamp - lastFrameTime) / 16.67, 0.25), 1.6)
          this._lastFrameTime = timestamp

          if (!this.isDragging) {
            const decay = Math.pow(this.momentumDecay, frameDelta)
            this._sphereVelocity.x *= decay
            this._sphereVelocity.y *= decay

            const autoSpeed = this.autoRotate ? this.autoRotateSpeed * frameDelta : 0
            this._sphereRotation.x = normalizeAngle(this._sphereRotation.x + this.clampSpeed(this._sphereVelocity.x) * frameDelta)
            this._sphereRotation.y = normalizeAngle(this._sphereRotation.y + autoSpeed + this.clampSpeed(this._sphereVelocity.y) * frameDelta)
          }

          this.updateSphereDom(timestamp)
        } else {
          this._lastFrameTime = 0
        }

        this.frameId = requestAnimationFrame(tick)
      }

      this.frameId = requestAnimationFrame(tick)
    },
    getImageNodes() {
      const nodes = this.$refs.imageNodes || []
      return Array.isArray(nodes) ? nodes : [nodes]
    },
    updateSphereDom(timestamp = 0) {
      const nodes = this.getImageNodes()
      const positions = this.spherePositions

      if (!nodes.length || !positions.length) {
        return
      }

      const rotX = degreesToRadians(this._sphereRotation.x)
      const rotY = degreesToRadians(this._sphereRotation.y)
      const sinX = Math.sin(rotX)
      const cosX = Math.cos(rotX)
      const sinY = Math.sin(rotY)
      const cosY = Math.cos(rotY)
      const shouldSyncDepth = !timestamp || timestamp - this._lastDepthSyncTime > (this.isDragging ? 140 : 90)

      if (shouldSyncDepth) {
        this._lastDepthSyncTime = timestamp
      }

      positions.forEach((position, index) => {
        const node = nodes[index]
        if (!node) {
          return
        }

        let { x, y, z } = position
        const rotatedX = x * cosY + z * sinY
        const rotatedZ = -x * sinY + z * cosY
        x = rotatedX
        z = rotatedZ

        const rotatedY = y * cosX - z * sinX
        const finalZ = y * sinX + z * cosX
        y = rotatedY
        z = finalZ

        const fadeOpacity = z < -this.sphereRadius * 0.35
          ? Math.max(0, (z + this.sphereRadius) / (this.sphereRadius * 0.65))
          : 1

        const depthRatio = (z + this.sphereRadius) / (2 * this.sphereRadius)
        const baseScale = 0.58 + depthRatio * 0.52
        const hoverScale = !this.isDragging && this.hoveredIndex === index ? 1.16 : 1
        const isVisible = fadeOpacity > 0.08
        const opacity = isVisible ? Math.max(0.12, fadeOpacity).toFixed(3) : '0'
        const transform = `translate3d(${x.toFixed(2)}px, ${y.toFixed(2)}px, 0) translate(-50%, -50%) scale(${(baseScale * hoverScale).toFixed(3)})`

        if (this._styleCache[index] !== transform) {
          node.style.transform = transform
          this._styleCache[index] = transform
        }

        if (node.style.opacity !== opacity) {
          node.style.opacity = opacity
        }

        if (this._visibilityCache[index] !== isVisible) {
          node.style.pointerEvents = isVisible ? '' : 'none'
          this._visibilityCache[index] = isVisible
        }

        if (shouldSyncDepth) {
          const zIndex = Math.round(1000 + z)
          if (this._zIndexCache[index] !== zIndex) {
            node.style.zIndex = zIndex
            this._zIndexCache[index] = zIndex
          }
        }
      })
    },
    handleMouseDown(event) {
      event.preventDefault()
      this.startDrag(event.clientX, event.clientY)
    },
    handleTouchStart(event) {
      const touch = event.touches[0]
      if (!touch) {
        return
      }
      this.startDrag(touch.clientX, touch.clientY)
    },
    startDrag(x, y) {
      this.isDragging = true
      this.hasDragged = false
      this._sphereVelocity = { x: 0, y: 0 }
      this.lastPointer = { x, y }
    },
    handleMouseMove(event) {
      if (!this.isDragging) {
        return
      }
      this.updateDrag(event.clientX, event.clientY)
    },
    handleTouchMove(event) {
      if (!this.isDragging) {
        return
      }

      const touch = event.touches[0]
      if (!touch) {
        return
      }

      event.preventDefault()
      this.updateDrag(touch.clientX, touch.clientY)
    },
    updateDrag(x, y) {
      const deltaX = x - this.lastPointer.x
      const deltaY = y - this.lastPointer.y
      const now = performance.now()

      if (Math.abs(deltaX) > 2 || Math.abs(deltaY) > 2) {
        this.hasDragged = true
      }

      const nextVelocity = {
        x: this.clampSpeed(-deltaY * this.dragSensitivity),
        y: this.clampSpeed(deltaX * this.dragSensitivity)
      }

      this._sphereRotation.x = normalizeAngle(this._sphereRotation.x + nextVelocity.x)
      this._sphereRotation.y = normalizeAngle(this._sphereRotation.y + nextVelocity.y)
      this._sphereVelocity = nextVelocity
      this.lastPointer = { x, y }

      if (now - this._lastDragUpdateTime > 16) {
        this.updateSphereDom(now)
        this._lastDragUpdateTime = now
      }
    },
    handlePointerEnd() {
      this.isDragging = false
      setTimeout(() => {
        this.hasDragged = false
      }, 0)
    },
    handleImageClick(image) {
      if (this.hasDragged) {
        return
      }
      this.$emit('select', image)
    }
  }
}
</script>

<style lang="scss" scoped>
.sphere-image-grid {
  position: relative;
  max-width: min(100%, 560px);
  max-height: min(100vw, 560px);
  margin: 0 auto;
  border-radius: 50%;
  cursor: grab;
  user-select: none;
  touch-action: none;
  background:
    radial-gradient(circle at 42% 40%, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.18) 28%, transparent 56%),
    radial-gradient(circle at center, rgba(129, 140, 248, 0.15), rgba(236, 72, 153, 0.08) 44%, transparent 70%);
  box-shadow: 0 26px 64px rgba(99, 102, 241, 0.14);
  animation: sphere-enter 0.8s ease both;
  contain: layout paint style;
  transform: translateZ(0);

  &.dragging {
    cursor: grabbing;

    .sphere-image {
      box-shadow: none;
    }

    .image-glow {
      opacity: 0;
    }
  }

  &::before,
  &::after {
    content: '';
    position: absolute;
    inset: 11%;
    border-radius: 50%;
    pointer-events: none;
  }

  &::before {
    border: 1px solid rgba(129, 140, 248, 0.16);
    box-shadow: inset 0 0 50px rgba(255, 255, 255, 0.45);
  }

  &::after {
    inset: 23%;
    border: 1px dashed rgba(148, 163, 184, 0.18);
  }
}

.sphere-image {
  position: absolute;
  left: 50%;
  top: 50%;
  padding: 0;
  border: 0;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.5);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.45);
  transition:
    box-shadow 0.18s ease;
  cursor: pointer;
  outline: none;
  backface-visibility: hidden;
  contain: layout paint style;
  transform: translate3d(0, 0, 0) translate(-50%, -50%);
  transform-origin: center;
  will-change: transform, opacity;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    pointer-events: none;
    transform: translateZ(0);
  }

  .image-glow {
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.32), transparent 44%);
    pointer-events: none;
    transition: opacity 0.16s ease;
  }

  &:hover,
  &.hovered {
    box-shadow:
      0 10px 26px rgba(99, 102, 241, 0.2),
      0 0 0 2px rgba(255, 255, 255, 0.82);
  }
}

@keyframes sphere-enter {
  from {
    opacity: 0;
    transform: translateY(22px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 768px) {
  .sphere-image-grid {
    transform: scale(0.86);
    transform-origin: center;
    margin-top: -34px;
    margin-bottom: -34px;
  }
}
</style>
