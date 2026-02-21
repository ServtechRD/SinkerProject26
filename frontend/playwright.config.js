import { defineConfig } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [['html', { open: 'never' }]],
  globalSetup: './e2e/global-setup.js',
  use: {
    baseURL,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
  ...(process.env.E2E_NO_SERVER
    ? {}
    : {
        webServer: {
          command: 'npm run dev',
          port: 5173,
          reuseExistingServer: true,
          timeout: 30000,
        },
      }),
})
