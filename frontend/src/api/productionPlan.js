import api from './axios'

export function getProductionPlan(year) {
  return api.get(`/api/production-plan?year=${year}`).then((r) => r.data)
}

export function updateProductionPlanItem(id, data) {
  return api.put(`/api/production-plan/${id}`, data).then((r) => r.data)
}
