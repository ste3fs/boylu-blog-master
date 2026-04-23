import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { ConfigEnv, UserConfig, loadEnv } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { svgBuilder } from './src/plugins/svgBuilder'

function normalizeBasePath(pathValue: string) {
  if (!pathValue) {
    return '/boylu1107/'
  }
  const withLeadingSlash = pathValue.startsWith('/') ? pathValue : `/${pathValue}`
  return withLeadingSlash.endsWith('/') ? withLeadingSlash : `${withLeadingSlash}/`
}

function splitVendorChunk(id: string) {
  if (!id.includes('node_modules')) {
    return undefined
  }

  const normalizedId = id.replace(/\\/g, '/')

  if (normalizedId.includes('/element-plus/') || normalizedId.includes('/@element-plus/')) {
    return 'vendor-element'
  }
  if (
    normalizedId.includes('/vue/') ||
    normalizedId.includes('/@vue/') ||
    normalizedId.includes('/vue-router/') ||
    normalizedId.includes('/pinia/')
  ) {
    return 'vendor-vue'
  }
  if (normalizedId.includes('/echarts/')) {
    return 'vendor-echarts'
  }
  if (normalizedId.includes('/@wangeditor/')) {
    return 'vendor-rich-editor'
  }
  if (normalizedId.includes('/mavon-editor/')) {
    return 'vendor-markdown-editor'
  }
  if (normalizedId.includes('/highlight.js/')) {
    return 'vendor-highlight'
  }
  if (normalizedId.includes('/axios/')) {
    return 'vendor-request'
  }
  if (normalizedId.includes('/cron-parser/')) {
    return 'vendor-cron'
  }
  if (
    normalizedId.includes('/screenfull/') ||
    normalizedId.includes('/nprogress/') ||
    normalizedId.includes('/file-saver/') ||
    normalizedId.includes('/js-cookie/')
  ) {
    return 'vendor-utils'
  }

  return 'vendor'
}

export default defineConfig(({ command, mode }: ConfigEnv): UserConfig => {
  // 获取环境变量
  const env = loadEnv(mode, process.cwd())
  const proxyDebug = env.VITE_PROXY_DEBUG === 'true'
  const adminBasePath = mode === 'production'
    ? normalizeBasePath(env.VITE_APP_ADMIN_BASE_PATH || '/boylu1107/')
    : '/'
  
  return {
    base: adminBasePath,
    css: {
      preprocessorOptions: {
        scss: {
          charset: false
        },
      },
    },
    plugins: [
      vue(),
      svgBuilder('./src/icons/svg/'),
      AutoImport({
        imports: [
          'vue',
          'vue-router',
          'pinia'
        ],
        dts: 'src/auto-imports.d.ts',
        // 可以选择是否自动导入 Vue 的组合式 API
        vueTemplate: true,
        // 自动导入 Pinia store，目录名与 src/store 保持一致
        dirs: [
          './src/store/modules'
        ],
        resolvers: [
          ElementPlusResolver({
            importStyle: 'css'
          })
        ]
      }),
      Components({
        dts: 'src/components.d.ts',
        directives: true,
        resolvers: [
          ElementPlusResolver({
            importStyle: 'css'
          })
        ]
      })
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src')
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: splitVendorChunk
        }
      }
    },
    server: {
      host: '0.0.0.0',
      port: Number(env.VITE_APP_PORT) || 3000,
      open: false,
      proxy: {
        '/api': {
          target: env.VITE_APP_API_URL,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          configure: (proxy, options) => {
            proxy.on('proxyReq', (proxyReq, req, res) => {
              if (proxyDebug) {
                console.log('代理请求:', {
                  target: options.target,
                  path: req.url
                })
              }
            })
          }
        }
      }
    }
  }
})
