import api from './axios'

export function uploadGiftForecast(file, month, channel) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('month', month)
  formData.append('channel', channel)

  return api
    .post('/api/gift-sales-forecast/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((r) => r.data)
}

export function downloadGiftTemplate(channel) {
  return api
    .get('/api/gift-sales-forecast/template', {
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
      link.download = `gift_sales_forecast_template_${safeName}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}

export function getGiftForecastVersions(month, channel) {
  return api
    .get('/api/gift-sales-forecast/versions', {
      params: { month, channel },
    })
    .then((r) => r.data)
}

export function getGiftForecastList(month, channel, version) {
  return api
    .get('/api/gift-sales-forecast', {
      params: { month, channel, version: version || undefined },
    })
    .then((r) => r.data)
}

export function copyGiftVersion(month, channel) {
  return api
    .post('/api/gift-sales-forecast/copy-version', null, {
      params: { month, channel },
    })
    .then((r) => r.data)
}

export function saveGiftVersionReason(month, channel, version, changeReason) {
  return api
    .put('/api/gift-sales-forecast/versions/reason', { changeReason }, {
      params: { month, channel, version },
    })
    .then(() => {})
}

export function deleteGiftVersion(month, channel, version) {
  return api
    .delete('/api/gift-sales-forecast/versions', {
      params: { month, channel, version },
    })
    .then(() => {})
}

export function getGiftVersionDiff(month, channel, version) {
  return api
    .get('/api/gift-sales-forecast/versions/diff', {
      params: { month, channel, version },
    })
    .then((r) => r.data)
}

export function createGiftForecastItem(data) {
  return api.post('/api/gift-sales-forecast', data).then((r) => r.data)
}

export function updateGiftForecastItem(id, data) {
  return api.put(`/api/gift-sales-forecast/${id}`, data).then((r) => r.data)
}

export function deleteGiftForecastItem(id) {
  return api.delete(`/api/gift-sales-forecast/${id}`).then((r) => r.data)
}
