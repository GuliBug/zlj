import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 10086,
    strictPort: true,
    host: true, // 监听所有网卡，局域网内其他人可通过 http://<本机IP>:10086 访问
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
