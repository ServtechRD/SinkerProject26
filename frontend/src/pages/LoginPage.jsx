import { useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { APP_VERSION } from '../version'
import logoUrl from '../assets/logo.jpg'
import './LoginPage.css'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
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
        <img src={logoUrl} alt="需求規劃平台" className="login-logo" />
        <h1 className="login-title">需求規劃平台</h1>
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
          <div className="login-password-wrap">
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              disabled={loading}
            />
            <button
              type="button"
              className="login-password-toggle"
              onClick={() => setShowPassword((v) => !v)}
              tabIndex={-1}
              disabled={loading}
              aria-label={showPassword ? '隱藏密碼' : '顯示密碼'}
              title={showPassword ? '隱藏密碼' : '顯示密碼'}
            >
              {showPassword ? (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              ) : (
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden>
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              )}
            </button>
          </div>
        </div>
        <button type="submit" className="login-submit" disabled={loading}>
          {loading ? '登入中...' : '登入'}
        </button>
        <p className="login-version">版次 {APP_VERSION}</p>
      </form>
    </div>
  )
}
