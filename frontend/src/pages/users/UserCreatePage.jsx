import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { createUser, listRoles } from '../../api/users'
import { useToast } from '../../components/Toast'
import './UserPages.css'

const CHANNELS = [
  'PX + 大全聯', '家樂福', '愛買', '7-11', '全家', 'Ok+萊爾富',
  '好市多', '楓康', '美聯社', '康是美', '電商', '市面經銷',
]

export default function UserCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()

  const [roles, setRoles] = useState([])
  const [form, setForm] = useState({
    username: '', email: '', password: '', fullName: '',
    roleId: '', department: '', phone: '', channels: [],
  })
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    listRoles().then(setRoles).catch(() => {})
  }, [])

  const selectedRole = roles.find((r) => String(r.id) === String(form.roleId))
  const isSales = selectedRole?.code === 'sales'

  // sales 角色預設勾選所有通路
  useEffect(() => {
    setForm((prev) => {
      if (isSales) {
        if (prev.channels && prev.channels.length > 0) return prev
        return { ...prev, channels: [...CHANNELS] }
      }
      return { ...prev, channels: [] }
    })
  }, [isSales])

  const USERNAME_REGEX = /^[a-zA-Z0-9_]*$/
  const USERNAME_MIN = 3
  const USERNAME_MAX = 50

  function handleChange(e) {
    const { name, value } = e.target
    if (name === 'username') {
      const filtered = value.replace(/[^a-zA-Z0-9_]/g, '')
      setForm((prev) => ({ ...prev, [name]: filtered }))
    } else {
      setForm((prev) => ({ ...prev, [name]: value }))
    }
    setErrors((prev) => ({ ...prev, [name]: '' }))
  }

  function handleChannelToggle(ch) {
    setForm((prev) => {
      const channels = prev.channels.includes(ch)
        ? prev.channels.filter((c) => c !== ch)
        : [...prev.channels, ch]
      return { ...prev, channels }
    })
    setErrors((prev) => ({ ...prev, channels: '' }))
  }

  function handleChannelsSelectAll() {
    setForm((prev) => ({ ...prev, channels: [...CHANNELS] }))
    setErrors((prev) => ({ ...prev, channels: '' }))
  }

  function handleChannelsSelectNone() {
    setForm((prev) => ({ ...prev, channels: [] }))
    setErrors((prev) => ({ ...prev, channels: '' }))
  }

  function validate() {
    const errs = {}
    const un = form.username.trim()
    if (!un) {
      errs.username = '帳號為必填'
    } else if (!USERNAME_REGEX.test(un)) {
      errs.username = '帳號僅能使用英文、數字與底線，不允許中文或特殊符號'
    } else if (un.length < USERNAME_MIN || un.length > USERNAME_MAX) {
      errs.username = `帳號長度為 ${USERNAME_MIN}-${USERNAME_MAX} 個字元`
    }
    if (!form.email.trim()) errs.email = 'Email 為必填'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = 'Email 格式無效'
    if (!form.password) {
      errs.password = '密碼為必填'
    } else {
      const pwd = form.password
      if (pwd.length < 8) {
        errs.password = '密碼至少 8 個字元'
      } else {
        const hasLetter = /[a-zA-Z]/.test(pwd)
        const hasDigit = /\d/.test(pwd)
        if (!hasLetter && !hasDigit) {
          errs.password = '密碼須包含英文字母與數字'
        } else if (!hasLetter) {
          errs.password = '密碼須包含英文字母'
        } else if (!hasDigit) {
          errs.password = '密碼須包含數字'
        }
      }
    }
    if (!form.fullName.trim()) errs.fullName = '姓名為必填'
    if (!form.roleId) errs.roleId = '角色為必填'
    if (isSales && form.channels.length === 0) errs.channels = '業務角色需選擇至少一個通路'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setApiError('')
    if (!validate()) return

    setSubmitting(true)
    try {
      const payload = {
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        roleId: Number(form.roleId),
        department: form.department.trim() || undefined,
        phone: form.phone.trim() || undefined,
      }
      if (isSales && form.channels.length > 0) {
        payload.channels = form.channels
      }
      await createUser(payload)
      toast.success('使用者建立成功')
      navigate('/users')
    } catch (err) {
      const msg = err.response?.data?.message || ''
      const status = err.response?.status
      if (status === 409) {
        const lower = (msg || '').toLowerCase()
        if (lower.includes('email')) {
          setErrors((prev) => ({ ...prev, email: '此 Email 已被使用' }))
          setApiError('此 Email 已被使用，請使用其他 Email。')
        } else if (lower.includes('username')) {
          setErrors((prev) => ({ ...prev, username: '此帳號已被使用' }))
          setApiError(msg || '此帳號已被使用，請使用其他帳號。')
        } else {
          setApiError(msg || '資料重複，請檢查後再試。')
        }
      } else {
        setApiError(msg || '建立失敗，請稍後再試')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="user-form-page">
      <h1>建立使用者</h1>

      {apiError && <div className="form-api-error" role="alert">{apiError}</div>}

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-group">
          <label htmlFor="username">帳號 <span className="required">*</span></label>
          <input id="username" name="username" className={`form-input${errors.username ? ' error' : ''}`}
            value={form.username} onChange={handleChange} maxLength={USERNAME_MAX}
            placeholder="僅限英文、數字、底線，3-50 字元"
            disabled={submitting} />
          {errors.username && <div className="form-error">{errors.username}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="email">Email <span className="required">*</span></label>
          <input id="email" name="email" type="email" className={`form-input${errors.email ? ' error' : ''}`}
            value={form.email} onChange={handleChange} disabled={submitting} />
          {errors.email && <div className="form-error">{errors.email}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="password">密碼 <span className="required">*</span></label>
          <input id="password" name="password" type="password" className={`form-input${errors.password ? ' error' : ''}`}
            value={form.password} onChange={handleChange} disabled={submitting}
            placeholder="至少 8 字元，須包含英文與數字" />
          {errors.password && <div className="form-error">{errors.password}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="fullName">姓名 <span className="required">*</span></label>
          <input id="fullName" name="fullName" className={`form-input${errors.fullName ? ' error' : ''}`}
            value={form.fullName} onChange={handleChange} disabled={submitting} />
          {errors.fullName && <div className="form-error">{errors.fullName}</div>}
        </div>

        <div className="form-group">
          <label htmlFor="roleId">角色 <span className="required">*</span></label>
          <select id="roleId" name="roleId" className={`form-select${errors.roleId ? ' error' : ''}`}
            value={form.roleId} onChange={handleChange} disabled={submitting}>
            <option value="">請選擇角色</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </select>
          {errors.roleId && <div className="form-error">{errors.roleId}</div>}
        </div>

        {isSales && (
          <div className="form-group">
            <label>通路 <span className="required">*</span></label>
            <div className="channel-toolbar">
              <button
                type="button"
                className="btn btn--small btn--outline"
                onClick={handleChannelsSelectAll}
                disabled={submitting}
              >
                全選
              </button>
              <button
                type="button"
                className="btn btn--small btn--outline"
                onClick={handleChannelsSelectNone}
                disabled={submitting}
              >
                全不選
              </button>
            </div>
            <div className="channel-checkboxes">
              {CHANNELS.map((ch) => (
                <label key={ch}>
                  <input type="checkbox" checked={form.channels.includes(ch)}
                    onChange={() => handleChannelToggle(ch)} disabled={submitting} />
                  {ch}
                </label>
              ))}
            </div>
            {errors.channels && <div className="form-error">{errors.channels}</div>}
          </div>
        )}

        <div className="form-group">
          <label htmlFor="department">部門</label>
          <input id="department" name="department" className="form-input"
            value={form.department} onChange={handleChange} disabled={submitting} />
        </div>

        <div className="form-group">
          <label htmlFor="phone">電話</label>
          <input id="phone" name="phone" className="form-input"
            value={form.phone} onChange={handleChange} disabled={submitting} />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {submitting ? '建立中...' : '建立'}
          </button>
          <button type="button" className="btn btn--secondary" onClick={() => navigate('/users')} disabled={submitting}>
            取消
          </button>
        </div>
      </form>
    </div>
  )
}
