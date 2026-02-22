import api from './axios'

export function getMaterialPurchase(weekStart, factory) {
  return api.get(`/api/material-purchase?week_start=${weekStart}&factory=${factory}`).then((r) => r.data)
}

export function triggerErp(id) {
  return api.post(`/api/material-purchase/${id}/trigger-erp`).then((r) => r.data)
}
