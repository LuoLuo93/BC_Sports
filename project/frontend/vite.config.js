import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { VantResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  base: '/bcsports/',
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver(), VantResolver()],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver(), VantResolver()],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5175,
    proxy: Object.fromEntries(
      // 只代理后端接口(REST + Shiro 登录登出),页面/模块/静态资源仍由本地 dev server 提供,
      // 整段代理 /bcsports 会把 dev 模块请求也转走,页面变成目标后端的旧构建。
      // 本地测试环境已下线:DEV_PROXY_TARGET 指向远程后端即可联调,如
      // DEV_PROXY_TARGET=http://192.168.5.180:9091 npm run dev
      ['/bcsports/api', '/bcsports/doLogin', '/bcsports/doLogout'].map(prefix => [
        prefix,
        {
          target: process.env.DEV_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          cookieDomainRewrite: { '*': '' }
        }
      ])
    )
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/element-plus')) return 'element-plus'
          if (id.includes('node_modules/vant')) return 'vant'
          if (id.includes('node_modules/echarts') || id.includes('node_modules/vue-echarts')) return 'echarts'
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) return 'vue-vendor'
        }
      }
    },
    chunkSizeWarningLimit: 500
  }
})
