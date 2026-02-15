import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './LoginPage.css'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const from = location.state?.from?.pathname || '/'

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (!username.trim() || !password) {
      setError('Username and password are required')
      return
    }

    setLoading(true)
    try {
      await login(username.trim(), password)
      navigate(from, { replace: true })
    } catch (err) {
      const status = err.response?.status
      const message = err.response?.data?.message
      if (status === 401) {
        setError(message || 'Invalid username or password')
      } else if (status === 403) {
        setError(message || 'Account access denied')
      } else {
        setError('Network error. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <form className="login-form" onSubmit={handleSubmit} noValidate>
        <h1 className="login-title">Sinker</h1>
        {error && (
          <div className="login-error" role="alert">
            {error}
          </div>
        )}
        <div className="login-field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            type="text"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
            disabled={loading}
          />
        </div>
        <div className="login-field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            disabled={loading}
          />
        </div>
        <button type="submit" className="login-submit" disabled={loading}>
          {loading ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </div>
  )
}
