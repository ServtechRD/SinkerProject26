import api from './axios'

export function getInventoryIntegration(month, startDate, endDate, version) {
  const params = { month }
  if (startDate) params.startDate = startDate
  if (endDate) params.endDate = endDate
  if (version) params.version = version

  return api
    .get('/api/inventory-integration', { params })
    .then((r) => r.data)
}

export function updateModifiedSubtotal(id, modifiedSubtotal) {
  return api
    .put(`/api/inventory-integration/${id}`, { modifiedSubtotal })
    .then((r) => r.data)
}
