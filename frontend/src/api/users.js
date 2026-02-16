import api from './axios'

export function listUsers(params = {}) {
  return api.get('/api/users', { params }).then((r) => r.data)
}

export function getUserById(id) {
  return api.get(`/api/users/${id}`).then((r) => r.data)
}

export function createUser(data) {
  return api.post('/api/users', data).then((r) => r.data)
}

export function updateUser(id, data) {
  return api.put(`/api/users/${id}`, data).then((r) => r.data)
}

export function deleteUser(id) {
  return api.delete(`/api/users/${id}`)
}

export function toggleUserActive(id) {
  return api.patch(`/api/users/${id}/toggle`).then((r) => r.data)
}

export function listRoles() {
  return api.get('/api/roles').then((r) => r.data.roles)
}
