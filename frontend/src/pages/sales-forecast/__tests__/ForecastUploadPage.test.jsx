import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastUploadPage from '../ForecastUploadPage'

vi.mock('../../../api/forecastConfig', () => ({
  listConfigs: vi.fn(),
}))

vi.mock('../../../api/forecast', () => ({
  uploadForecast: vi.fn(),
  downloadTemplate: vi.fn(),
  getForecastVersions: vi.fn(),
  getForecastList: vi.fn(),
  updateForecastItem: vi.fn(),
  createForecastItem: vi.fn(),
  copyVersion: vi.fn(),
  saveVersionReason: vi.fn(),
  deleteVersion: vi.fn(),
  getVersionDiff: vi.fn(),
}))

import { listConfigs } from '../../../api/forecastConfig'
import {
  uploadForecast,
  downloadTemplate,
  getForecastVersions,
  getForecastList,
  updateForecastItem,
} from '../../../api/forecast'

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
  {
    id: 3,
    month: '202512',
    autoCloseDay: 20,
    isClosed: true,
    closedAt: '2025-12-20T10:00:00',
  },
]

async function selectMonthChannelAndQuery(user, { month = '202601', channel = '家樂福' } = {}) {
  await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
  await user.selectOptions(screen.getByLabelText('月份', { exact: false }), month)
  await user.selectOptions(screen.getByLabelText('通路', { exact: false }), channel)
  await user.click(screen.getByRole('button', { name: '查詢' }))
}

function renderPage({ permissions = [], roleCode = 'sales', channels = [] } = {}) {
  const authValue = {
    token: 'test-token',
    user: { username: 'sales', fullName: 'Sales', roleCode, permissions, channels },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast/upload']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <ForecastUploadPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastUploadPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getForecastVersions.mockResolvedValue([])
    getForecastList.mockResolvedValue([])
    updateForecastItem.mockResolvedValue({})
    downloadTemplate.mockResolvedValue()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders loading state during fetch', () => {
    listConfigs.mockImplementation(() => new Promise(() => {}))
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('shows access denied without permission', async () => {
    listConfigs.mockResolvedValue([])
    renderPage({ permissions: [], roleCode: 'viewer' })

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('allows access with only update_after_closed permission', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.update_after_closed'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: '銷售預估量表單上傳' })).toBeInTheDocument()
    })
    expect(screen.queryByText('您沒有權限檢視此頁面')).not.toBeInTheDocument()
  })

  it('shows access denied when listConfigs returns 403', async () => {
    listConfigs.mockRejectedValue({ response: { status: 403 } })
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('toasts when listConfigs fails without 403', async () => {
    listConfigs.mockRejectedValue(new Error('network'))
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByText('無法載入月份設定')).toBeInTheDocument()
    })
  })

  it('runs query and loads versions and list', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1', 'v0'])
    getForecastList.mockResolvedValue([
      {
        id: 1,
        category: 'c',
        spec: 's',
        productCode: 'P1',
        productName: 'N1',
        warehouseLocation: 'W',
        quantity: 5,
      },
    ])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument()
    })
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    await user.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(getForecastVersions).toHaveBeenCalledWith('202601', '家樂福')
      expect(getForecastList).toHaveBeenCalled()
      expect(screen.getByText('N1')).toBeInTheDocument()
    })
  })

  it('toasts query failure when getForecastVersions rejects', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockRejectedValue(new Error('fail'))
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    await user.click(screen.getByRole('button', { name: '查詢' }))

    await waitFor(() => {
      expect(screen.getByText('查詢失敗')).toBeInTheDocument()
    })
  })

  it('downloads CSV template when button clicked', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    const createObjectURL = vi.fn().mockReturnValue('blob:csv')
    const revokeObjectURL = vi.fn()
    const origCreateObjectURL = global.URL.createObjectURL
    const origRevokeObjectURL = global.URL.revokeObjectURL
    global.URL.createObjectURL = createObjectURL
    global.URL.revokeObjectURL = revokeObjectURL

    try {
      renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })
      await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
      await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
      await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
      await user.click(screen.getByRole('button', { name: '下載 CSV 範本' }))

      expect(createObjectURL).toHaveBeenCalled()
      expect(revokeObjectURL).toHaveBeenCalled()
    } finally {
      global.URL.createObjectURL = origCreateObjectURL
      global.URL.revokeObjectURL = origRevokeObjectURL
    }
  })

  it('shows upload closed hint for closed month', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202512')

    await waitFor(() => {
      expect(screen.getByText(/該月份已結束新增設定，無法上傳/)).toBeInTheDocument()
    })
  })

  it('channel dropdown shows only user channels without sales_forecast.view', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['7-11'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')

    expect(screen.getByRole('option', { name: '7-11' })).toBeInTheDocument()
    expect(screen.queryByRole('option', { name: '家樂福' })).not.toBeInTheDocument()
  })

  it('renders form with open months only', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    expect(monthSelect).toBeInTheDocument()
    expect(screen.getByText('202601 (January 2026)')).toBeInTheDocument()
    expect(screen.getByText('202602 (February 2026)')).toBeInTheDocument()
    expect(screen.queryByText('202512')).not.toBeInTheDocument()
  })

  it('shows user authorized channels only', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福', '7-11'] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路', { exact: false })).toBeInTheDocument()
    })

    const channelSelect = screen.getByLabelText('通路', { exact: false })
    expect(channelSelect).toBeInTheDocument()
    expect(screen.getByText('家樂福')).toBeInTheDocument()
    expect(screen.getByText('7-11')).toBeInTheDocument()
  })

  it('shows all channels for admin', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], roleCode: 'admin', channels: [] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路', { exact: false })).toBeInTheDocument()
    })

    const channelSelect = screen.getByLabelText('通路', { exact: false })
    const options = channelSelect.querySelectorAll('option')
    expect(options.length).toBeGreaterThanOrEqual(1)
  })

  it('disables upload button until all fields filled', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByText('上傳')).toBeInTheDocument()
    })

    const uploadBtn = screen.getByText('上傳')
    expect(uploadBtn).toBeDisabled()
  })

  it('enables download template button when channel selected', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路', { exact: false })).toBeInTheDocument()
    })

    const downloadBtn = screen.getByText('下載 Excel 範本')
    expect(downloadBtn).toBeDisabled()

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    await user.selectOptions(monthSelect, '202601')

    const channelSelect = screen.getByLabelText('通路', { exact: false })
    await user.selectOptions(channelSelect, '家樂福')

    expect(downloadBtn).not.toBeDisabled()
  })

  it('calls downloadTemplate on button click', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路', { exact: false })).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    await user.selectOptions(monthSelect, '202601')

    const channelSelect = screen.getByLabelText('通路', { exact: false })
    await user.selectOptions(channelSelect, '家樂福')

    const downloadBtn = screen.getByText('下載 Excel 範本')
    await user.click(downloadBtn)

    expect(downloadTemplate).toHaveBeenCalledWith('家樂福')
  })

  it('validates file type', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('上傳檔案區域')).toBeInTheDocument()
    })

    const invalidFile = new File(['test'], 'test.pdf', { type: 'application/pdf' })
    const fileInput = document.querySelector('input[type="file"]')

    Object.defineProperty(fileInput, 'files', {
      value: [invalidFile],
      writable: false,
    })
    fileInput.dispatchEvent(new Event('change', { bubbles: true }))

    await waitFor(() => {
      expect(screen.getByText(/請上傳 Excel \(\.xlsx\) 或 CSV 檔案/)).toBeInTheDocument()
    })
  })

  it('validates file size', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('上傳檔案區域')).toBeInTheDocument()
    })

    const largeFile = new File([new ArrayBuffer(11 * 1024 * 1024)], 'large.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const fileInput = document.querySelector('input[type="file"]')

    Object.defineProperty(fileInput, 'files', {
      value: [largeFile],
      writable: false,
    })
    fileInput.dispatchEvent(new Event('change', { bubbles: true }))

    await waitFor(() => {
      expect(screen.getByText('檔案大小超過 10MB 上限')).toBeInTheDocument()
    })
  })

  it('accepts valid file', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('上傳檔案區域')).toBeInTheDocument()
    })

    const validFile = new File(['test'], 'test.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const fileInput = document.querySelector('input[type="file"]')

    Object.defineProperty(fileInput, 'files', {
      value: [validFile],
      writable: false,
    })
    fileInput.dispatchEvent(new Event('change', { bubbles: true }))

    await waitFor(() => {
      expect(screen.getByText('test.xlsx')).toBeInTheDocument()
    })
  })

  it('calls uploadForecast with correct parameters', async () => {
    const user = userEvent.setup()
    uploadForecast.mockResolvedValue({ rows_processed: 100 })
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    const channelSelect = screen.getByLabelText('通路', { exact: false })
    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['test'], 'test.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })

    await user.selectOptions(monthSelect, '202601')
    await user.selectOptions(channelSelect, '家樂福')
    await user.upload(fileInput, validFile)

    await waitFor(() => {
      expect(screen.getByText('上傳')).not.toBeDisabled()
    })

    const uploadBtn = screen.getByText('上傳')
    await user.click(uploadBtn)

    await waitFor(() => {
      expect(uploadForecast).toHaveBeenCalledWith(validFile, '202601', '家樂福')
    })
  })

  it('shows success message and clears form after upload', async () => {
    const user = userEvent.setup()
    uploadForecast.mockResolvedValue({ rows_processed: 100 })
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    const channelSelect = screen.getByLabelText('通路', { exact: false })
    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['test'], 'test.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })

    await user.selectOptions(monthSelect, '202601')
    await user.selectOptions(channelSelect, '家樂福')
    await user.upload(fileInput, validFile)

    const uploadBtn = screen.getByText('上傳')
    await user.click(uploadBtn)

    await waitFor(() => {
      expect(screen.getByText('成功上傳 100 筆資料')).toBeInTheDocument()
    })

    expect(monthSelect.value).toBe('202601')
    expect(channelSelect.value).toBe('家樂福')
  })

  it('shows error message on upload failure', async () => {
    const user = userEvent.setup()
    uploadForecast.mockRejectedValue({
      response: { data: { details: ['錯誤訊息'] } },
    })
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份', { exact: false })
    const channelSelect = screen.getByLabelText('通路', { exact: false })
    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['test'], 'test.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })

    await user.selectOptions(monthSelect, '202601')
    await user.selectOptions(channelSelect, '家樂福')
    await user.upload(fileInput, validFile)

    const uploadBtn = screen.getByText('上傳')
    await user.click(uploadBtn)

    await waitFor(() => {
      expect(screen.getByText('錯誤訊息')).toBeInTheDocument()
    })
  })

  it('maps object-shaped versions from getForecastVersions', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue([{ version: 'rv1' }, { version: 'rv0' }])
    getForecastList.mockResolvedValue([{ id: 1, productCode: 'X', quantity: 1 }])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => {
      expect(getForecastList).toHaveBeenCalledWith('202601', '家樂福', 'rv1')
    })
  })

  it('calls getForecastList without version when no versions exist', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue([])
    getForecastList.mockResolvedValue([])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => {
      expect(getForecastList).toHaveBeenCalledWith('202601', '家樂福', undefined)
    })
  })

  it('toasts when getForecastList fails during query', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockRejectedValue(new Error('list down'))
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => {
      const toasts = screen.getAllByText('無法載入上傳結果')
      expect(toasts.length).toBeGreaterThanOrEqual(1)
    })
  })

  it('toasts when refetch after version select fails', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    let n = 0
    getForecastList.mockImplementation(() => {
      n += 1
      if (n === 1) {
        return Promise.resolve([
          {
            id: 9,
            category: 'c',
            spec: 's',
            productCode: 'KEEP',
            productName: 'N',
            warehouseLocation: 'W',
            quantity: 2,
          },
        ])
      }
      return Promise.reject(new Error('refetch'))
    })
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => {
      expect(screen.getByText('無法載入上傳結果')).toBeInTheDocument()
    })
    await waitFor(() => {
      expect(screen.getByText('尚無上傳資料')).toBeInTheDocument()
    })
  })

  it('toasts downloadTemplate error with Blob details', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    const body = JSON.stringify({ details: ['原因一', '原因二'] })
    const blob = new Blob([body], { type: 'application/json' })
    downloadTemplate.mockRejectedValue({ message: 'bad', response: { data: blob } })
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    await user.click(screen.getByRole('button', { name: '下載 Excel 範本' }))

    await waitFor(() => {
      expect(screen.getByText('原因一; 原因二')).toBeInTheDocument()
    })
  })

  it('toasts downloadTemplate error with Blob message field', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    const body = JSON.stringify({ message: '範本維護中' })
    const blob = new Blob([body], { type: 'application/json' })
    downloadTemplate.mockRejectedValue({ message: 'x', response: { data: blob } })
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    await user.click(screen.getByRole('button', { name: '下載 Excel 範本' }))

    await waitFor(() => {
      expect(screen.getByText('範本維護中')).toBeInTheDocument()
    })
  })

  it('toasts generic message when upload returns error field', async () => {
    const user = userEvent.setup()
    uploadForecast.mockRejectedValue({
      response: { data: { error: '伺服器拒絕' } },
    })
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['x'], 'f.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    await user.upload(fileInput, validFile)
    await user.click(screen.getByRole('button', { name: '上傳' }))

    await waitFor(() => {
      expect(screen.getByText('伺服器拒絕')).toBeInTheDocument()
    })
  })

  it('toasts generic upload failure when response has no details', async () => {
    const user = userEvent.setup()
    uploadForecast.mockRejectedValue(new Error('timeout'))
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('月份', { exact: false })).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')
    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['x'], 'f.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    await user.upload(fileInput, validFile)
    await user.click(screen.getByRole('button', { name: '上傳' }))

    await waitFor(() => {
      expect(screen.getByText('上傳失敗，請重試')).toBeInTheDocument()
    })
  })

  it('shows readonly banner for closed month after query', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user, { month: '202512', channel: '家樂福' })

    await waitFor(() => {
      expect(
        screen.getByText('該月份已結束新增設定，僅可檢視不可編輯與新增。')
      ).toBeInTheDocument()
    })
  })

  it('paginates result table', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    const rows = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      category: '',
      spec: '',
      productCode: `P${i + 1}`,
      productName: '',
      warehouseLocation: '',
      quantity: 1,
    }))
    getForecastList.mockResolvedValue(rows)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('P1')).toBeInTheDocument())
    expect(screen.queryByText('P11')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: '下一頁' }))

    await waitFor(() => {
      expect(screen.getByText('P11')).toBeInTheDocument()
      expect(screen.getByText('P12')).toBeInTheDocument()
    })
  })

  it('changes page size from pagination control', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    const rows = Array.from({ length: 12 }, (_, i) => ({
      id: i + 1,
      productCode: `PX${i + 1}`,
      quantity: 1,
    }))
    getForecastList.mockResolvedValue(rows)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('PX1')).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('每頁筆數'), '20')

    await waitFor(() => {
      expect(screen.getByText(/第 1–12 筆，共 12 筆/)).toBeInTheDocument()
    })
  })

  it('exports CSV and revokes object URL', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      {
        id: 1,
        category: 'C',
        spec: 'S',
        productCode: 'PC1',
        productName: 'Name,WithComma',
        warehouseLocation: 'W',
        quantity: 3,
      },
    ])
    const createObjectURL = vi.fn().mockReturnValue('blob:export')
    const revokeObjectURL = vi.fn()
    const origCreate = global.URL.createObjectURL
    const origRevoke = global.URL.revokeObjectURL
    global.URL.createObjectURL = createObjectURL
    global.URL.revokeObjectURL = revokeObjectURL

    try {
      renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })
      await selectMonthChannelAndQuery(user)

      await waitFor(() => expect(screen.getByText('PC1')).toBeInTheDocument())
      await user.click(screen.getByRole('button', { name: '匯出 Excel (CSV)' }))

      await waitFor(() => {
        expect(createObjectURL).toHaveBeenCalled()
        expect(revokeObjectURL).toHaveBeenCalled()
        expect(screen.getByText('已匯出 CSV')).toBeInTheDocument()
      })
    } finally {
      global.URL.createObjectURL = origCreate
      global.URL.revokeObjectURL = origRevoke
    }
  })

  it('clears selected file with remove button', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => expect(screen.getByLabelText('上傳檔案區域')).toBeInTheDocument())
    await user.selectOptions(screen.getByLabelText('月份', { exact: false }), '202601')
    await user.selectOptions(screen.getByLabelText('通路', { exact: false }), '家樂福')

    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['x'], 'keep.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    await user.upload(fileInput, validFile)

    await waitFor(() => expect(screen.getByText('keep.xlsx')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '移除檔案' }))

    await waitFor(() => {
      expect(screen.queryByText('keep.xlsx')).not.toBeInTheDocument()
    })
  })

  it('saves inline edit when user has edit permission', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      {
        id: 44,
        category: 'c',
        spec: 's',
        productCode: 'ED1',
        productName: 'n',
        warehouseLocation: 'w',
        quantity: 5,
      },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('ED1')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))

    const input = screen.getByRole('spinbutton')
    await user.clear(input)
    await user.type(input, '88')
    await user.click(screen.getByRole('button', { name: '儲存' }))

    await waitFor(() => {
      expect(updateForecastItem).toHaveBeenCalledWith(44, { quantity: 88 })
      expect(screen.getByText('已儲存')).toBeInTheDocument()
    })
  })

  it('toasts when quantity is invalid on save', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 55, productCode: 'E2', quantity: 1, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('E2')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))
    const input = screen.getByRole('spinbutton')
    await user.clear(input)
    await user.type(input, '-3')
    await user.click(screen.getByRole('button', { name: '儲存' }))

    await waitFor(() => {
      expect(screen.getByText('箱數小計不可為負數')).toBeInTheDocument()
    })
    expect(updateForecastItem).not.toHaveBeenCalled()
  })

  it('toasts when updateForecastItem fails', async () => {
    const user = userEvent.setup()
    updateForecastItem.mockRejectedValue({
      response: { data: { error: '版本鎖定' } },
    })
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 66, productCode: 'E3', quantity: 1, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('E3')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))
    await user.click(screen.getByRole('button', { name: '儲存' }))

    await waitFor(() => {
      expect(screen.getByText('版本鎖定')).toBeInTheDocument()
    })
  })

  it('cancels inline edit', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 77, productCode: 'E4', quantity: 9, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('E4')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))
    await user.click(screen.getByRole('button', { name: '取消' }))

    await waitFor(() => {
      expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: '編輯' })).toBeInTheDocument()
    })
  })

  it('saves inline edit on Enter key', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 88, productCode: 'E5', quantity: 2, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('E5')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))
    const input = await screen.findByRole('spinbutton')
    await user.click(input)
    await user.clear(input)
    await user.type(input, '11')
    await user.keyboard('{Enter}')

    await waitFor(() => {
      expect(updateForecastItem).toHaveBeenCalledWith(88, { quantity: 11 })
    })
  })

  it('cancels inline edit on Escape', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 99, productCode: 'E6', quantity: 2, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({
      permissions: ['sales_forecast.upload', 'sales_forecast.edit'],
      channels: ['家樂福'],
    })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(screen.getByText('E6')).toBeInTheDocument())
    await user.click(screen.getByRole('button', { name: '編輯' }))
    const input = await screen.findByRole('spinbutton')
    await user.click(input)
    await user.keyboard('{Escape}')

    await waitFor(() => {
      expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
    })
  })

  it('marks zero quantity cell styling class', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 100, productCode: 'Z0', quantity: 0, category: '', spec: '', productName: '', warehouseLocation: '' },
    ])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => {
      const row = screen.getByText('Z0').closest('tr')
      const qtyCell = within(row).getByText('0').closest('td')
      expect(qtyCell?.className).toMatch(/quantity-cell--zero/)
    })
  })
})