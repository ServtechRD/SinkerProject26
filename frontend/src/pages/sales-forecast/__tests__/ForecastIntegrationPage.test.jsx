import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastIntegrationPage from '../ForecastIntegrationPage'

vi.mock('../../../api/forecastConfig', () => ({
  listConfigs: vi.fn(),
}))

vi.mock('../../../api/forecastIntegration', () => ({
  getIntegrationVersions: vi.fn(),
  getIntegrationData: vi.fn(),
  exportIntegrationExcel: vi.fn(),
}))

import { listConfigs } from '../../../api/forecastConfig'
import {
  getIntegrationVersions,
  getIntegrationData,
  exportIntegrationExcel,
} from '../../../api/forecastIntegration'

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

const mockIntegrationData = [
  {
    warehouseLocation: 'A1',
    category: '飲料',
    spec: '500ml',
    productName: '測試產品1',
    productCode: 'P001',
    pxDaQuanLian: 100,
    jiaLeFu: 50,
    aiMai: 30,
    sevenEleven: 40,
    quanJia: 20,
    okLaiErFu: 10,
    haoShiDuo: 60,
    fengKang: 25,
    meiLianShe: 15,
    kangShiMei: 12,
    dianShang: 80,
    shiFanJingXiao: 45,
    originalSubtotal: 487,
    difference: 10,
    remarks: '數量增加',
  },
  {
    warehouseLocation: 'B2',
    category: '食品',
    spec: '1kg',
    productName: '測試產品2',
    productCode: 'P002',
    pxDaQuanLian: 200,
    jiaLeFu: 100,
    aiMai: 60,
    sevenEleven: 80,
    quanJia: 40,
    okLaiErFu: 20,
    haoShiDuo: 120,
    fengKang: 50,
    meiLianShe: 30,
    kangShiMei: 24,
    dianShang: 160,
    shiFanJingXiao: 90,
    originalSubtotal: 974,
    difference: -20,
    remarks: '數量減少',
  },
]

function renderPage({ permissions = [], roleCode = 'sales' } = {}) {
  const authValue = {
    token: 'test-token',
    user: { username: 'sales', fullName: 'Sales', roleCode, permissions },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast/integration']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <ForecastIntegrationPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastIntegrationPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading state during fetch', () => {
    listConfigs.mockImplementation(() => new Promise(() => {}))
    renderPage({ permissions: ['sales_forecast.view'] })

    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows access denied without permission', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: [], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('renders page with month selector', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    expect(screen.getByText('銷售預測整合 - 12 通路')).toBeInTheDocument()
  })

  it('loads versions when month selected', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(getIntegrationVersions).toHaveBeenCalledWith('202601')
    })
  })

  it('loads integration data when month and version selected', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)
    getIntegrationData.mockResolvedValue(mockIntegrationData)

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(screen.getByLabelText('版本')).toBeInTheDocument()
    })

    const versionSelect = screen.getByLabelText('版本')
    await user.selectOptions(versionSelect, 'v2')

    await waitFor(() => {
      expect(getIntegrationData).toHaveBeenCalledWith('202601', 'v2')
    })

    expect(screen.getByText('測試產品1')).toBeInTheDocument()
    expect(screen.getByText('測試產品2')).toBeInTheDocument()
  })

  it('displays table with all 20 columns', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)
    getIntegrationData.mockResolvedValue(mockIntegrationData)

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(screen.getByLabelText('版本')).toBeInTheDocument()
    })

    const versionSelect = screen.getByLabelText('版本')
    await user.selectOptions(versionSelect, 'v2')

    await waitFor(() => {
      expect(screen.getByText('測試產品1')).toBeInTheDocument()
    })

    // Check all column headers exist
    expect(screen.getByText('庫位')).toBeInTheDocument()
    expect(screen.getByText('中類名稱')).toBeInTheDocument()
    expect(screen.getByText('貨品規格')).toBeInTheDocument()
    expect(screen.getByText('品名')).toBeInTheDocument()
    expect(screen.getByText('品號')).toBeInTheDocument()
    expect(screen.getByText('PX/大全聯')).toBeInTheDocument()
    expect(screen.getByText('家樂福')).toBeInTheDocument()
    expect(screen.getByText('愛買')).toBeInTheDocument()
    expect(screen.getByText('7-11')).toBeInTheDocument()
    expect(screen.getByText('全家')).toBeInTheDocument()
    expect(screen.getByText('OK/萊爾富')).toBeInTheDocument()
    expect(screen.getByText('好市多')).toBeInTheDocument()
    expect(screen.getByText('楓康')).toBeInTheDocument()
    expect(screen.getByText('美聯社')).toBeInTheDocument()
    expect(screen.getByText('康是美')).toBeInTheDocument()
    expect(screen.getByText('電商')).toBeInTheDocument()
    expect(screen.getByText('市面經銷')).toBeInTheDocument()
    expect(screen.getByText('原始小計')).toBeInTheDocument()
    expect(screen.getByText('差異')).toBeInTheDocument()
    expect(screen.getByText('備註')).toBeInTheDocument()
  })

  it('formats numbers with thousand separator', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)
    getIntegrationData.mockResolvedValue(mockIntegrationData)

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(screen.getByLabelText('版本')).toBeInTheDocument()
    })

    const versionSelect = screen.getByLabelText('版本')
    await user.selectOptions(versionSelect, 'v2')

    await waitFor(() => {
      expect(screen.getByText('100.00')).toBeInTheDocument()
    })

    // Check formatted numbers exist
    expect(screen.getByText('487.00')).toBeInTheDocument()
    expect(screen.getByText('974.00')).toBeInTheDocument()
  })

  it('calls export function when export button clicked', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)
    getIntegrationData.mockResolvedValue(mockIntegrationData)
    exportIntegrationExcel.mockResolvedValue()

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(screen.getByLabelText('版本')).toBeInTheDocument()
    })

    const versionSelect = screen.getByLabelText('版本')
    await user.selectOptions(versionSelect, 'v2')

    await waitFor(() => {
      expect(screen.getByText('匯出 Excel')).toBeInTheDocument()
    })

    const exportButton = screen.getByText('匯出 Excel')
    await user.click(exportButton)

    await waitFor(() => {
      expect(exportIntegrationExcel).toHaveBeenCalledWith('202601', 'v2')
    })
  })

  it('displays empty state when no data', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    getIntegrationVersions.mockResolvedValue(mockVersions)
    getIntegrationData.mockResolvedValue([])

    const user = userEvent.setup()
    renderPage({ permissions: ['sales_forecast.view'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    await user.selectOptions(monthSelect, '202601')

    await waitFor(() => {
      expect(screen.getByLabelText('版本')).toBeInTheDocument()
    })

    const versionSelect = screen.getByLabelText('版本')
    await user.selectOptions(versionSelect, 'v2')

    await waitFor(() => {
      expect(screen.getByText('所選月份與版本無整合資料')).toBeInTheDocument()
    })
  })
})
