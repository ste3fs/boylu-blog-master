<template>
    <div class="random-video" :class="{ 'is-open': drawer }">
        <button
            class="random-video-btn"
            type="button"
            :aria-label="drawer ? '收起随机视频播放' : '展开随机视频播放'"
            :aria-expanded="drawer ? 'true' : 'false'"
            @click="toggleDrawer"
        >
            <i :class="drawer ? 'el-icon-d-arrow-left' : 'el-icon-d-arrow-right'"></i>
        </button>

        <transition name="random-video-slide">
            <aside v-if="drawer" class="random-video-panel" aria-label="随机视频播放">
                <div class="random-video-header">
                    <span>随机视频播放</span>
                    <button type="button" aria-label="关闭随机视频播放" @click="closeDrawer">
                        <i class="el-icon-close"></i>
                    </button>
                </div>

                <div class="video-container">
                    <video
                        ref="video"
                        class="random-video-player"
                        controls
                        playsinline
                        :src="videoSrc"
                        @play="syncPlaying(true)"
                        @pause="syncPlaying(false)"
                        @error="handleVideoError"
                    ></video>
                </div>

                <div class="random-video-actions">
                    <button type="button" class="video-action" @click="operateVideo">
                        <i :class="btnIcon"></i>
                        <span>{{ btnContent }}</span>
                    </button>
                    <button type="button" class="video-action video-action-primary" @click="nextVideo">
                        <i class="el-icon-arrow-right"></i>
                        <span>下一个视频</span>
                    </button>
                </div>

                <p v-if="videoError" class="random-video-error">{{ videoError }}</p>
            </aside>
        </transition>
    </div>
</template>

<script>
export default {
    name: 'RandomVideo',
    data() {
        return {
            drawer: false,
            baseVideoSrc: 'https://api.yujn.cn/api/zzxjj.php',
            videoSrc: 'https://api.yujn.cn/api/zzxjj.php',
            isPlaying: false,
            btnContent: '播放',
            btnIcon: 'el-icon-video-play',
            videoError: ''
        }
    },
    watch: {
        drawer(isOpen) {
            this.updatePageOffset(isOpen);
        }
    },
    beforeDestroy() {
        this.pauseVideo();
        this.updatePageOffset(false);
    },
    methods: {
        toggleDrawer() {
            if (this.drawer) {
                this.closeDrawer();
                return;
            }

            this.drawer = true;
            this.videoError = '';
            this.$nextTick(() => {
                this.playVideo();
            });
        },
        closeDrawer() {
            this.pauseVideo();
            this.drawer = false;
        },
        nextVideo() {
            this.videoError = '';
            this.videoSrc = `${this.baseVideoSrc}?temps=${Date.now()}`;
            this.$nextTick(() => {
                this.playVideo();
            });
        },
        operateVideo() {
            if (this.isPlaying) {
                this.pauseVideo();
            } else {
                this.playVideo();
            }
        },
        playVideo() {
            const video = this.$refs.video;
            if (!video) return;

            const playTask = video.play();
            if (playTask && typeof playTask.catch === 'function') {
                playTask.catch(() => {
                    this.syncPlaying(false);
                });
            }
        },
        pauseVideo() {
            const video = this.$refs.video;
            if (video && !video.paused) {
                video.pause();
            }
            this.syncPlaying(false);
        },
        syncPlaying(isPlaying) {
            this.isPlaying = isPlaying;
            this.btnContent = isPlaying ? '暂停' : '播放';
            this.btnIcon = isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play';
        },
        updatePageOffset(isOpen) {
            if (typeof document === 'undefined') {
                return;
            }
            document.body.classList.toggle('random-video-open', Boolean(isOpen));
        },
        handleVideoError() {
            this.videoError = '视频加载失败，请稍后再试或切换下一个视频';
            this.syncPlaying(false);
        }
    }
}
</script>

<style scoped lang="scss">
.random-video {
    position: fixed;
    left: 0;
    top: 50%;
    z-index: 1200;
    pointer-events: none;
}

.random-video-btn {
    position: fixed;
    left: 0;
    top: 50%;
    width: 30px;
    height: 56px;
    border: 0;
    border-radius: 0 18px 18px 0;
    background: rgba(82, 219, 238, 0.88);
    color: #fff;
    box-shadow: 0 10px 24px rgba(31, 175, 198, 0.24);
    transform: translateY(-50%);
    font-size: 1.5rem;
    cursor: pointer;
    pointer-events: auto;
    transition: background 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;

    &:hover {
        background: rgba(82, 219, 238, 1);
        box-shadow: 0 12px 28px rgba(31, 175, 198, 0.32);
        transform: translateY(-50%) translateX(2px);
    }
}

.random-video-panel {
    position: fixed;
    left: 18px;
    top: 78px;
    bottom: 26px;
    display: flex;
    flex-direction: column;
    width: calc(clamp(560px, 32vw, 680px) - 36px);
    max-width: calc(100vw - 54px);
    overflow: hidden;
    border: 1px solid rgba(120, 140, 170, 0.18);
    border-radius: 14px;
    background: var(--card-bg, rgba(255, 255, 255, 0.94));
    box-shadow: 0 18px 50px rgba(20, 31, 50, 0.18);
    backdrop-filter: blur(16px);
    transform: none;
    pointer-events: auto;
}

.random-video-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 44px;
    padding: 0 14px 0 16px;
    color: var(--font-color, #303133);
    font-size: 0.95rem;
    font-weight: 600;

    button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        border: 0;
        border-radius: 50%;
        background: transparent;
        color: var(--minor, #909399);
        cursor: pointer;

        &:hover {
            background: rgba(120, 140, 170, 0.12);
            color: var(--font-color, #303133);
        }
    }
}

.video-container {
    flex: 1 1 auto;
    min-height: 0;
    width: 100%;
    overflow: hidden;
    background: #05070a;
}

.random-video-player {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
}

.random-video-actions {
    display: flex;
    gap: 8px;
    padding: 12px 14px 14px;
}

.video-action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 86px;
    height: 34px;
    padding: 0 12px;
    border: 1px solid rgba(120, 140, 170, 0.2);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.72);
    color: var(--font-color, #303133);
    font-size: 0.86rem;
    cursor: pointer;
    transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

    i {
        margin-right: 5px;
    }

    &:hover {
        border-color: rgba(82, 219, 238, 0.55);
        color: $primary;
    }
}

.video-action-primary {
    background: rgba(82, 219, 238, 0.12);
    color: $primary;
}

.random-video-error {
    margin: -4px 14px 14px;
    color: #f56c6c;
    font-size: 0.78rem;
    line-height: 1.5;
}

.random-video-slide-enter-active,
.random-video-slide-leave-active {
    transition: opacity 0.2s ease, transform 0.2s ease;
}

.random-video-slide-enter,
.random-video-slide-leave-to {
    opacity: 0;
    transform: translateX(-16px);
}

@media (max-width: 1024px) {
    .random-video-btn {
        top: auto;
        bottom: 118px;
        transform: none;

        &:hover {
            transform: translateX(2px);
        }
    }

    .random-video-panel {
        left: 36px;
        top: auto;
        bottom: 92px;
        display: block;
        width: calc(100vw - 50px);
        max-width: calc(100vw - 50px);
        transform: none;
    }

    .video-container {
        aspect-ratio: 16 / 9;
    }

    .random-video-slide-enter,
    .random-video-slide-leave-to {
        transform: translateX(-16px);
    }
}
</style>

<style lang="scss">
@media (min-width: 1025px) {
    body.random-video-open {
        padding-left: clamp(560px, 32vw, 680px);
        transition: padding-left 0.2s ease;
    }
}

@media (max-width: 1024px) {
    body.random-video-open {
        padding-left: 0;
    }
}
</style>
