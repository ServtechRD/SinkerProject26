import api from './axios'

export function getProductionPlan(year) {
  return api.get(`/api/production-plan`, { params: { year } }).then((r) => r.data)
}

export function getProductionPlanVersions(startMonth, endMonth) {
  return api
    .get('/api/production-plan/versions', { params: { start_month: startMonth, end_month: endMonth } })
    .then((r) => r.data)
}

export function getProductionPlanByRange(startMonth, endMonth, version) {
  return api
    .get('/api/production-plan', {
      params: { start_month: startMonth, end_month: endMonth, version },
    })
    .then((r) => r.data)
}

export function updateProductionPlanBuffer(year, productCode, bufferQuantity) {
  return api.put('/api/production-plan/buffer', { year, productCode, bufferQuantity }).then((r) => r.data)
}

export function updateProductionPlanItem(id, data) {
  return api.put(`/api/production-plan/${id}`, data).then((r) => r.data)
}
