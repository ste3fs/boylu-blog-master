import { defineConfig } from 'vite'
import vue2 from '@vitejs/plugin-vue2'
import path from 'path'
import { loadEnv } from 'vite'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'

export default defineConfig(({ command, mode }) => {

    const env = loadEnv(mode, process.cwd());
    return {
        server: {
            // 允许IP访问
            host: "0.0.0.0",
            // 应用端口 (默认:3000)
            port: Number(env.VITE_APP_PORT),
            // 运行是否自动打开浏览器
            open: false,
            proxy: {
              /** 代理前缀为 /dev-api 的请求  */
              [env.VITE_APP_BASE_API]: {
                changeOrigin: true,
                // 接口地址
                target: env.VITE_APP_API_URL,
                rewrite: (path) =>
                  path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
              },
              '/localFile': {
                changeOrigin: true,
                target: env.VITE_APP_API_URL,
              },
              '/img': {
                changeOrigin: true,
                target: env.VITE_APP_API_URL,
              },
            },
        },
        plugins: [
          vue2(),
          createSvgIconsPlugin({
            // 指定需要缓存的图标文件夹
            iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
            // 指定symbolId格式
            symbolId: 'icon-[dir]-[name]',
          }),
        ],
        resolve: {
          alias: {
            '@': path.resolve(__dirname, './src')
          }
        },
        build: {
          rollupOptions: {
            output: {
              manualChunks(id) {
                if (!id.includes('node_modules')) {
                  return
                }

                if (id.includes('element-ui')) {
                  return 'vendor-element'
                }

                if (
                  id.includes('/vue/') ||
                  id.includes('/vue-router/') ||
                  id.includes('/vuex/')
                ) {
                  return 'vendor-vue'
                }

                if (id.includes('marked') || id.includes('highlight.js')) {
                  return 'vendor-markdown'
                }

                if (id.includes('gsap') || id.includes('animate.css')) {
                  return 'vendor-motion'
                }

                if (
                  id.includes('vue-cropper') ||
                  id.includes('vue-danmaku') ||
                  id.includes('vue-lazyload')
                ) {
                  return 'vendor-media'
                }

                if (id.includes('axios')) {
                  return 'vendor-request'
                }

                return 'vendor'
              }
            }
          }
        },
        css: {
            preprocessorOptions: {
              scss: {
                api: 'modern-compiler',
                additionalData: `
                  @use "@/styles/variables.scss" as *;
                  @use "@/styles/mixins.scss" as *;
                `
              }
            }
        }
    }
  
})
