import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { listRoles, deleteRole } from '../../api/roles'
import './RolePages.css'

export default function RoleListPage() {
  const navigate = useNavigate()
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadRoles()
  }, [])

  async function loadRoles() {
    setError('')
    setLoading(true)
    try {
      const data = await listRoles()
      setRoles(data)
    } catch (err) {
      setError('無法載入角色列表')
    } finally {
      setLoading(false)
    }
  }

  async function handleDeleteRole(r) {
    const ok = window.confirm(`確定要刪除角色「${r.name}」嗎？`)
    if (!ok) return

    setError('')
    setLoading(true)
    try {
      await deleteRole(r.id)
      await loadRoles()
    } catch (err) {
      setError(err.response?.data?.message || '刪除失敗')
      setLoading(false)
    }
  }

  return (
    <div className="role-page">
      <div className="role-page-header">
        <h1>角色管理</h1>
        <button
          type="button"
          className="btn btn--small btn--primary"
          onClick={() => navigate('/roles/create')}
        >
          建立角色
        </button>
      </div>

      {error && <div className="role-error" role="alert">{error}</div>}

      {loading ? (
        <div className="role-loading">載入中...</div>
      ) : roles.length === 0 ? (
        <div className="role-empty">查無角色</div>
      ) : (
        <div className="role-table-wrap">
          <table className="role-table">
            <thead>
              <tr>
                <th>代碼</th>
                <th>名稱</th>
                <th>描述</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {roles.map((r) => (
                <tr key={r.id}>
                  <td><code>{r.code}</code></td>
                  <td>{r.name}</td>
                  <td>{r.description || '—'}</td>
                  <td>
                    <button
                      type="button"
                      className="btn btn--small btn--secondary"
                      onClick={() => navigate(`/roles/${r.id}/edit`)}
                    >
                      編輯
                    </button>
                    <button
                      type="button"
                      className="btn btn--small btn--danger"
                      onClick={() => handleDeleteRole(r)}
                      disabled={loading}
                    >
                      刪除
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

