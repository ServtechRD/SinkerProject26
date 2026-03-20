import api from './axios'

/**
 * 將月份參數轉成 YYYYMM 字串，避免誤傳物件時被序列化為 [object Object]。
 */
function toMonthString(month) {
  if (month == null) return ''
  if (typeof month === 'string' || typeof month === 'number') return String(month)
  if (typeof month === 'object' && month !== null && month.month != null) {
    return String(month.month)
  }
  return String(month)
}

/**
 * 將版本號轉成數字；若為 { version_no } 等物件則取出再轉。
 */
function toVersionNoNumber(versionNo) {
  if (versionNo == null || versionNo === '') return undefined
  if (typeof versionNo === 'number') {
    if (Number.isNaN(versionNo) || !Number.isFinite(versionNo)) return undefined
    return versionNo
  }
  if (typeof versionNo === 'string') {
    const n = parseInt(versionNo, 10)
    return Number.isNaN(n) ? undefined : n
  }
  if (typeof versionNo === 'object' && versionNo !== null) {
    const v = versionNo.version_no ?? versionNo.versionNo
    if (v != null) return toVersionNoNumber(v)
  }
  return undefined
}

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

/**
 * @param {string|number|object} month YYYYMM
 * @param {number|null|undefined} versionNo 表單版本（必須帶正整數，關帳後查表單摘要必用，否則後端會走舊版彙總且 version_no 為 null）
 */
export function getFormSummary(month, versionNo = null) {
  const params = { month: toMonthString(month) }
  const vn = toVersionNoNumber(versionNo)
  // 僅在能解析出有效數字時附帶；若漏帶 version_no，後端會當成「未指定版本」走 legacy
  if (vn !== undefined && vn !== null) params.version_no = vn
  return api.get('/api/sales-forecast/form-summary', { params }).then((r) => r.data)
}

export function getFormVersions(month) {
  return api
    .get('/api/sales-forecast/form-versions', { params: { month: toMonthString(month) } })
    .then((r) => r.data)
}

export function saveFormSummaryVersion(month, payload) {
  return api
    .post('/api/sales-forecast/form-summary/save-version', payload, {
      params: { month: toMonthString(month) },
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
