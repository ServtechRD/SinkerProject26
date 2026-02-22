import { test, expect } from '@playwright/test'
import { LoginPage } from './page-objects/LoginPage.js'
import { MainLayout } from './page-objects/MainLayout.js'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())

    const loginPage = new LoginPage(page)
    await loginPage.navigate()
    await loginPage.login('admin', 'admin123')
    await page.waitForURL('**/')
  })

  test('sidebar navigation to Users', async ({ page }) => {
    const layout = new MainLayout(page)

    await layout.clickUsers()
    await page.waitForURL('**/users')

    expect(page.url()).toContain('/users')
    await expect(layout.usersLink).toHaveClass(/sidebar-link--active/)
    await expect(page.getByText('使用者管理')).toBeVisible()
  })

  test('sidebar navigation to Forecast Config', async ({ page }) => {
    const layout = new MainLayout(page)

    await layout.clickForecastConfig()
    await page.waitForURL('**/sales-forecast/config')

    expect(page.url()).toContain('/sales-forecast/config')
    await expect(layout.forecastConfigLink).toHaveClass(/sidebar-link--active/)
  })

  test('sidebar navigation to Forecast Upload', async ({ page }) => {
    const layout = new MainLayout(page)

    await layout.clickForecastUpload()
    await page.waitForURL('**/sales-forecast/upload')

    expect(page.url()).toContain('/sales-forecast/upload')
    await expect(layout.forecastUploadLink).toHaveClass(/sidebar-link--active/)
  })

  test('sidebar navigation to Production Plan', async ({ page }) => {
    const layout = new MainLayout(page)

    await layout.clickProductionPlan()
    await page.waitForURL('**/production-plan')

    expect(page.url()).toContain('/production-plan')
    await expect(layout.productionPlanLink).toHaveClass(/sidebar-link--active/)
    await expect(page.getByRole('heading', { name: /生產計畫/ })).toBeVisible()
  })

  test('sidebar navigation between pages', async ({ page }) => {
    const layout = new MainLayout(page)

    await layout.clickUsers()
    await page.waitForURL('**/users')
    await expect(layout.usersLink).toHaveClass(/sidebar-link--active/)

    await layout.clickProductionPlan()
    await page.waitForURL('**/production-plan')
    await expect(layout.productionPlanLink).toHaveClass(/sidebar-link--active/)
    await expect(page.getByRole('heading', { name: /生產計畫/ })).toBeVisible()
  })

  test('logout clears auth and redirects to login', async ({ page }) => {
    const layout = new MainLayout(page)

    const tokenBefore = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(tokenBefore).toBeTruthy()

    await layout.clickLogout()
    await page.waitForURL('**/login')

    expect(page.url()).toContain('/login')

    const tokenAfter = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(tokenAfter).toBeNull()

    await page.goto('/users')
    await page.waitForURL('**/login')
    expect(page.url()).toContain('/login')
  })

  test('token persists on page refresh', async ({ page }) => {
    const layout = new MainLayout(page)

    await expect(layout.sidebar).toBeVisible()
    const tokenBefore = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(tokenBefore).toBeTruthy()

    await page.reload()
    await page.waitForLoadState('networkidle')

    expect(page.url()).not.toContain('/login')
    await expect(layout.sidebar).toBeVisible()

    const tokenAfter = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(tokenAfter).toBeTruthy()

    await layout.clickUsers()
    await page.waitForURL('**/users')
    expect(page.url()).toContain('/users')
  })
})
