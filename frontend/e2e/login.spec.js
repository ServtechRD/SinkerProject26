import { test, expect } from '@playwright/test'
import { LoginPage } from './page-objects/LoginPage.js'
import { MainLayout } from './page-objects/MainLayout.js'

test.describe('Login Flow', () => {
  let loginPage

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page)
    await page.context().clearCookies()
    await page.goto('/')
    await page.evaluate(() => localStorage.clear())
    await loginPage.navigate()
  })

  test('successful login redirects to dashboard', async ({ page }) => {
    const layout = new MainLayout(page)

    await loginPage.login('admin', 'admin123')
    await page.waitForURL('**/')

    expect(page.url()).not.toContain('/login')
    await expect(layout.sidebar).toBeVisible()

    const token = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(token).toBeTruthy()
  })

  test('invalid username shows error', async ({ page }) => {
    await loginPage.login('nonexistent', 'password')

    await expect(loginPage.errorMessage).toBeVisible()
    const errorText = await loginPage.getErrorMessage()
    expect(errorText).toContain('Invalid username or password')

    expect(page.url()).toContain('/login')

    const token = await page.evaluate(() => localStorage.getItem('authToken'))
    expect(token).toBeNull()
  })

  test('invalid password shows error', async ({ page }) => {
    await loginPage.login('admin', 'wrongpassword')

    await expect(loginPage.errorMessage).toBeVisible()
    const errorText = await loginPage.getErrorMessage()
    expect(errorText).toContain('Invalid username or password')

    expect(page.url()).toContain('/login')
  })

  test('locked account shows error', async ({ page }) => {
    await loginPage.login('locked_user', 'password')

    await expect(loginPage.errorMessage).toBeVisible()
    const errorText = await loginPage.getErrorMessage()
    expect(errorText.toLowerCase()).toContain('locked')

    expect(page.url()).toContain('/login')
  })

  test('inactive account shows error', async ({ page }) => {
    await loginPage.login('inactive_user', 'password')

    await expect(loginPage.errorMessage).toBeVisible()
    const errorText = await loginPage.getErrorMessage()
    expect(errorText.toLowerCase()).toContain('inactive')

    expect(page.url()).toContain('/login')
  })

  test('protected route redirects unauthenticated user to login', async ({ page }) => {
    await page.evaluate(() => localStorage.clear())
    await page.goto('/users')

    await page.waitForURL('**/login')
    expect(page.url()).toContain('/login')
    await expect(loginPage.usernameInput).toBeVisible()
  })

  test('empty form shows validation error', async ({ page }) => {
    await loginPage.clickSubmit()

    await expect(loginPage.errorMessage).toBeVisible()
    expect(page.url()).toContain('/login')
  })
})
