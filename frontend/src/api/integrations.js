import api from './axios'

export function syncErpProducts(param = {}) {
  return api.post('/api/integrations/erp/product-sync', param).then((r) => r.data)
}

export function getErpProductSyncStatus() {
  return api.get('/api/integrations/erp/product-sync/status').then((r) => r.data)
}

export function syncErpVendors(param = {}) {
  return api.post('/api/integrations/erp/vendor-sync', param).then((r) => r.data)
}

export function getErpVendorSyncStatus() {
  return api.get('/api/integrations/erp/vendor-sync/status').then((r) => r.data)
}
