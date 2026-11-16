import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../axios', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

import api from '../axios'
import {
  listConfigs,
  createMonth,
  updateConfig,
  getFormDetail,
  getFormLogs,
  reopenChannels,
  endReopen,
  getEditStatus,
} from '../forecastConfig'

describe('forecastConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({ data: 'ok' })
    api.post.mockResolvedValue({ data: 'ok' })
    api.put.mockResolvedValue({ data: 'ok' })
  })

  it('listConfigs', async () => {
    expect(await listConfigs()).toBe('ok')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config')
  })

  it('createMonth 送出月份與截止時間', async () => {
    await createMonth('202612', '2026-12-10T18:30')
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config', { month: '202612', autoCloseAt: '2026-12-10T18:30' })
  })

  it('updateConfig', async () => {
    await updateConfig(1, { autoCloseAt: '2026-12-11T18:00', isClosed: true })
    expect(api.put).toHaveBeenCalledWith('/api/sales-forecast/config/1', { autoCloseAt: '2026-12-11T18:00', isClosed: true })
  })

  it('getFormDetail／getFormLogs', async () => {
    await getFormDetail(7)
    await getFormLogs(7)
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/7/detail')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/7/logs')
  })

  it('reopenChannels／endReopen', async () => {
    const payload = { channels: ['家樂福'], expiresAt: '2026-12-12T18:00', reason: '新品追加' }
    await reopenChannels(7, payload)
    await endReopen(7, 3)
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config/7/reopen', payload)
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config/7/reopen/3/end')
  })

  it('getEditStatus 以 query 帶月份與通路', async () => {
    await getEditStatus('202612', '家樂福')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/edit-status', { params: { month: '202612', channel: '家樂福' } })
  })
})
