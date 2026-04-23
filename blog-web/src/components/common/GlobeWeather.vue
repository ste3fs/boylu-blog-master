<template>
  <div class="globe-weather" :class="{ 'is-loading': loading }">
    <div class="globe-weather__head">
      <div>
        <span class="eyebrow">Live Weather</span>
        <h3>全球实时天气</h3>
      </div>
      <span class="sync-state">{{ syncText }}</span>
    </div>

    <div class="globe-weather__stage">
      <canvas
        ref="canvas"
        class="globe-weather__canvas"
        @pointerdown="handlePointerDown"
      ></canvas>

      <span
        v-for="marker in weatherMarkers"
        :key="marker.id"
        class="weather-marker"
        :class="`tone-${marker.tone}`"
        :style="getMarkerStyle(marker)"
        :title="`${marker.city} ${marker.temperatureText} ${marker.condition}`"
      >
        <span class="marker-bubble">
          <span class="marker-main">{{ marker.emoji }}</span>
          <span class="marker-accent">{{ marker.accent }}</span>
        </span>
        <span class="marker-temp">{{ marker.temperatureText }}</span>
      </span>
    </div>

    <div class="weather-list">
      <div
        v-for="marker in featuredWeather"
        :key="`${marker.id}-item`"
        class="weather-item"
      >
        <span class="weather-item__icon">
          <span>{{ marker.emoji }}</span>
          <small>{{ marker.accent }}</small>
        </span>
        <span class="weather-item__meta">
          <span>{{ marker.city }}</span>
          <em>{{ marker.condition }}</em>
        </span>
        <strong>{{ marker.temperatureText }}</strong>
      </div>
    </div>
  </div>
</template>

<script>
import createGlobe from "cobe";

const WEATHER_LOCATIONS = [
  { id: "weather-beijing", city: "北京", location: [39.9042, 116.4074] },
  { id: "weather-tokyo", city: "东京", location: [35.6762, 139.6503] },
  { id: "weather-london", city: "伦敦", location: [51.5072, -0.1276] },
  { id: "weather-new-york", city: "纽约", location: [40.7128, -74.006] },
  { id: "weather-dubai", city: "迪拜", location: [25.2048, 55.2708] },
  { id: "weather-singapore", city: "新加坡", location: [1.3521, 103.8198] },
  { id: "weather-north-pacific", city: "北太平洋", location: [24.0, -155.0] },
  { id: "weather-equatorial-atlantic", city: "赤道大西洋", location: [0.0, -28.0] },
  { id: "weather-sydney", city: "悉尼", location: [-33.8688, 151.2093] },
  { id: "weather-auckland", city: "奥克兰", location: [-36.8485, 174.7633] },
  { id: "weather-rio", city: "里约", location: [-22.9068, -43.1729] },
  { id: "weather-cape-town", city: "开普敦", location: [-33.9249, 18.4241] },
  { id: "weather-buenos-aires", city: "布宜诺斯艾利斯", location: [-34.6037, -58.3816] },
  { id: "weather-south-pacific", city: "南太平洋", location: [-28.0, -132.0] },
  { id: "weather-south-atlantic", city: "南大西洋", location: [-32.0, -18.0] },
  { id: "weather-south-indian", city: "南印度洋", location: [-25.0, 82.0] },
  { id: "weather-southern-ocean", city: "南冰洋", location: [-58.0, 120.0] },
];

const weatherCodeMap = {
  0: { emoji: "☀️", accent: "✨", label: "晴", tone: "sunny" },
  1: { emoji: "🌤️", accent: "☀️", label: "大部晴朗", tone: "sunny" },
  2: { emoji: "⛅", accent: "🌥️", label: "局部多云", tone: "cloudy" },
  3: { emoji: "☁️", accent: "🌫️", label: "阴", tone: "cloudy" },
  45: { emoji: "🌫️", accent: "🫧", label: "雾", tone: "fog" },
  48: { emoji: "🌫️", accent: "❄️", label: "雾凇", tone: "fog" },
  51: { emoji: "🌦️", accent: "💧", label: "小毛毛雨", tone: "rain" },
  53: { emoji: "🌦️", accent: "💦", label: "毛毛雨", tone: "rain" },
  55: { emoji: "🌧️", accent: "💦", label: "强毛毛雨", tone: "rain" },
  56: { emoji: "🌧️", accent: "🧊", label: "冻毛毛雨", tone: "rain" },
  57: { emoji: "🌧️", accent: "🧊", label: "强冻毛毛雨", tone: "rain" },
  61: { emoji: "🌧️", accent: "💧", label: "小雨", tone: "rain" },
  63: { emoji: "🌧️", accent: "💦", label: "中雨", tone: "rain" },
  65: { emoji: "🌧️", accent: "🌊", label: "大雨", tone: "rain" },
  66: { emoji: "🌧️", accent: "🧊", label: "冻雨", tone: "rain" },
  67: { emoji: "🌧️", accent: "🧊", label: "强冻雨", tone: "rain" },
  71: { emoji: "🌨️", accent: "❄️", label: "小雪", tone: "snow" },
  73: { emoji: "🌨️", accent: "☃️", label: "中雪", tone: "snow" },
  75: { emoji: "❄️", accent: "☃️", label: "大雪", tone: "snow" },
  77: { emoji: "❄️", accent: "🧊", label: "雪粒", tone: "snow" },
  80: { emoji: "🌦️", accent: "💧", label: "阵雨", tone: "rain" },
  81: { emoji: "🌧️", accent: "💦", label: "强阵雨", tone: "rain" },
  82: { emoji: "⛈️", accent: "🌊", label: "暴雨", tone: "storm" },
  85: { emoji: "🌨️", accent: "❄️", label: "阵雪", tone: "snow" },
  86: { emoji: "❄️", accent: "🌬️", label: "强阵雪", tone: "snow" },
  95: { emoji: "⛈️", accent: "⚡", label: "雷暴", tone: "storm" },
  96: { emoji: "⛈️", accent: "🧊", label: "雷暴冰雹", tone: "storm" },
  99: { emoji: "⛈️", accent: "⚡", label: "强雷暴冰雹", tone: "storm" },
};

export default {
  name: "GlobeWeather",
  props: {
    speed: {
      type: Number,
      default: 0.003,
    },
  },
  data() {
    return {
      localLocation: null,
      weatherById: {},
      loading: true,
      lastSync: "",
      syncTimer: null,
    };
  },
  computed: {
    allLocations() {
      if (!this.localLocation) {
        return WEATHER_LOCATIONS;
      }

      return [
        this.localLocation,
        ...WEATHER_LOCATIONS.filter((item) => item.id !== this.localLocation.id),
      ];
    },
    weatherMarkers() {
      return this.allLocations.map((location) => {
        const weather = this.weatherById[location.id] || {};
        const codeInfo = weatherCodeMap[weather.weatherCode] || {
          emoji: "🌍",
          accent: "·",
          label: "同步中",
          tone: "cloudy",
        };
        const temperature = Number.isFinite(weather.temperature)
          ? Math.round(weather.temperature)
          : null;
        const temperatureAccent = this.getTemperatureAccent(temperature);

        return {
          ...location,
          emoji: codeInfo.emoji,
          accent: temperatureAccent || codeInfo.accent,
          condition: codeInfo.label,
          tone: codeInfo.tone,
          temperatureText: temperature === null ? "--°" : `${temperature}°`,
        };
      });
    },
    featuredWeather() {
      return this.weatherMarkers.slice(0, 4);
    },
    syncText() {
      if (this.loading) {
        return "同步中";
      }
      return this.lastSync ? `${this.lastSync} 更新` : "实时天气";
    },
  },
  mounted() {
    this.initRuntimeState();
    this.bootstrapWeather();
    window.addEventListener("resize", this.handleResize, { passive: true });
    window.addEventListener("pointermove", this.handlePointerMove, { passive: true });
    window.addEventListener("pointerup", this.handlePointerUp, { passive: true });
  },
  beforeDestroy() {
    window.clearInterval(this.syncTimer);
    window.removeEventListener("resize", this.handleResize);
    window.removeEventListener("pointermove", this.handlePointerMove);
    window.removeEventListener("pointerup", this.handlePointerUp);
    if (this._animationId) {
      cancelAnimationFrame(this._animationId);
    }
    if (this._globe) {
      this._globe.destroy();
    }
  },
  methods: {
    initRuntimeState() {
      this._globe = null;
      this._animationId = null;
      this._phi = 0;
      this._pointerStart = null;
      this._dragOffset = { phi: 0, theta: 0 };
      this._phiOffset = 0;
      this._thetaOffset = 0;
      this._paused = false;
    },
    async bootstrapWeather() {
      await this.resolveLocalLocation();
      await this.fetchWeather();
      this.$nextTick(this.initGlobe);
      this.syncTimer = window.setInterval(this.fetchWeather, 15 * 60 * 1000);
    },
    async resolveLocalLocation() {
      const providers = [
        {
          url: "https://ipapi.co/json/",
          normalize: (data) => ({
            city: data.city,
            region: data.region,
            country: data.country_name,
            latitude: data.latitude,
            longitude: data.longitude,
          }),
        },
        {
          url: "http://ip-api.com/json/?fields=status,country,regionName,city,lat,lon",
          normalize: (data) => {
            if (data && data.status !== "success") {
              return null;
            }
            return {
              city: data.city,
              region: data.regionName,
              country: data.country,
              latitude: data.lat,
              longitude: data.lon,
            };
          },
        },
      ];

      for (const provider of providers) {
        try {
          const response = await fetch(provider.url);
          if (!response.ok) {
            continue;
          }
          const data = await response.json();
          const location = provider.normalize(data);
          const latitude = Number(location?.latitude);
          const longitude = Number(location?.longitude);

          if (Number.isFinite(latitude) && Number.isFinite(longitude)) {
            const city = location.city || location.region || location.country || "当前位置";
            this.localLocation = {
              id: "weather-local",
              city,
              location: [latitude, longitude],
              isLocal: true,
            };
            return;
          }
        } catch (error) {
          console.warn("Failed to resolve local IP location:", error);
        }
      }
    },
    async fetchWeather() {
      this.loading = true;
      try {
        const result = await Promise.all(
          this.allLocations.map(async (marker) => {
            const url = new URL("https://api.open-meteo.com/v1/forecast");
            url.searchParams.set("latitude", marker.location[0]);
            url.searchParams.set("longitude", marker.location[1]);
            url.searchParams.set("current", "temperature_2m,weather_code");
            url.searchParams.set("timezone", "auto");

            const response = await fetch(url.toString());
            if (!response.ok) {
              throw new Error(`Weather request failed: ${response.status}`);
            }

            const data = await response.json();
            return [
              marker.id,
              {
                temperature: data.current?.temperature_2m,
                weatherCode: data.current?.weather_code,
              },
            ];
          })
        );

        this.weatherById = Object.fromEntries(result);
        this.lastSync = this.formatSyncTime(new Date());
      } catch (error) {
        console.error("Failed to fetch live weather:", error);
      } finally {
        this.loading = false;
      }
    },
    initGlobe() {
      const canvas = this.$refs.canvas;
      if (!canvas || this._globe || !canvas.offsetWidth) {
        return;
      }

      const width = canvas.offsetWidth;
      this._globe = createGlobe(canvas, {
        devicePixelRatio: Math.min(window.devicePixelRatio || 1, 2),
        width,
        height: width,
        phi: 0,
        theta: 0.2,
        dark: 0,
        diffuse: 1.45,
        mapSamples: 12000,
        mapBrightness: 9,
        baseColor: [0.98, 0.98, 1],
        markerColor: [0.35, 0.65, 0.95],
        glowColor: [0.94, 0.95, 0.98],
        markerElevation: 0.12,
        markers: this.allLocations.map((marker) => ({
          location: marker.location,
          size: 0.025,
          id: marker.id,
        })),
      });

      this.animateGlobe();
      window.setTimeout(() => {
        if (canvas) {
          canvas.style.opacity = "1";
        }
      }, 120);
    },
    animateGlobe() {
      if (!this._globe) {
        return;
      }

      if (!this._paused) {
        this._phi += this.speed;
      }

      this._globe.update({
        phi: this._phi + this._phiOffset + this._dragOffset.phi,
        theta: 0.2 + this._thetaOffset + this._dragOffset.theta,
      });
      this._animationId = requestAnimationFrame(this.animateGlobe);
    },
    handleResize() {
      if (!this.$refs.canvas || !this._globe) {
        return;
      }
      cancelAnimationFrame(this._animationId);
      this._globe.destroy();
      this._globe = null;
      this.$nextTick(this.initGlobe);
    },
    handlePointerDown(event) {
      this._pointerStart = { x: event.clientX, y: event.clientY };
      this._dragOffset = { phi: 0, theta: 0 };
      this._paused = true;
      event.currentTarget.style.cursor = "grabbing";
    },
    handlePointerMove(event) {
      if (!this._pointerStart) {
        return;
      }
      this._dragOffset = {
        phi: (event.clientX - this._pointerStart.x) / 300,
        theta: (event.clientY - this._pointerStart.y) / 1000,
      };
    },
    handlePointerUp() {
      if (!this._pointerStart) {
        return;
      }
      this._phiOffset += this._dragOffset.phi;
      this._thetaOffset += this._dragOffset.theta;
      this._pointerStart = null;
      this._dragOffset = { phi: 0, theta: 0 };
      this._paused = false;
      if (this.$refs.canvas) {
        this.$refs.canvas.style.cursor = "grab";
      }
    },
    getMarkerStyle(marker) {
      return {
        positionAnchor: `--cobe-${marker.id}`,
        opacity: `var(--cobe-visible-${marker.id}, 0)`,
      };
    },
    getTemperatureAccent(temperature) {
      if (!Number.isFinite(temperature)) {
        return "";
      }
      if (temperature >= 35) {
        return "🔥";
      }
      if (temperature >= 28) {
        return "🌡️";
      }
      if (temperature <= -5) {
        return "🧊";
      }
      if (temperature <= 3) {
        return "❄️";
      }
      return "";
    },
    formatSyncTime(date) {
      const pad = (value) => String(value).padStart(2, "0");
      return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.globe-weather {
  margin-top: 18px;
  padding: 16px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 50% 42%, rgba(59, 130, 246, 0.12), transparent 42%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(239, 246, 255, 0.86));
  border: 1px solid rgba(147, 197, 253, 0.22);
}

.globe-weather__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;

  h3 {
    margin: 6px 0 0;
    color: #0f172a;
    font-size: 18px;
    line-height: 1.25;
  }
}

.eyebrow {
  color: #2563eb;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.sync-state {
  flex-shrink: 0;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 11px;
  font-weight: 700;
}

.globe-weather__stage {
  position: relative;
  width: min(100%, 240px);
  aspect-ratio: 1;
  margin: 0 auto;
  user-select: none;
}

.globe-weather__canvas {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  cursor: grab;
  opacity: 0;
  touch-action: none;
  transition: opacity 1s ease;
}

.weather-marker {
  position: absolute;
  bottom: anchor(top);
  left: anchor(center);
  translate: -50% 0;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  pointer-events: none;
  filter:
    drop-shadow(0 0 2px #fff)
    drop-shadow(0 3px 9px rgba(37, 99, 235, 0.34));
  animation: weather-float 3s ease-in-out infinite;
}

.marker-bubble {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.14);
}

.marker-main {
  font-size: 22px;
  line-height: 1;
}

.marker-accent {
  position: absolute;
  right: -4px;
  bottom: -3px;
  font-size: 13px;
  line-height: 1;
}

.marker-temp {
  padding: 2px 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.tone-sunny .marker-bubble {
  background: linear-gradient(135deg, #fff7cc, #ffffff);
}

.tone-rain .marker-bubble {
  background: linear-gradient(135deg, #dbeafe, #ffffff);
}

.tone-storm .marker-bubble {
  background: linear-gradient(135deg, #e0e7ff, #ffffff);
}

.tone-snow .marker-bubble {
  background: linear-gradient(135deg, #eef6ff, #ffffff);
}

.tone-fog .marker-bubble,
.tone-cloudy .marker-bubble {
  background: linear-gradient(135deg, #f1f5f9, #ffffff);
}

.weather-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.weather-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 9px 10px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
}

.weather-item__icon {
  position: relative;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.12);

  span {
    font-size: 19px;
  }

  small {
    position: absolute;
    right: -3px;
    bottom: -3px;
    font-size: 11px;
  }
}

.weather-item__meta {
  min-width: 0;

  span,
  em {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    color: #0f172a;
    font-size: 12px;
    font-weight: 700;
  }

  em {
    color: #2563eb;
    font-size: 11px;
    font-style: normal;
  }
}

.weather-item strong {
  grid-column: 1 / -1;
  color: #0f172a;
  font-size: 17px;
}

@keyframes weather-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-4px);
  }
}
</style>
