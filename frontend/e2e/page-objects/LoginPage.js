export class LoginPage {
  constructor(page) {
    this.page = page
    this.usernameInput = page.getByLabel('Username')
    this.passwordInput = page.getByLabel('Password')
    this.submitButton = page.getByRole('button', { name: /login/i })
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
