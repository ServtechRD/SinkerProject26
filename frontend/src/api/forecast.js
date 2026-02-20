import api from './axios'

export function uploadForecast(file, month, channel) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('month', month)
  formData.append('channel', channel)

  return api
    .post('/api/sales-forecast/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((r) => r.data)
}

export function downloadTemplate(channel) {
  return api
    .get(`/api/sales-forecast/template/${channel}`, {
      responseType: 'blob',
    })
    .then((r) => {
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `sales_forecast_template_${channel}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}

export function getForecastVersions(month, channel) {
  return api
    .get('/api/sales-forecast/versions', {
      params: { month, channel },
    })
    .then((r) => r.data)
}

export function getForecastList(month, channel, version) {
  return api
    .get('/api/sales-forecast', {
      params: { month, channel, version },
    })
    .then((r) => r.data)
}

export function createForecastItem(data) {
  return api.post('/api/sales-forecast', data).then((r) => r.data)
}

export function updateForecastItem(id, data) {
  return api.put(`/api/sales-forecast/${id}`, data).then((r) => r.data)
}

export function deleteForecastItem(id) {
  return api.delete(`/api/sales-forecast/${id}`).then((r) => r.data)
}
