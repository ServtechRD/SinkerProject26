import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import UserListPage from './pages/users/UserListPage'
import ForecastConfigPage from './pages/sales-forecast/ForecastConfigPage'
import ForecastUploadPage from './pages/sales-forecast/ForecastUploadPage'

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <MainLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="users" element={<UserListPage />} />
        <Route path="sales-forecast/config" element={<ForecastConfigPage />} />
        <Route path="sales-forecast/upload" element={<ForecastUploadPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
