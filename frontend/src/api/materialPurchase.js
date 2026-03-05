import api from './axios'

export function getMaterialPurchase(weekStart, factory) {
  return api
    .get('/api/material-purchase', { params: { week_start: weekStart, factory } })
    .then((r) => r.data)
}

export function updateMaterialPurchase(id, data) {
  return api.put(`/api/material-purchase/${id}`, data).then((r) => r.data)
}

export function uploadMaterialPurchase(file, weekStart, factory) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('week_start', weekStart)
  formData.append('factory', factory)
  return api
    .post('/api/material-purchase/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
}

export function downloadMaterialPurchaseTemplate(factory) {
  return api
    .get(`/api/material-purchase/template/${encodeURIComponent(factory)}`, { responseType: 'blob' })
    .then((r) => {
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `material_purchase_template_${factory}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}

export function triggerErp(id) {
  return api.post(`/api/material-purchase/${id}/trigger-erp`).then((r) => r.data)
}
