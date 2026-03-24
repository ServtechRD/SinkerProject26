export class MainLayout {
  constructor(page) {
    this.page = page
    this.sidebar = page.getByTestId('sidebar')
    this.usersLink = page.getByRole('link', { name: '使用者管理' })
    this.forecastConfigLink = page.getByRole('link', { name: '銷售預估量-表單設定' })
    this.forecastUploadLink = page.getByRole('link', { name: '銷售預估量表單上傳', exact: true })
    this.productionPlanLink = page.getByRole('link', { name: '生產表單' })
    this.logoutButton = page.getByRole('button', { name: '登出' })
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

  async clickProductionPlan() {
    await this.productionPlanLink.click()
  }

  async clickLogout() {
    await this.logoutButton.click()
  }

  async isSidebarVisible() {
    return this.sidebar.isVisible()
  }
}
