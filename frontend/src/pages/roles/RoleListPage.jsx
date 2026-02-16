import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { listRoles } from '../../api/roles'
import './RolePages.css'

export default function RoleListPage() {
  const navigate = useNavigate()
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    listRoles()
      .then(setRoles)
      .catch(() => setError('無法載入角色列表'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="role-page">
      <div className="role-page-header">
        <h1>角色管理</h1>
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
                <th>系統角色</th>
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
                    <span className={`badge badge--${r.isSystem ? 'active' : 'inactive'}`}>
                      {r.isSystem ? '是' : '否'}
                    </span>
                  </td>
                  <td>
                    <button
                      className="btn btn--small btn--secondary"
                      onClick={() => navigate(`/roles/${r.id}/edit`)}
                    >
                      編輯
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
