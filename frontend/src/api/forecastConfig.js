import api from './axios'

export function listConfigs() {
  return api.get('/api/sales-forecast/config').then((r) => r.data)
}

export function createMonth(month, autoCloseDate) {
  return api
    .post('/api/sales-forecast/config', { month, autoCloseDate })
    .then((r) => r.data)
}

export function updateConfig(id, data) {
  return api.put(`/api/sales-forecast/config/${id}`, data).then((r) => r.data)
}
