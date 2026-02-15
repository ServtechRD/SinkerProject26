import { NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import './Sidebar.css'

const navItems = [
  { to: '/', label: 'Dashboard' },
  { to: '/users', label: 'Users' },
  { to: '/sales-forecast/config', label: 'Forecast Config' },
  { to: '/sales-forecast/upload', label: 'Forecast Upload' },
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
          Logout
        </button>
      </div>
    </aside>
  )
}
