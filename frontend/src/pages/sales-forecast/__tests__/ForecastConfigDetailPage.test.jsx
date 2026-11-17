import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastConfigDetailPage from '../ForecastConfigDetailPage'

vi.mock('../../../api/forecastConfig', () => ({
  getFormDetail: vi.fn(),
  getFormLogs: vi.fn(),
  endReopen: vi.fn(),
  reopenChannels: vi.fn(),
}))

import { getFormDetail, getFormLogs, endReopen } from '../../../api/forecastConfig'

const detail = {
  config: { id: 7, month: '202612', autoCloseAt: '2026-12-10T00:00:00', isClosed: true, closedAt: '2026-12-09T16:00:00' },
  closeType: 'AUTO',
  reopenMaxExpiresAt: '2026-12-15T08:00:00',
  channels: [
    { channel: '家樂福', salesLastUploadAt: '2026-12-08T09:40:00', giftLastUploadAt: null,
      activeWindow: { id: 3, expiresAt: '2026-12-12T18:00:00', reason: '新品追加' }, correctionCount: 2 },
    { channel: '7-11', salesLastUploadAt: null, giftLastUploadAt: null, activeWindow: null, correctionCount: 0 },
  ],
}

const logs = [
  { id: 2, createdAt: '2026-12-11T09:15:00', operatorName: '陳美玲', action: 'REOPEN', channel: '家樂福', detail: '期限 2026/12/12 18:00；原因：新品追加' },
  { id: 1, createdAt: '2026-12-10T00:00:00', operatorName: '系統', action: 'AUTO_CLOSE', channel: null, detail: '到達截止時間 2026/12/10 00:00' },
]

function renderPage(permissions = ['sales_forecast_config.view', 'sales_forecast_config.reopen']) {
  const authValue = {
    token: 't',
    user: { username: 'pm', fullName: '陳美玲', roleCode: 'production_planner', permissions },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast/config/7']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <Routes>
            <Route path="/sales-forecast/config/:id" element={<ForecastConfigDetailPage />} />
          </Routes>
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastConfigDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getFormDetail.mockResolvedValue(detail)
    getFormLogs.mockResolvedValue(logs)
  })

  it('顯示狀態、各通路上傳與補登、異動紀錄', async () => {
    renderPage()
    expect(await screen.findByText(/> 202612/)).toBeInTheDocument()
    expect(getFormDetail).toHaveBeenCalledWith('7')
    expect(screen.getByText('已結束新增設定')).toBeInTheDocument()
    expect(screen.getByText(/系統自動截止/)).toBeInTheDocument()
    expect(screen.getByText('2026/12/08 17:40:00')).toBeInTheDocument()
    expect(screen.getAllByText('未上傳')).toHaveLength(3)
    expect(screen.getByText(/補登中，至 2026\/12\/12 18:00/)).toBeInTheDocument()
    expect(screen.getByText('2 筆')).toBeInTheDocument()
    const logTable = screen.getByRole('table', { name: '異動紀錄' })
    expect(within(logTable).getByText('開放補登')).toBeInTheDocument()
    expect(within(logTable).getByText('自動截止')).toBeInTheDocument()
    expect(within(logTable).getByText('全部')).toBeInTheDocument()
  })

  it('有權限 35 才顯示開放補登與提前結束', async () => {
    renderPage(['sales_forecast_config.view'])
    await screen.findByText(/> 202612/)
    expect(screen.queryByRole('button', { name: '開放補登' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '提前結束' })).not.toBeInTheDocument()
  })

  it('開放補登按鈕開啟視窗', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '開放補登' }))
    expect(screen.getByRole('dialog', { name: '開放補登 – 202612' })).toBeInTheDocument()
  })

  it('提前結束後重新載入', async () => {
    endReopen.mockResolvedValue()
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '提前結束' }))
    await waitFor(() => expect(endReopen).toHaveBeenCalledWith('7', 3))
    await waitFor(() => expect(getFormDetail).toHaveBeenCalledTimes(2))
  })

  it('403 顯示無權限', async () => {
    getFormDetail.mockRejectedValue({ response: { status: 403 } })
    renderPage()
    expect(await screen.findByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })
})
