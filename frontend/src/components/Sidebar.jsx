import { NavLink } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { APP_VERSION } from '../version'
import './Sidebar.css'

const navItems = [
  { to: '/users', label: '使用者管理', permission: 'user.view' },
  { to: '/roles', label: '角色管理', permission: 'role.view' },
  { to: '/sales-forecast/config', label: '銷售預估量-表單設定', permission: 'sales_forecast_config.view' },
  { to: '/sales-forecast/upload', label: '銷售預估量表單上傳-共同編輯界面', permission: 'sales_forecast.upload' },
  { to: '/gift-sales-forecast/upload', label: '禮品銷售預估量表單上傳', permission: 'sales_forecast.upload' },
  { to: '/sales-forecast', label: '銷售預估量表單', permission: 'sales_forecast.update_after_closed', end: true },
  { to: '/inventory-integration', label: '庫存銷量預估量整合表單', permission: 'inventory.view' },
  { to: '/production-plan', label: '生產表單', permission: 'production_plan.view' },
  { to: '/weekly-schedule', label: '生產週排程表單-匯入', permission: 'weekly_schedule.view' },
  { to: '/semi-product/import', label: '半成品提前採購設定表單-匯入', permission: 'semi_product.view' },
  { to: '/material-demand/form', label: '物料需求數量表單', permission: 'material_demand.view' },
]

function hasPermission(user, perm) {
  if (perm == null) return true
  if (!user?.permissions || !Array.isArray(user.permissions)) return false
  if (Array.isArray(perm)) return perm.some((p) => user.permissions.includes(p))
  return user.permissions.includes(perm)
}

export default function Sidebar() {
  const { user, logout } = useAuth()
  const visibleItems = navItems.filter((item) => hasPermission(user, item.permission))

  return (
    <aside className="sidebar" data-testid="sidebar">
      <div className="sidebar-header">
        <h2>需求規劃平台</h2>
      </div>
      <nav className="sidebar-nav">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end ?? false}
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
        <div className="sidebar-version">版次 {APP_VERSION}</div>
      </div>
    </aside>
  )
}
