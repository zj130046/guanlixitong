import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 3002,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log(`[Proxy] ${req.method} ${req.url} -> ${options.target}${req.url}`)
          })
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log(`[Proxy Response] ${req.method} ${req.url} -> ${proxyRes.statusCode}`)
          })
          proxy.on('error', (err, req, res) => {
            console.error(`[Proxy Error] ${req.method} ${req.url}:`, err.message)
          })
        }
      }
    }
  }
})

