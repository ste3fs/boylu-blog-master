<template>
  <div
    ref="scene"
    class="stacked-panels-scene"
    :class="{ 'stacked-panels-scene--embedded': embedded }"
    @mousemove="handleMove"
    @mouseleave="handleLeave"
  >
    <div class="scene-noise"></div>
    <div class="scene-aurora scene-aurora--a"></div>
    <div class="scene-aurora scene-aurora--b"></div>
    <div
      class="stacked-panels-stage"
      :style="stageStyle"
    >
      <div
        v-for="panel in panelItems"
        :key="panel.index"
        class="stacked-panel"
        :style="getPanelStyle(panel)"
      >
        <div
          class="stacked-panel__image"
          :style="{ backgroundImage: `url(${panel.image})` }"
        ></div>
        <div class="stacked-panel__gradient" :style="{ background: panel.gradient }"></div>
        <div class="stacked-panel__shade"></div>
        <div class="stacked-panel__border"></div>
      </div>
    </div>
  </div>
</template>

<script>
const PANEL_COUNT = 22
const DEFAULT_CONFIG = {
  rotateX: 16,
  rotateY: -38,
  rotateYRange: 12,
  rotateXRange: 8,
  widthBase: 200,
  widthGrow: 80,
  heightBase: 280,
  heightGrow: 120,
  depthStep: 42,
  hoverLift: 70,
  scaleMin: 0.35,
  influenceSigma: 2.8
}
const EMBEDDED_CONFIG = {
  rotateX: 12,
  rotateY: -26,
  rotateYRange: 7,
  rotateXRange: 5,
  widthBase: 132,
  widthGrow: 52,
  heightBase: 184,
  heightGrow: 84,
  depthStep: 28,
  hoverLift: 36,
  scaleMin: 0.62,
  influenceSigma: 2.45
}
const PANEL_IMAGES = [
  'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=600&q=80',
  'https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600&q=80',
  'https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=600&q=80',
  'https://images.unsplash.com/photo-1501854140801-50d01698950b?w=600&q=80',
  'https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=600&q=80',
  'https://images.unsplash.com/photo-1475924156734-496f6cac6ec1?w=600&q=80',
  'https://images.unsplash.com/photo-1518495973542-4542c06a5843?w=600&q=80',
  'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&q=80',
  'https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?w=600&q=80',
  'https://images.unsplash.com/photo-1510784722466-f2aa240c3c4a?w=600&q=80',
  'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=600&q=80',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600&q=80'
]
const GRADIENTS = [
  'linear-gradient(135deg, rgba(99,55,255,0.58) 0%, rgba(236,72,153,0.42) 100%)',
  'linear-gradient(135deg, rgba(6,182,212,0.54) 0%, rgba(59,130,246,0.46) 100%)',
  'linear-gradient(135deg, rgba(245,158,11,0.52) 0%, rgba(239,68,68,0.44) 100%)',
  'linear-gradient(135deg, rgba(16,185,129,0.46) 0%, rgba(6,182,212,0.54) 100%)',
  'linear-gradient(135deg, rgba(236,72,153,0.52) 0%, rgba(245,158,11,0.4) 100%)',
  'linear-gradient(135deg, rgba(59,130,246,0.54) 0%, rgba(99,55,255,0.42) 100%)'
]

export default {
  name: 'StackedPanelsGallery',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      rotateX: this.config.rotateX,
      rotateY: this.config.rotateY,
      activePosition: (PANEL_COUNT - 1) / 2,
      hovered: false,
      panelItems: Array.from({ length: PANEL_COUNT }, (_, index) => ({
        index,
        image: PANEL_IMAGES[index % PANEL_IMAGES.length],
        gradient: GRADIENTS[index % GRADIENTS.length]
      }))
    }
  },
  computed: {
    config() {
      return this.embedded ? EMBEDDED_CONFIG : DEFAULT_CONFIG
    },
    stageStyle() {
      return {
        transform: `rotateX(${this.rotateX}deg) rotateY(${this.rotateY}deg)`
      }
    }
  },
  watch: {
    embedded() {
      this.handleLeave()
    }
  },
  methods: {
    handleMove(event) {
      const rect = this.$refs.scene && this.$refs.scene.getBoundingClientRect()
      if (!rect) {
        return
      }

      const px = (event.clientX - rect.left) / rect.width
      const py = (event.clientY - rect.top) / rect.height
      this.hovered = true
      this.rotateY = Number((this.config.rotateY + (px - 0.5) * this.config.rotateYRange).toFixed(3))
      this.rotateX = Number((this.config.rotateX + (py - 0.5) * -this.config.rotateXRange).toFixed(3))
      this.activePosition = Number((px * (PANEL_COUNT - 1)).toFixed(3))
    },
    handleLeave() {
      this.hovered = false
      this.rotateX = this.config.rotateX
      this.rotateY = this.config.rotateY
      this.activePosition = (PANEL_COUNT - 1) / 2
    },
    getPanelStyle(panel) {
      const t = panel.index / (PANEL_COUNT - 1)
      const width = this.config.widthBase + t * this.config.widthGrow
      const height = this.config.heightBase + t * this.config.heightGrow
      const baseZ = (panel.index - (PANEL_COUNT - 1)) * this.config.depthStep
      const distance = Math.abs(panel.index - this.activePosition)
      const influence = Math.exp(-(distance * distance) / (2 * this.config.influenceSigma * this.config.influenceSigma))
      const offsetY = this.hovered ? -influence * this.config.hoverLift : 0
      const scaleY = this.hovered ? this.config.scaleMin + influence * (1 - this.config.scaleMin) : 1
      const opacity = this.embedded ? 0.3 + t * 0.7 : 0.24 + t * 0.76
      const borderOpacity = this.embedded ? 0.1 + t * 0.16 : 0.08 + t * 0.22

      return {
        width: `${width}px`,
        height: `${height}px`,
        marginLeft: `${-width / 2}px`,
        marginTop: `${-height / 2}px`,
        opacity,
        transform: `translate3d(0, ${offsetY}px, ${baseZ}px) scaleY(${scaleY})`,
        '--panel-border-opacity': borderOpacity
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.stacked-panels-scene {
  position: relative;
  min-height: 620px;
  width: 100%;
  overflow: hidden;
  border-radius: 32px;
  background:
    radial-gradient(circle at top left, rgba($primary, 0.16), transparent 34%),
    radial-gradient(circle at bottom right, rgba($secondary, 0.16), transparent 32%),
    linear-gradient(135deg, #050816 0%, #0f172a 48%, #111827 100%);
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.28);
  perspective: 960px;
  isolation: isolate;
  user-select: none;
}

.stacked-panels-scene--embedded {
  min-height: 260px;
  border-radius: 26px;
  background:
    radial-gradient(circle at top left, rgba(#38bdf8, 0.18), transparent 30%),
    radial-gradient(circle at bottom right, rgba(#8b5cf6, 0.14), transparent 28%),
    linear-gradient(135deg, rgba(8, 15, 33, 0.92), rgba(15, 23, 42, 0.8));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 18px 50px rgba(15, 23, 42, 0.22);
  perspective: 760px;
}

.scene-noise,
.scene-aurora {
  pointer-events: none;
  position: absolute;
  inset: 0;
}

.scene-noise {
  opacity: 0.08;
  background-image: radial-gradient(rgba(255, 255, 255, 0.75) 0.8px, transparent 0.8px);
  background-size: 18px 18px;
  mix-blend-mode: soft-light;
}

.stacked-panels-scene--embedded .scene-noise {
  opacity: 0.05;
  background-size: 20px 20px;
}

.scene-aurora {
  filter: blur(34px);
}

.scene-aurora--a {
  top: -12%;
  left: 10%;
  width: 34%;
  height: 40%;
  background: rgba(#8b5cf6, 0.34);
  animation: auroraFloatA 8s ease-in-out infinite;
}

.scene-aurora--b {
  right: 6%;
  bottom: -10%;
  width: 40%;
  height: 44%;
  background: rgba(#06b6d4, 0.28);
  animation: auroraFloatB 9s ease-in-out infinite;
}

.stacked-panels-scene--embedded .scene-aurora {
  filter: blur(28px);
}

.stacked-panels-scene--embedded .scene-aurora--a {
  top: -18%;
  left: 4%;
  width: 30%;
  height: 36%;
  background: rgba(#8b5cf6, 0.24);
}

.stacked-panels-scene--embedded .scene-aurora--b {
  right: -2%;
  bottom: -18%;
  width: 38%;
  height: 42%;
  background: rgba(#38bdf8, 0.18);
}

.stacked-panels-stage {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 0;
  height: 0;
  transform-style: preserve-3d;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.stacked-panels-scene--embedded .stacked-panels-stage {
  top: 57%;
  transition-duration: 0.26s;
}

.stacked-panel {
  position: absolute;
  left: 0;
  top: 0;
  overflow: hidden;
  border-radius: 24px;
  transform-origin: bottom center;
  transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.22s ease;
  will-change: transform;
  pointer-events: none;
}

.stacked-panels-scene--embedded .stacked-panel {
  border-radius: 20px;
}

.stacked-panel__image,
.stacked-panel__gradient,
.stacked-panel__shade,
.stacked-panel__border {
  position: absolute;
  inset: 0;
}

.stacked-panel__image {
  background-size: cover;
  background-position: center;
}

.stacked-panel__gradient {
  mix-blend-mode: multiply;
}

.stacked-panel__shade {
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.08) 0%, rgba(0, 0, 0, 0.34) 100%);
}

.stacked-panels-scene--embedded .stacked-panel__shade {
  background: linear-gradient(to bottom, rgba(7, 11, 24, 0.04) 0%, rgba(7, 11, 24, 0.28) 100%);
}

.stacked-panel__border {
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, var(--panel-border-opacity));
  box-sizing: border-box;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

@keyframes auroraFloatA {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(24px, 18px, 0);
  }
}

@keyframes auroraFloatB {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(-26px, -18px, 0);
  }
}

@include responsive(md) {
  .stacked-panels-scene {
    min-height: 520px;
    border-radius: 24px;
  }

  .stacked-panels-scene--embedded {
    min-height: 220px;
    border-radius: 22px;
  }
}

@include responsive(sm) {
  .stacked-panels-scene {
    min-height: 420px;
    border-radius: 20px;
  }

  .stacked-panels-scene--embedded {
    min-height: 188px;
    border-radius: 18px;
  }
}
</style>
