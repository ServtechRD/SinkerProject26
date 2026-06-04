import api from './axios'

export function syncErpProducts(param = {}) {
  return api.post('/api/integrations/erp/product-sync', param).then((r) => r.data)
}
