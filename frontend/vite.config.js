import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from "node:path";

const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react({
      babel: {
        plugins: [['babel-plugin-react-compiler']],
      },
    }),
  ],
  server: {
    host: '0.0.0.0',
    port: 5173,
    // Allow remote VM access (bypasses Vite DNS rebinding protection for dev)
    allowedHosts: true,
    proxy: {
      '/api': {
        target: proxyTarget,
        changeOrigin: true,
      },
      '/swagger-ui': {
        target: proxyTarget,
        changeOrigin: true,
      },
      '/v3/api-docs': {
        target: proxyTarget,
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    exclude: ['e2e/**', 'node_modules/**'],
    coverage: {
      provider: "v8",
      reporter: ["text", "html", "lcov"],
      reportsDirectory: "coverage",
      thresholds: {
        lines: 70,
        branches: 70,     // ⭐ 目標：branches >= 70
        functions: 70,
        statements: 70,
      },
    },
  },
})
