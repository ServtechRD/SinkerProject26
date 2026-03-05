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
    .get('/api/sales-forecast/template', {
      params: { channel },
      responseType: 'blob',
    })
    .then((r) => {
      const contentType = (r.headers?.['content-type'] || '').toLowerCase()
      if (contentType.includes('application/json')) {
        return r.data.text().then((text) => {
          let msg = '下載範本失敗'
          try {
            const body = JSON.parse(text)
            if (Array.isArray(body.details)) msg = body.details.join('; ')
            else if (body.message) msg = body.message
          } catch (_) {}
          throw new Error(msg)
        })
      }
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const safeName = (channel || 'channel').replace(/\//g, '_')
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `sales_forecast_template_${safeName}.xlsx`
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
      params: { month, channel, version: version || undefined },
    })
    .then((r) => r.data)
}

export function copyVersion(month, channel) {
  return api
    .post('/api/sales-forecast/copy-version', null, {
      params: { month, channel },
    })
    .then((r) => r.data)
}

export function saveVersionReason(month, channel, version, changeReason) {
  return api
    .put('/api/sales-forecast/versions/reason', { changeReason }, {
      params: { month, channel, version },
    })
    .then(() => {})
}

export function deleteVersion(month, channel, version) {
  return api
    .delete('/api/sales-forecast/versions', {
      params: { month, channel, version },
    })
    .then(() => {})
}

export function getVersionDiff(month, channel, version) {
  return api
    .get('/api/sales-forecast/versions/diff', {
      params: { month, channel, version },
    })
    .then((r) => r.data)
}

export function getFormSummary(month) {
  return api.get('/api/sales-forecast/form-summary', { params: { month } }).then((r) => r.data)
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
