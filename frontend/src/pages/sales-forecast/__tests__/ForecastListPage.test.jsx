import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastListPage from '../ForecastListPage'

vi.mock('../../../api/forecastConfig', () => ({
  listConfigs: vi.fn(),
}))

vi.mock('../../../api/forecast', () => ({
  getForecastVersions: vi.fn(),
  getForecastList: vi.fn(),
  getFormVersions: vi.fn(),
  getFormSummary: vi.fn(),
  saveFormSummaryVersion: vi.fn(),
  getFormSummaryRange: vi.fn(),
  updateForecastItem: vi.fn(),
  deleteForecastItem: vi.fn(),
}))

import { listConfigs } from '../../../api/forecastConfig'
import { getForecastVersions, getForecastList, getFormVersions, getFormSummary, saveFormSummaryVersion } from '../../../api/forecast'

const mockConfigs = [
  {
    id: 1,
    month: '202601',
    autoCloseDay: 10,
    isClosed: false,
    closedAt: null,
  },
  {
    id: 2,
    month: '202602',
    autoCloseDay: 15,
    isClosed: false,
    closedAt: null,
  },
]

const mockVersions = ['v2', 'v1']

const mockForecastData = [
  {
    id: 1,
    category: '飲料',
    spec: '500ml',
    productCode: 'P001',
    productName: '測試產品1',
    warehouseLocation: 'A1',
    quantity: 100,
    isModified: false,
  },
  {
    id: 2,
    category: '食品',
    spec: '1kg',
    productCode: 'P002',
    productName: '測試產品2',
    warehouseLocation: 'B2',
    quantity: 50,
    isModified: true,
  },
]

function renderPage({ permissions = [], roleCode = 'sales', channels = [] } = {}) {
  const authValue = {
    token: 'test-token',
    user: { username: 'sales', fullName: 'Sales', roleCode, permissions, channels },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <ForecastListPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading state during fetch', () => {
    listConfigs.mockImplementation(() => new Promise(() => {}))
    renderPage({ permissions: ['sales_forecast.view'], channels: ['家樂福'] })

    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows access denied without permission', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: [], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('renders filters', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    expect(screen.getByLabelText('月份')).toBeInTheDocument()
    expect(screen.getByLabelText('版本')).toBeInTheDocument()
  })

  it('shows months in dropdown', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByText('202601 (January 2026)')).toBeInTheDocument()
    })

    expect(screen.getByText('202602 (February 2026)')).toBeInTheDocument()
  })

  it('loads versions when month selected', async () => {
    const closedConfigs = [{ id: 1, month: '202601', autoCloseDay: 10, isClosed: true, closedAt: '2026-01-10T10:00:00' }]
    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])

    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(getFormVersions).toHaveBeenCalledWith('202601')
    })
  })

  it('displays forecast data in table', async () => {
    const closedConfigs = [{ id: 1, month: '202601', autoCloseDay: 10, isClosed: true, closedAt: '2026-01-10T10:00:00' }]
    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: mockForecastData, channel_order: [] })

    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(getFormVersions).toHaveBeenCalledWith('202601')
    })
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '查詢' })).not.toBeDisabled()
    })

    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(getFormSummary).toHaveBeenCalledWith('202601', 1)
    })

    await waitFor(() => {
      expect(screen.getByText('測試產品1')).toBeInTheDocument()
      expect(screen.getByText('測試產品2')).toBeInTheDocument()
    })
  })

  it('highlights modified rows', async () => {
    const closedConfigs = [{ id: 1, month: '202601', autoCloseDay: 10, isClosed: true, closedAt: '2026-01-10T10:00:00' }]
    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: mockForecastData, channel_order: [] })

    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.getByText('測試產品2')).toBeInTheDocument()
    })
  })

  it('shows readonly banner for closed month (sales role)', async () => {
    const closedConfigs = [
      {
        id: 1,
        month: '202601',
        autoCloseDay: 10,
        isClosed: true,
        closedAt: '2026-01-10T10:00:00',
      },
    ]

    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: mockForecastData, channel_order: [] })

    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], roleCode: 'sales', channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.getByText('總體銷售預估量表單結果')).toBeInTheDocument()
      expect(screen.getByText('測試產品1')).toBeInTheDocument()
    })
  })

  it('does not show readonly banner for production planner on closed month', async () => {
    const closedConfigs = [
      {
        id: 1,
        month: '202601',
        autoCloseDay: 10,
        isClosed: true,
        closedAt: '2026-01-10T10:00:00',
      },
    ]

    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: mockForecastData, channel_order: [] })

    renderPage({
      permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed', 'sales_forecast.edit'],
      roleCode: 'production_planner',
      channels: ['家樂福'],
    })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.queryByText('月份已關帳，資料為唯讀')).not.toBeInTheDocument()
    })
  })

  it('shows add button with create permission', async () => {
    const closedConfigs = [{ id: 1, month: '202601', autoCloseDay: 10, isClosed: true, closedAt: '2026-01-10T10:00:00' }]
    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: mockForecastData, channel_order: [] })

    renderPage({
      permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed', 'sales_forecast.create'],
      channels: ['家樂福'],
    })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.getByText('測試產品1')).toBeInTheDocument()
    })
    expect(screen.getByText('測試產品2')).toBeInTheDocument()
  })

  it('switches to cross-month tab (F127)', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'] })
    await waitFor(() => expect(screen.getByLabelText('月份')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('tab', { name: '跨月彙整' }))

    expect(screen.getByRole('tab', { name: '跨月彙整' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByLabelText('起始月份')).toBeInTheDocument()
    expect(screen.queryByLabelText('月份')).not.toBeInTheDocument()
  })

  it('shows empty state when no data', async () => {
    const closedConfigs = [{ id: 1, month: '202601', autoCloseDay: 10, isClosed: true, closedAt: '2026-01-10T10:00:00' }]
    listConfigs.mockResolvedValue(closedConfigs)
    getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-01-15T10:00:00' }])
    getFormSummary.mockResolvedValue({ rows: [], channel_order: [] })

    renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await userEvent.selectOptions(monthSelect, '202601')
    await userEvent.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.getByText('該月份尚無預估資料')).toBeInTheDocument()
    })
  })

  describe('per-channel remarks (F126)', () => {
    const closedConfigs = [{ id: 1, month: '202612', autoCloseDay: 10, isClosed: true, closedAt: '2026-12-10T10:00:00' }]
    const rowWithRemarks = {
      productCode: 'P001',
      productName: '中元禮盒',
      channel_cells: [{ current_qty: 100, previous_qty: 100, current_sales_qty: 100 }],
      remarks: [
        { channel: '全聯', month: '202612', text: '12月替換新品' },
        { channel: '7-11', month: '202612', text: '9/30前需生產完成' },
      ],
    }

    async function openSummary() {
      listConfigs.mockResolvedValue(closedConfigs)
      getFormVersions.mockResolvedValue([{ version_no: 1, created_at: '2026-12-15T10:00:00' }])
      getFormSummary.mockResolvedValue({ rows: [rowWithRemarks], channel_order: ['PX + 大全聯'], version_no: 1 })
      renderPage({ permissions: ['sales_forecast.view', 'sales_forecast.update_after_closed'], roleCode: 'production_planner' })
      await waitFor(() => expect(screen.getByLabelText('月份')).toBeInTheDocument())
      await userEvent.selectOptions(screen.getByLabelText('月份'), '202612')
      await userEvent.click(screen.getByRole('button', { name: '查詢' }))
      await waitFor(() => expect(screen.getByText('中元禮盒')).toBeInTheDocument())
    }

    it('shows remark button and opens modal listing each channel remark with [channel/month] label', async () => {
      await openSummary()
      expect(screen.queryByText('[全聯/12月]')).not.toBeInTheDocument()
      await userEvent.click(screen.getByRole('button', { name: '備註' }))
      expect(screen.getByRole('dialog', { name: '備註' })).toBeInTheDocument()
      expect(screen.getByText('[全聯/12月]')).toBeInTheDocument()
      expect(screen.getByText('[7-11/12月]')).toBeInTheDocument()
      expect(screen.getByText(/12月替換新品/)).toBeInTheDocument()
      expect(screen.getByText(/9\/30前需生產完成/)).toBeInTheDocument()
    })

    it('edits a single channel remark and sends per-channel remarks on save', async () => {
      saveFormSummaryVersion.mockResolvedValue({ version_no: 2 })
      await openSummary()

      await userEvent.click(screen.getByRole('button', { name: '編輯' }))
      await userEvent.click(screen.getByRole('button', { name: '備註' }))
      const input711 = screen.getByLabelText('[7-11/12月] 備註')
      await userEvent.clear(input711)
      await userEvent.type(input711, '改為10/15前')
      await userEvent.click(screen.getByRole('button', { name: '關閉' }))
      await userEvent.click(screen.getByRole('button', { name: '儲存' }))
      await userEvent.type(screen.getByLabelText(/生管備註（修改原因）/), '家樂福加量')
      await userEvent.click(screen.getByRole('button', { name: '是' }))

      await waitFor(() => expect(saveFormSummaryVersion).toHaveBeenCalled())
      const payload = saveFormSummaryVersion.mock.calls[0][1]
      expect(payload.change_reason).toBe('家樂福加量')
      expect(payload.rows[0].remarks).toEqual([
        { channel: '全聯', text: '12月替換新品' },
        { channel: '7-11', text: '改為10/15前' },
      ])
    })

    it('修改原因空白時不能確認儲存', async () => {
      await openSummary()
      await userEvent.click(screen.getByRole('button', { name: '編輯' }))
      await userEvent.click(screen.getByRole('button', { name: '儲存' }))
      expect(screen.getByRole('button', { name: '是' })).toBeDisabled()
      await userEvent.type(screen.getByLabelText(/生管備註（修改原因）/), '   ')
      expect(screen.getByRole('button', { name: '是' })).toBeDisabled()
      expect(saveFormSummaryVersion).not.toHaveBeenCalled()
    })
  })
})
