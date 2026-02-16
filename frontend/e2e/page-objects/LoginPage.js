export class LoginPage {
  constructor(page) {
    this.page = page
    this.usernameInput = page.getByLabel('帳號')
    this.passwordInput = page.getByLabel('密碼')
    this.submitButton = page.getByRole('button', { name: '登入' })
    this.errorMessage = page.getByRole('alert')
  }

  async navigate() {
    await this.page.goto('/login')
  }

  async fillUsername(username) {
    await this.usernameInput.fill(username)
  }

  async fillPassword(password) {
    await this.passwordInput.fill(password)
  }

  async clickSubmit() {
    await this.submitButton.click()
  }

  async login(username, password) {
    await this.fillUsername(username)
    await this.fillPassword(password)
    await this.clickSubmit()
  }

  async getErrorMessage() {
    return this.errorMessage.textContent()
  }

  async isOnLoginPage() {
    return this.page.url().includes('/login')
  }
}
