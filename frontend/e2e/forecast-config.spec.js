import { test, expect } from '@playwright/test'
import { LoginPage } from './page-objects/LoginPage.js'
import { MainLayout } from './page-objects/MainLayout.js'

test.describe('Forecast Config Page', () => {
  let loginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    await page.context().clearCookies()
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
  })

  test('full workflow - navigate, view table, create months, edit config', async ({ page }) => {
    const layout = new MainLayout(page)

    await loginPage.navigate()
    await loginPage.login('admin', 'admin123')
    await page.waitForURL('**/')

    await layout.clickForecastConfig()
    await page.waitForURL('**/sales-forecast/config')

    await expect(page.getByRole('heading', { name: '預測設定' })).toBeVisible()

    // Wait for loading to finish
    await expect(page.getByText('載入中...')).not.toBeVisible({ timeout: 10000 })

    // Page should show either the table or the empty state
    const hasTable = await page.locator('.forecast-table').isVisible().catch(() => false)
    const hasEmpty = await page.getByText('尚無設定資料，請建立第一個月份。').isVisible().catch(() => false)
    expect(hasTable || hasEmpty).toBeTruthy()

    // Create months
    const createBtn = page.getByRole('button', { name: '建立月份' })
    await expect(createBtn).toBeVisible()
    await createBtn.click()

    await expect(page.getByText('建立月份').last()).toBeVisible()

    const startInput = page.getByLabel('起始月份')
    const endInput = page.getByLabel('結束月份')
    await startInput.fill('202701')
    await endInput.fill('202703')

    const submitBtn = page.getByRole('button', { name: '建立' })
    await expect(submitBtn).toBeEnabled()
    await submitBtn.click()

    // Wait for dialog to close and table to refresh
    await expect(page.getByLabel('起始月份')).not.toBeVisible({ timeout: 10000 })

    // Verify rows in table
    await expect(page.getByText('202701')).toBeVisible()
    await expect(page.getByText('202702')).toBeVisible()
    await expect(page.getByText('202703')).toBeVisible()

    // Edit the first month
    const editBtns = page.getByRole('button', { name: '編輯' })
    await editBtns.first().click()

    await expect(page.getByText('編輯設定')).toBeVisible()

    const dayInput = page.getByLabel('自動關帳日')
    await dayInput.clear()
    await dayInput.fill('25')

    const saveBtn = page.getByRole('button', { name: '儲存' })
    await expect(saveBtn).toBeEnabled()
    await saveBtn.click()

    // Wait for dialog to close
    await expect(page.getByText('編輯設定')).not.toBeVisible({ timeout: 10000 })
  })

  test('validation - create button disabled when start > end', async ({ page }) => {
    const layout = new MainLayout(page)

    await loginPage.navigate()
    await loginPage.login('admin', 'admin123')
    await page.waitForURL('**/')

    await layout.clickForecastConfig()
    await page.waitForURL('**/sales-forecast/config')
    await expect(page.getByText('載入中...')).not.toBeVisible({ timeout: 10000 })

    await page.getByRole('button', { name: '建立月份' }).click()
    await expect(page.getByText('建立月份').last()).toBeVisible()

    await page.getByLabel('起始月份').fill('202705')
    await page.getByLabel('結束月份').fill('202703')

    await expect(page.getByRole('button', { name: '建立' })).toBeDisabled()

    // Cancel
    await page.getByRole('button', { name: '取消' }).click()
    await expect(page.getByLabel('起始月份')).not.toBeVisible()
  })

  test('validation - edit auto_close_day range', async ({ page }) => {
    const layout = new MainLayout(page)

    await loginPage.navigate()
    await loginPage.login('admin', 'admin123')
    await page.waitForURL('**/')

    await layout.clickForecastConfig()
    await page.waitForURL('**/sales-forecast/config')
    await expect(page.getByText('載入中...')).not.toBeVisible({ timeout: 10000 })

    // Skip if no data to edit
    const editBtns = page.getByRole('button', { name: '編輯' })
    const editCount = await editBtns.count()
    if (editCount === 0) {
      test.skip()
      return
    }

    await editBtns.first().click()
    await expect(page.getByText('編輯設定')).toBeVisible()

    const dayInput = page.getByLabel('自動關帳日')
    await dayInput.clear()
    await dayInput.fill('50')

    await expect(page.getByText('自動關帳日需為 1-31')).toBeVisible()
    await expect(page.getByRole('button', { name: '儲存' })).toBeDisabled()

    await page.getByRole('button', { name: '取消' }).click()
  })
})
