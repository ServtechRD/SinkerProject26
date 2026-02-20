import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
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
}))

import { listConfigs } from '../../../api/forecastConfig'
import { uploadForecast, downloadTemplate } from '../../../api/forecast'

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

  it('renders form with open months only', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    expect(monthSelect).toBeInTheDocument()
    expect(screen.getByText('202601 (January 2026)')).toBeInTheDocument()
    expect(screen.getByText('202602 (February 2026)')).toBeInTheDocument()
    expect(screen.queryByText('202512')).not.toBeInTheDocument()
  })

  it('shows user authorized channels only', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福', '7-11'] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路')).toBeInTheDocument()
    })

    const channelSelect = screen.getByLabelText('通路')
    expect(channelSelect).toBeInTheDocument()
    expect(screen.getByText('家樂福')).toBeInTheDocument()
    expect(screen.getByText('7-11')).toBeInTheDocument()
  })

  it('shows all channels for admin', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], roleCode: 'admin', channels: [] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路')).toBeInTheDocument()
    })

    const channelSelect = screen.getByLabelText('通路')
    const options = channelSelect.querySelectorAll('option')
    expect(options.length).toBeGreaterThan(12)
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
      expect(screen.getByLabelText('通路')).toBeInTheDocument()
    })

    const downloadBtn = screen.getByText('下載範本')
    expect(downloadBtn).toBeDisabled()

    const channelSelect = screen.getByLabelText('通路')
    await user.selectOptions(channelSelect, '家樂福')

    expect(downloadBtn).not.toBeDisabled()
  })

  it('calls downloadTemplate on button click', async () => {
    const user = userEvent.setup()
    downloadTemplate.mockResolvedValue()
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('通路')).toBeInTheDocument()
    })

    const channelSelect = screen.getByLabelText('通路')
    await user.selectOptions(channelSelect, '家樂福')

    const downloadBtn = screen.getByText('下載範本')
    await user.click(downloadBtn)

    expect(downloadTemplate).toHaveBeenCalledWith('家樂福')
  })

  it('validates file type', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('上傳檔案區域')).toBeInTheDocument()
    })

    const invalidFile = new File(['test'], 'test.csv', { type: 'text/csv' })
    const fileInput = document.querySelector('input[type="file"]')

    // Manually trigger the change event
    Object.defineProperty(fileInput, 'files', {
      value: [invalidFile],
      writable: false,
    })
    fileInput.dispatchEvent(new Event('change', { bubbles: true }))

    await waitFor(() => {
      expect(screen.getByText('請上傳有效的 Excel 檔案 (.xlsx)')).toBeInTheDocument()
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
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    const channelSelect = screen.getByLabelText('通路')
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
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    const channelSelect = screen.getByLabelText('通路')
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

    expect(monthSelect.value).toBe('')
    expect(channelSelect.value).toBe('')
  })

  it('shows error message on upload failure', async () => {
    const user = userEvent.setup()
    uploadForecast.mockRejectedValue({
      response: { data: { details: ['錯誤訊息'] } },
    })
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await waitFor(() => {
      expect(screen.getByLabelText('月份')).toBeInTheDocument()
    })

    const monthSelect = screen.getByLabelText('月份')
    const channelSelect = screen.getByLabelText('通路')
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
})
