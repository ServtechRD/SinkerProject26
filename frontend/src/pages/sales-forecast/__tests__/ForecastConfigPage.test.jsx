import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastConfigPage from '../ForecastConfigPage'

vi.mock('../../../api/forecastConfig', () => ({
  listConfigs: vi.fn(),
  createMonth: vi.fn(),
  updateConfig: vi.fn(),
}))

import { listConfigs } from '../../../api/forecastConfig'

const mockConfigs = [
  {
    id: 1,
    month: '202601',
    autoCloseAt: '2026-01-10T00:00:00', activeReopenCount: 0,
    isClosed: false,
    closedAt: null,
    createdAt: '2026-01-01T00:00:00',
    updatedAt: '2026-01-01T00:00:00',
  },
  {
    id: 2,
    month: '202512',
    autoCloseAt: '2025-12-15T00:00:00', activeReopenCount: 2,
    isClosed: true,
    closedAt: '2025-12-15T10:30:00',
    createdAt: '2025-12-01T00:00:00',
    updatedAt: '2025-12-15T10:30:00',
  },
]

function renderPage({ permissions = [], roleCode = 'admin' } = {}) {
  const authValue = {
    token: 'test-token',
    user: { username: 'admin', fullName: 'Admin', roleCode, permissions },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast/config']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <ForecastConfigPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastConfigPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading spinner during fetch', () => {
    listConfigs.mockImplementation(() => new Promise(() => {}))
    renderPage({ permissions: ['sales_forecast_config.view', 'sales_forecast_config.edit'] })

    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('renders config table with data', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view', 'sales_forecast_config.edit'] })

    await waitFor(() => {
      expect(screen.getByText('202601')).toBeInTheDocument()
    })
    expect(screen.getByText('202512')).toBeInTheDocument()
    expect(screen.getByText('2026/01/10 00:00')).toBeInTheDocument()
    expect(screen.getByText('2025/12/15 00:00')).toBeInTheDocument()
    expect(screen.getByText('開放中')).toBeInTheDocument()
    expect(screen.getByText('已結束新增設定')).toBeInTheDocument()
  })

  it('displays open status with correct badge', async () => {
    listConfigs.mockResolvedValue([mockConfigs[0]])
    renderPage({ permissions: ['sales_forecast_config.view'] })

    await waitFor(() => {
      expect(screen.getByText('開放中')).toBeInTheDocument()
    })
    const badge = screen.getByText('開放中')
    expect(badge.className).toContain('badge--open')
  })

  it('displays closed status with correct badge', async () => {
    listConfigs.mockResolvedValue([mockConfigs[1]])
    renderPage({ permissions: ['sales_forecast_config.view'] })

    await waitFor(() => {
      expect(screen.getByText('已結束新增設定')).toBeInTheDocument()
    })
    const badge = screen.getByText('已結束新增設定')
    expect(badge.className).toContain('badge--closed')
  })

  it('shows create button with edit permission', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: ['sales_forecast_config.view', 'sales_forecast_config.edit'] })

    await waitFor(() => {
      expect(screen.getByText('新增填寫月份')).toBeInTheDocument()
    })
  })

  it('hides create button without edit permission', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: ['sales_forecast_config.view'], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.queryByText('載入中...')).not.toBeInTheDocument()
    })
    expect(screen.queryByText('新增填寫月份')).not.toBeInTheDocument()
  })

  it('hides edit buttons without edit permission', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view'], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.getByText('202601')).toBeInTheDocument()
    })
    expect(screen.queryByRole('button', { name: '編輯' })).not.toBeInTheDocument()
  })

  it('displays empty state when no configs', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: ['sales_forecast_config.view', 'sales_forecast_config.edit'] })

    await waitFor(() => {
      expect(screen.getByText('尚無設定資料，請建立第一個月份。')).toBeInTheDocument()
    })
  })

  it('shows access denied on 403', async () => {
    listConfigs.mockRejectedValue({ response: { status: 403 } })
    renderPage({ permissions: [], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('已結束且有補登中通路時顯示橘色標籤', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view'] })
    expect(await screen.findByText('補登中 2 通路')).toBeInTheDocument()
  })

  it('月份連結導向表單明細頁', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view'] })
    const link = await screen.findByRole('link', { name: '202601' })
    expect(link).toHaveAttribute('href', '/sales-forecast/config/1')
  })
})
