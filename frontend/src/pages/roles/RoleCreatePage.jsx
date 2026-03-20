import { useState, useEffect, useMemo, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRole, getPermissionsGroupedByModule } from '../../api/roles'
import { useToast } from '../../components/Toast'
import './RolePages.css'

const MODULE_LABELS = {
  user: '使用者管理',
  role: '角色管理',
  sales_forecast: '銷售預測',
  sales_forecast_config: '預測設定',
  production_plan: '生產表單',
  inventory: '庫存銷量預估量整合表單',
  weekly_schedule: '週排程',
  semi_product: '半成品設定',
  material_demand: '物料需求',
}

export default function RoleCreatePage() {
  const navigate = useNavigate()
  const toast = useToast()

  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [apiError, setApiError] = useState('')
  const [errors, setErrors] = useState({})

  const [code, setCode] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [selectedIds, setSelectedIds] = useState(new Set())
  const [allPermissions, setAllPermissions] = useState({})
  const selectAllRefs = useRef({})

  useEffect(() => {
    getPermissionsGroupedByModule()
      .then((data) => {
        setAllPermissions(data.permissionsByModule || {})
      })
      .catch(() => setApiError('無法載入權限資料'))
      .finally(() => setLoading(false))
  }, [])

  const visiblePermissionsByModule = useMemo(() => allPermissions || {}, [allPermissions])

  const updateIndeterminate = useCallback(() => {
    Object.entries(visiblePermissionsByModule).forEach(([mod, perms]) => {
      const ref = selectAllRefs.current[mod]
      if (!ref) return
      const list = Array.isArray(perms) ? perms : []
      const count = list.filter((p) => selectedIds.has(p.id)).length
      ref.indeterminate = count > 0 && count < list.length
    })
  }, [visiblePermissionsByModule, selectedIds])

  useEffect(() => {
    updateIndeterminate()
  }, [updateIndeterminate])

  function handleToggle(permId) {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      if (next.has(permId)) next.delete(permId)
      else next.add(permId)
      return next
    })
  }

  function handleModuleSelectAll(mod) {
    const perms = visiblePermissionsByModule[mod] || []
    const allSelected = perms.every((p) => selectedIds.has(p.id))
    setSelectedIds((prev) => {
      const next = new Set(prev)
      perms.forEach((p) => {
        if (allSelected) next.delete(p.id)
        else next.add(p.id)
      })
      return next
    })
  }

  function validate() {
    const errs = {}
    if (!code.trim()) errs.code = '代碼為必填'
    if (!name.trim()) errs.name = '名稱為必填'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return
    setSubmitting(true)
    setApiError('')
    try {
      await createRole({
        code: code.trim(),
        name: name.trim(),
        description: description.trim() || null,
        permissionIds: Array.from(selectedIds).map((pid) => Number(pid)),
      })
      toast.success('角色已建立')
      navigate('/roles')
    } catch (err) {
      setApiError(err.response?.data?.message || '建立失敗')
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) return <div className="role-loading">載入中...</div>

  const moduleOrder = Object.keys(MODULE_LABELS)
  const modules = moduleOrder.filter((m) => visiblePermissionsByModule[m]?.length > 0)

  return (
    <div className="role-edit-page">
      <h1>建立角色</h1>

      {apiError && <div className="form-api-error" role="alert">{apiError}</div>}

      <form onSubmit={handleSubmit}>
        <fieldset disabled={submitting} className="role-fieldset">
          <div className="role-info-section">
            <div className="form-group">
              <label htmlFor="role-code">代碼 <span className="required">*</span></label>
              <input
                id="role-code"
                className={`form-input${errors.code ? ' error' : ''}`}
                value={code}
                onChange={(e) => {
                  setCode(e.target.value)
                  setErrors((prev) => ({ ...prev, code: '' }))
                }}
                maxLength={50}
              />
              {errors.code && <div className="form-error">{errors.code}</div>}
            </div>
            <div className="form-group">
              <label htmlFor="role-name">名稱 <span className="required">*</span></label>
              <input
                id="role-name"
                className={`form-input${errors.name ? ' error' : ''}`}
                value={name}
                onChange={(e) => {
                  setName(e.target.value)
                  setErrors((prev) => ({ ...prev, name: '' }))
                }}
                maxLength={100}
              />
              {errors.name && <div className="form-error">{errors.name}</div>}
            </div>
            <div className="form-group">
              <label htmlFor="role-desc">描述</label>
              <textarea
                id="role-desc"
                className="form-input"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={255}
                rows={3}
              />
            </div>
          </div>

          <div className="permissions-section">
            <h2>權限設定</h2>
            {modules.map((mod) => {
              const perms = visiblePermissionsByModule[mod]
              const allChecked = perms.every((p) => selectedIds.has(p.id))
              return (
                <div key={mod} className="permission-module">
                  <div className="module-header">
                    <label className="module-select-all">
                      <input
                        type="checkbox"
                        ref={(el) => {
                          selectAllRefs.current[mod] = el
                        }}
                        checked={allChecked}
                        onChange={() => handleModuleSelectAll(mod)}
                        aria-label={`全選 ${MODULE_LABELS[mod] || mod}`}
                      />
                      <strong>{MODULE_LABELS[mod] || mod}</strong>
                    </label>
                  </div>
                  <div className="permission-list">
                    {perms.map((p) => (
                      <label key={p.id} className="permission-item">
                        <input
                          type="checkbox"
                          checked={selectedIds.has(p.id)}
                          onChange={() => handleToggle(p.id)}
                        />
                        <span className="permission-name">{p.name}</span>
                        <span className="permission-code">{p.code}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )
            })}
          </div>

          <div className="form-actions">
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? '建立中...' : '建立'}
            </button>
            <button type="button" className="btn btn--secondary" onClick={() => navigate('/roles')} disabled={submitting}>
              取消
            </button>
          </div>
        </fieldset>
      </form>
    </div>
  )
}

