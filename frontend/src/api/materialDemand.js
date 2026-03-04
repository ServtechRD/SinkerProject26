import api from './axios'

export function getMaterialDemand(weekStart, factory) {
  return api
    .get('/api/material-demand', { params: { week_start: weekStart, factory } })
    .then((r) => r.data)
}

export function updateMaterialDemand(id, data) {
  return api.put(`/api/material-demand/${id}`, data).then((r) => r.data)
}

export function uploadMaterialDemand(file, weekStart, factory) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('week_start', weekStart)
  formData.append('factory', factory)
  return api
    .post('/api/material-demand/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    .then((r) => r.data)
}

export function downloadMaterialDemandTemplate(factory) {
  return api
    .get(`/api/material-demand/template/${encodeURIComponent(factory)}`, { responseType: 'blob' })
    .then((r) => {
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `material_demand_template_${factory}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}
