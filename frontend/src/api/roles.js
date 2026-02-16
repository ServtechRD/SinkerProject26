import api from './axios'

export function listRoles() {
  return api.get('/api/roles').then((r) => r.data.roles)
}

export function getRoleById(id) {
  return api.get(`/api/roles/${id}`).then((r) => r.data)
}

export function updateRole(id, data) {
  return api.put(`/api/roles/${id}`, data).then((r) => r.data)
}
