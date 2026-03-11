import { Outlet } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import MaterialDemandPendingBanner from '../components/MaterialDemandPendingBanner'
import './MainLayout.css'

export default function MainLayout() {
  return (
    <div className="main-layout">
      <Sidebar />
      <main className="main-content" data-testid="main-content">
        <MaterialDemandPendingBanner />
        <Outlet />
      </main>
    </div>
  )
}
