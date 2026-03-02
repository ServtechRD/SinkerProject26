import api from './axios'

export function getCategories() {
  return api.get('/api/categories').then((r) => r.data)
}

export function getProducts() {
  return api.get('/api/products').then((r) => r.data)
}
