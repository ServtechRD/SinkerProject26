import api from './axios'

export function listConfigs() {
  return api.get('/api/sales-forecast/config').then((r) => r.data)
}

export function createMonth(month, autoCloseAt) {
  return api
    .post('/api/sales-forecast/config', { month, autoCloseAt })
    .then((r) => r.data)
}

export function updateConfig(id, data) {
  return api.put(`/api/sales-forecast/config/${id}`, data).then((r) => r.data)
}

export function getFormDetail(id) {
  return api.get(`/api/sales-forecast/config/${id}/detail`).then((r) => r.data)
}

export function getFormLogs(id) {
  return api.get(`/api/sales-forecast/config/${id}/logs`).then((r) => r.data)
}

export function reopenChannels(id, payload) {
  return api.post(`/api/sales-forecast/config/${id}/reopen`, payload).then((r) => r.data)
}

export function endReopen(id, windowId) {
  return api.post(`/api/sales-forecast/config/${id}/reopen/${windowId}/end`).then((r) => r.data)
}

export function getEditStatus(month, channel) {
  return api
    .get('/api/sales-forecast/config/edit-status', { params: { month, channel } })
    .then((r) => r.data)
}
