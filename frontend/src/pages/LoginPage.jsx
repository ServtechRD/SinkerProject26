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
      setError('請輸入帳號與密碼')
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
        setError(message || '帳號或密碼錯誤')
      } else if (status === 403) {
        setError(message || '帳號存取被拒絕')
      } else {
        setError('網路錯誤，請稍後再試')
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
          <label htmlFor="username">帳號</label>
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
          <label htmlFor="password">密碼</label>
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
          {loading ? '登入中...' : '登入'}
        </button>
      </form>
    </div>
  )
}
