import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: false
    }),
    Components({
      resolvers: [ElementPlusResolver()],
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
    proxy: {
      '/bcsports/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: { '*': '' }
      },
      '/bcsports/doLogin': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: { '*': '' }
      },
      '/bcsports/doLogout': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: { '*': '' }
      },
      '/bcsports/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: { '*': '' }
      },
      '/bcsports/images': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        cookieDomainRewrite: { '*': '' }
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: false,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/element-plus')) return 'element-plus'
          if (id.includes('node_modules/echarts') || id.includes('node_modules/vue-echarts')) return 'echarts'
          if (id.includes('node_modules/three')) return 'three'
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) return 'vue-vendor'
        }
      }
    },
    chunkSizeWarningLimit: 1000
  }
})
