import { NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './Sidebar.css'

const navItems = [
  { to: '/', label: '儀表板' },
  { to: '/users', label: '使用者' },
  { to: '/roles', label: '角色管理' },
  { to: '/sales-forecast/config', label: '預測設定' },
  { to: '/sales-forecast/upload', label: '預測上傳' },
  { to: '/sales-forecast', label: '銷售預測' },
  { to: '/inventory-integration', label: '庫存整合' },
  { to: '/production-plan', label: '生產計畫' },
]

export default function Sidebar() {
  const { user, logout } = useAuth()

  return (
    <aside className="sidebar" data-testid="sidebar">
      <div className="sidebar-header">
        <h2>Sinker</h2>
      </div>
      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `sidebar-link${isActive ? ' sidebar-link--active' : ''}`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        {user && (
          <div className="sidebar-user">{user.fullName || user.username}</div>
        )}
        <button className="sidebar-logout" onClick={logout}>
          登出
        </button>
      </div>
    </aside>
  )
}
