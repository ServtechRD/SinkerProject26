// 路由層權限守衛使用的對照表：path -> 需要的權限（陣列代表符合其一即可）。
// 只列出頁面本身已有 canView 判斷的路由，清單需與各頁面內的權限判斷保持一致。
export const ROUTE_PERMISSIONS = [
  { path: '/users', permission: 'user.view' },
  { path: '/roles', permission: 'role.view' },
  { path: '/sales-forecast/config', permission: 'sales_forecast_config.view' },
  { path: '/sales-forecast/upload', permission: ['sales_forecast.upload', 'sales_forecast.update_after_closed'] },
  { path: '/gift-sales-forecast/upload', permission: ['sales_forecast.upload', 'sales_forecast.update_after_closed'] },
  { path: '/sales-forecast/integration', permission: 'sales_forecast.view' },
  { path: '/sales-forecast', permission: 'sales_forecast.update_after_closed' },
  { path: '/inventory-integration', permission: 'inventory.view' },
  { path: '/production-plan', permission: 'production_plan.view' },
  { path: '/weekly-schedule', permission: 'weekly_schedule.view' },
  { path: '/semi-product/import', permission: 'semi_product.view' },
  { path: '/material-demand', permission: 'material_demand.view' },
  { path: '/material-demand/form', permission: 'material_demand.view' },
  { path: '/material-purchase', permission: 'material_purchase.view' },
  { path: '/material-purchase/form', permission: 'material_purchase.view' },
  { path: '/erp/product-sync', permission: 'user.view' },
  { path: '/erp/vendor-sync', permission: 'user.view' },
]

export function hasRoutePermission(user, pathname) {
  const route = ROUTE_PERMISSIONS.find((r) => r.path === pathname)
  if (!route) return true
  if (!user?.permissions || !Array.isArray(user.permissions)) return false
  const required = route.permission
  return Array.isArray(required)
    ? required.some((p) => user.permissions.includes(p))
    : user.permissions.includes(required)
}
