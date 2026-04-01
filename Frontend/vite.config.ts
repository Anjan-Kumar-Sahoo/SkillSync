import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiUrl = env.VITE_API_URL || 'http://35.153.59.2:8080'
  const targetOrigin = apiUrl ? new URL(apiUrl).origin : undefined

  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: targetOrigin
        ? {
            '/api': {
              target: targetOrigin,
              changeOrigin: true
            },
            '/ws': {
              target: targetOrigin,
              ws: true
            }
          }
        : undefined
    }
  }
})
