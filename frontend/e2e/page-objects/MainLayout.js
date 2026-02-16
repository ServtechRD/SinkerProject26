export class MainLayout {
  constructor(page) {
    this.page = page
    this.sidebar = page.getByTestId('sidebar')
    this.dashboardLink = page.getByRole('link', { name: '儀表板' })
    this.usersLink = page.getByRole('link', { name: '使用者' })
    this.forecastConfigLink = page.getByRole('link', { name: '預測設定' })
    this.forecastUploadLink = page.getByRole('link', { name: '預測上傳' })
    this.logoutButton = page.getByRole('button', { name: '登出' })
  }

  async clickDashboard() {
    await this.dashboardLink.click()
  }

  async clickUsers() {
    await this.usersLink.click()
  }

  async clickForecastConfig() {
    await this.forecastConfigLink.click()
  }

  async clickForecastUpload() {
    await this.forecastUploadLink.click()
  }

  async clickLogout() {
    await this.logoutButton.click()
  }

  async isSidebarVisible() {
    return this.sidebar.isVisible()
  }
}
