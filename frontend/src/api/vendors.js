import api from './axios'

export function getVendorList(keyword = '') {
  const params = keyword ? { keyword } : {}
  return api.get('/api/vendors', { params }).then((r) => r.data)
}
