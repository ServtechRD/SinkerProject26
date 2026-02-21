import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../axios', () => {
  return {
    default: {
      get: vi.fn(),
    },
  }
})

import api from '../axios'
import { getMaterialDemand } from '../materialDemand'

describe('materialDemand API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getMaterialDemand calls GET /api/material-demand with week_start and factory', async () => {
    const mockData = [
      {
        materialCode: 'MAT001',
        materialName: 'Material 1',
        unit: 'kg',
        lastPurchaseDate: '2026-02-15',
        demandDate: '2026-02-20',
        expectedDelivery: 100.50,
        demandQuantity: 150.00,
        estimatedInventory: 80.25,
      },
      {
        materialCode: 'MAT002',
        materialName: 'Material 2',
        unit: 'pcs',
        lastPurchaseDate: null,
        demandDate: '2026-02-21',
        expectedDelivery: 200.00,
        demandQuantity: 180.00,
        estimatedInventory: 220.00,
      },
    ]
    api.get.mockResolvedValue({ data: mockData })

    const result = await getMaterialDemand('2026-02-17', 'F1')

    expect(api.get).toHaveBeenCalledWith('/api/material-demand?week_start=2026-02-17&factory=F1')
    expect(result).toEqual(mockData)
  })

  it('propagates network errors', async () => {
    api.get.mockRejectedValue(new Error('Network Error'))

    await expect(getMaterialDemand('2026-02-17', 'F1')).rejects.toThrow('Network Error')
  })

  it('propagates API errors', async () => {
    const error = { response: { status: 403, data: { error: 'Forbidden' } } }
    api.get.mockRejectedValue(error)

    await expect(getMaterialDemand('2026-02-17', 'F1')).rejects.toEqual(error)
  })

  it('handles empty data response', async () => {
    api.get.mockResolvedValue({ data: [] })

    const result = await getMaterialDemand('2026-02-17', 'F2')

    expect(api.get).toHaveBeenCalledWith('/api/material-demand?week_start=2026-02-17&factory=F2')
    expect(result).toEqual([])
  })
})
