import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const backendTarget = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // No rewrite: backend controllers are mapped at /api/** themselves
      // (e.g. @RequestMapping("/api/environments")), so the prefix must
      // reach the backend unchanged.
      '/api': {
        target: backendTarget,
        changeOrigin: true,
      },
    },
  },
})
