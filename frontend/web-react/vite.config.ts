import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'Referrer-Policy': 'no-referrer',
      'X-Frame-Options': 'DENY',
    },
    port: 5173,
    proxy: {
      '/api/v1/ia': {
        target: 'http://127.0.0.1:8000',
        changeOrigin: true,
      },
      '/api/admin': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: false,
      },
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: false,
      },
    },
    watch: {
      usePolling: true,
      interval: 300,
    },
  },
})
