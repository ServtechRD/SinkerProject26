export class MainLayout {
  constructor(page) {
    this.page = page
    this.sidebar = page.getByTestId('sidebar')
    this.dashboardLink = page.getByRole('link', { name: 'Dashboard' })
    this.usersLink = page.getByRole('link', { name: 'Users' })
    this.forecastConfigLink = page.getByRole('link', { name: 'Forecast Config' })
    this.forecastUploadLink = page.getByRole('link', { name: 'Forecast Upload' })
    this.logoutButton = page.getByRole('button', { name: /logout/i })
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
