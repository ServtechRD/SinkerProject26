import api from './axios'

export async function loginApi(username, password) {
  const response = await api.post('/api/auth/login', { username, password })
  return response.data
}
