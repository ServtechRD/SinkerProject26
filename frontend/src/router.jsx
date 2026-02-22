import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import UserListPage from './pages/users/UserListPage'
import UserCreatePage from './pages/users/UserCreatePage'
import UserEditPage from './pages/users/UserEditPage'
import RoleListPage from './pages/roles/RoleListPage'
import RoleEditPage from './pages/roles/RoleEditPage'
import ForecastConfigPage from './pages/sales-forecast/ForecastConfigPage'
import ForecastUploadPage from './pages/sales-forecast/ForecastUploadPage'
import ForecastListPage from './pages/sales-forecast/ForecastListPage'
import ForecastIntegrationPage from './pages/sales-forecast/ForecastIntegrationPage'
import InventoryIntegrationPage from './pages/inventory/InventoryIntegrationPage'
import ProductionPlanPage from './pages/production/ProductionPlanPage'
import WeeklySchedulePage from './pages/schedule/WeeklySchedulePage'
import SemiProductPage from './pages/semiProduct/SemiProductPage'
import MaterialDemandPage from './pages/material/MaterialDemandPage'
import MaterialPurchasePage from './pages/material/MaterialPurchasePage'

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
        <Route path="users/create" element={<UserCreatePage />} />
        <Route path="users/:id/edit" element={<UserEditPage />} />
        <Route path="roles" element={<RoleListPage />} />
        <Route path="roles/:id/edit" element={<RoleEditPage />} />
        <Route path="sales-forecast/config" element={<ForecastConfigPage />} />
        <Route path="sales-forecast/upload" element={<ForecastUploadPage />} />
        <Route path="sales-forecast/integration" element={<ForecastIntegrationPage />} />
        <Route path="sales-forecast" element={<ForecastListPage />} />
        <Route path="inventory-integration" element={<InventoryIntegrationPage />} />
        <Route path="production-plan" element={<ProductionPlanPage />} />
        <Route path="weekly-schedule" element={<WeeklySchedulePage />} />
        <Route path="semi-product" element={<SemiProductPage />} />
        <Route path="material-demand" element={<MaterialDemandPage />} />
        <Route path="material-purchase" element={<MaterialPurchasePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
