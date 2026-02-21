import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { listUsers, deleteUser, toggleUserActive, listRoles } from '../../api/users'
import { useToast } from '../../components/Toast'
import ConfirmDialog from '../../components/ConfirmDialog'
import './UserPages.css'

export default function UserListPage() {
  const navigate = useNavigate()
  const toast = useToast()

  const [users, setUsers] = useState([])
  const [roles, setRoles] = useState([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)

  const [keyword, setKeyword] = useState('')
  const [roleId, setRoleId] = useState('')
  const [isActive, setIsActive] = useState('')
  const [sortBy, setSortBy] = useState('id')
  const [sortOrder, setSortOrder] = useState('asc')

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [togglingId, setTogglingId] = useState(null)

  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  const debounceRef = useRef(null)

  const fetchUsers = useCallback(async (params) => {
    setLoading(true)
    setError('')
    try {
      const data = await listUsers({
        page: params.page ?? currentPage,
        size: 20,
        keyword: params.keyword ?? keyword,
        roleId: (params.roleId ?? roleId) || undefined,
        isActive: (params.isActive ?? isActive) || undefined,
        sortBy: params.sortBy ?? sortBy,
        sortOrder: params.sortOrder ?? sortOrder,
      })
      setUsers(data.users)
      setTotalElements(data.totalElements)
      setTotalPages(data.totalPages)
      setCurrentPage(data.currentPage)
    } catch {
      setError('無法載入使用者列表')
    } finally {
      setLoading(false)
    }
  }, [currentPage, keyword, roleId, isActive, sortBy, sortOrder])

  useEffect(() => {
    listRoles().then(setRoles).catch(() => {})
  }, [])

  useEffect(() => {
    fetchUsers({})
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  function handleSearchChange(e) {
    const val = e.target.value
    setKeyword(val)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      fetchUsers({ keyword: val, page: 0 })
      setCurrentPage(0)
    }, 300)
  }

  function handleRoleFilter(e) {
    const val = e.target.value
    setRoleId(val)
    setCurrentPage(0)
    fetchUsers({ roleId: val, page: 0 })
  }

  function handleStatusFilter(e) {
    const val = e.target.value
    setIsActive(val)
    setCurrentPage(0)
    fetchUsers({ isActive: val, page: 0 })
  }

  function handleSort(col) {
    const newOrder = sortBy === col && sortOrder === 'asc' ? 'desc' : 'asc'
    setSortBy(col)
    setSortOrder(newOrder)
    fetchUsers({ sortBy: col, sortOrder: newOrder })
  }

  function handlePageChange(page) {
    setCurrentPage(page)
    fetchUsers({ page })
  }

  async function handleToggle(user) {
    setTogglingId(user.id)
    try {
      const updated = await toggleUserActive(user.id)
      setUsers((prev) => prev.map((u) => (u.id === user.id ? updated : u)))
      toast.success(`${user.username} 狀態已更新`)
    } catch {
      toast.error('狀態切換失敗')
    } finally {
      setTogglingId(null)
    }
  }

  async function handleDeleteConfirm() {
    if (!deleteTarget) return
    setDeleting(true)
    try {
      await deleteUser(deleteTarget.id)
      toast.success(`使用者 ${deleteTarget.username} 已刪除`)
      setDeleteTarget(null)
      fetchUsers({})
    } catch {
      toast.error('刪除失敗')
    } finally {
      setDeleting(false)
    }
  }

  function sortIcon(col) {
    if (sortBy !== col) return ''
    return sortOrder === 'asc' ? ' ▲' : ' ▼'
  }

  return (
    <div className="user-page">
      <div className="user-page-header">
        <h1>使用者管理</h1>
        <button className="btn btn--primary" onClick={() => navigate('/users/create')}>
          建立使用者
        </button>
      </div>

      <div className="user-filters">
        <input
          className="filter-input"
          type="text"
          placeholder="搜尋帳號、姓名、Email..."
          value={keyword}
          onChange={handleSearchChange}
          aria-label="搜尋"
        />
        <select className="filter-select" value={roleId} onChange={handleRoleFilter} aria-label="角色篩選">
          <option value="">所有角色</option>
          {roles.map((r) => (
            <option key={r.id} value={r.id}>
              {r.name}
            </option>
          ))}
        </select>
        <select className="filter-select" value={isActive} onChange={handleStatusFilter} aria-label="狀態篩選">
          <option value="">全部狀態</option>
          <option value="true">啟用</option>
          <option value="false">停用</option>
        </select>
      </div>

      {error && <div className="user-error" role="alert">{error}</div>}

      {loading ? (
        <div className="user-loading">載入中...</div>
      ) : users.length === 0 ? (
        <div className="user-empty">查無使用者</div>
      ) : (
        <>
          <div className="user-table-wrap">
            <table className="user-table">
              <thead>
                <tr>
                  <th className="sortable" onClick={() => handleSort('username')}>
                    帳號{sortIcon('username')}
                  </th>
                  <th>姓名</th>
                  <th className="sortable" onClick={() => handleSort('email')}>
                    Email{sortIcon('email')}
                  </th>
                  <th>角色</th>
                  <th>狀態</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.username}</td>
                    <td>{u.fullName}</td>
                    <td>{u.email}</td>
                    <td>{u.role?.name}</td>
                    <td>
                      <span className={`badge badge--${u.isActive ? 'active' : 'inactive'}`}>
                        {u.isActive ? '啟用' : '停用'}
                      </span>
                    </td>
                    <td className="actions-cell">
                      <button
                        className="btn btn--small btn--secondary"
                        onClick={() => navigate(`/users/${u.id}/edit`)}
                      >
                        編輯
                      </button>
                      <button
                        className="btn btn--small btn--outline"
                        onClick={() => handleToggle(u)}
                        disabled={togglingId === u.id}
                      >
                        {togglingId === u.id ? '...' : u.isActive ? '停用' : '啟用'}
                      </button>
                      <button
                        className="btn btn--small btn--danger"
                        onClick={() => setDeleteTarget(u)}
                      >
                        刪除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <button
              className="btn btn--small"
              disabled={currentPage === 0}
              onClick={() => handlePageChange(currentPage - 1)}
            >
              上一頁
            </button>
            <span className="pagination-info">
              第 {currentPage + 1} / {totalPages} 頁 (共 {totalElements} 筆)
            </span>
            <button
              className="btn btn--small"
              disabled={currentPage >= totalPages - 1}
              onClick={() => handlePageChange(currentPage + 1)}
            >
              下一頁
            </button>
          </div>
        </>
      )}

      <ConfirmDialog
        open={!!deleteTarget}
        title="確認刪除"
        message={deleteTarget ? `確定要刪除使用者「${deleteTarget.username}」嗎？此操作無法復原。` : ''}
        onConfirm={handleDeleteConfirm}
        onCancel={() => setDeleteTarget(null)}
        loading={deleting}
      />
    </div>
  )
}
