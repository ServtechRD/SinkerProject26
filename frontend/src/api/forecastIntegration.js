import api from './axios'

export function getIntegrationVersions(month) {
  return api
    .get('/api/sales-forecast/integration/versions', {
      params: { month },
    })
    .then((r) => r.data)
}

export function getIntegrationData(month, version) {
  return api
    .get('/api/sales-forecast/integration', {
      params: { month, version },
    })
    .then((r) => r.data)
}

export function exportIntegrationExcel(month, version) {
  return api
    .get('/api/sales-forecast/integration/export', {
      params: { month, version },
      responseType: 'blob',
    })
    .then((r) => {
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').substring(0, 19)
      link.download = `sales_forecast_integration_${month}_${timestamp}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}
