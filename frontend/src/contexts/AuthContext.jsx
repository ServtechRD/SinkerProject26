import { createContext, useContext, useState, useCallback } from 'react'
import { loginApi } from '../api/auth'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('authToken'))
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    if (stored) {
      try {
        return JSON.parse(stored)
      } catch {
        localStorage.removeItem('user')
        return null
      }
    }
    return null
  })

  const isAuthenticated = !!token

  const login = useCallback(async (username, password) => {
    const data = await loginApi(username, password)
    localStorage.setItem('authToken', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    setToken(data.token)
    setUser(data.user)
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export default AuthContext
