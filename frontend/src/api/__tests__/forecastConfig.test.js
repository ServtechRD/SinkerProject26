import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../axios', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
    },
  }
})

import api from '../axios'
import { listConfigs, createMonths, updateConfig } from '../forecastConfig'

describe('forecastConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listConfigs calls GET /api/sales-forecast/config', async () => {
    const mockData = [
      { id: 1, month: '202601', autoCloseDay: 10, isClosed: false },
    ]
    api.get.mockResolvedValue({ data: mockData })

    const result = await listConfigs()

    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config')
    expect(result).toEqual(mockData)
  })

  it('createMonths calls POST with correct payload', async () => {
    const mockResponse = { createdCount: 3, months: ['202601', '202602', '202603'] }
    api.post.mockResolvedValue({ data: mockResponse })

    const result = await createMonths('202601', '202603')

    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config', {
      startMonth: '202601',
      endMonth: '202603',
    })
    expect(result).toEqual(mockResponse)
  })

  it('updateConfig calls PUT with correct payload', async () => {
    const mockResponse = { id: 1, month: '202601', autoCloseDay: 20, isClosed: true }
    api.put.mockResolvedValue({ data: mockResponse })

    const result = await updateConfig(1, { autoCloseDay: 20, isClosed: true })

    expect(api.put).toHaveBeenCalledWith('/api/sales-forecast/config/1', {
      autoCloseDay: 20,
      isClosed: true,
    })
    expect(result).toEqual(mockResponse)
  })

  it('propagates network errors', async () => {
    api.get.mockRejectedValue(new Error('Network Error'))

    await expect(listConfigs()).rejects.toThrow('Network Error')
  })
})
