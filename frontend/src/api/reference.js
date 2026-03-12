import api from './axios'

export function getCategories() {
  return api.get('/api/categories').then((r) => r.data)
}

export function getProducts() {
  return api.get('/api/products').then((r) => r.data)
}

/** 依品號查詢單一產品，回傳品名、貨品規格(spec)、庫位(warehouseLocation) */
export function getProductByCode(code) {
  if (!code || !String(code).trim()) return Promise.resolve(null)
  return api
    .get('/api/products', { params: { code: String(code).trim() } })
    .then((r) => r.data)
    .catch((err) => {
      if (err.response?.status === 404) return null
      throw err
    })
}
