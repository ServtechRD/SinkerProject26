import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../axios', () => {
  return {
    default: {
      get: vi.fn(),
      put: vi.fn(),
    },
  }
})

import api from '../axios'
import { getProductionPlan, updateProductionPlanItem } from '../productionPlan'

describe('productionPlan API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getProductionPlan calls GET /api/production-plan with year', async () => {
    const mockData = [
      {
        id: 1,
        productCode: 'PROD001',
        productName: 'Product 1',
        category: 'A',
        spec: 'S1',
        warehouseLocation: 'WH1',
        channel: 'Direct',
        monthlyAllocations: { '2': 100, '3': 150 },
        buffer: 50,
        forecast: 2300,
        remarks: 'Test',
      },
    ]
    api.get.mockResolvedValue({ data: mockData })

    const result = await getProductionPlan(2026)

    expect(api.get).toHaveBeenCalledWith('/api/production-plan?year=2026')
    expect(result).toEqual(mockData)
  })

  it('updateProductionPlanItem calls PUT with correct payload', async () => {
    const mockResponse = {
      id: 1,
      productCode: 'PROD001',
      productName: 'Product 1',
      channel: 'Direct',
      monthlyAllocations: { '2': 200, '3': 150 },
      buffer: 60,
      remarks: 'Updated',
    }
    api.put.mockResolvedValue({ data: mockResponse })

    const result = await updateProductionPlanItem(1, {
      monthlyAllocations: { '2': 200, '3': 150 },
      buffer: 60,
      remarks: 'Updated',
    })

    expect(api.put).toHaveBeenCalledWith('/api/production-plan/1', {
      monthlyAllocations: { '2': 200, '3': 150 },
      buffer: 60,
      remarks: 'Updated',
    })
    expect(result).toEqual(mockResponse)
  })

  it('propagates network errors', async () => {
    api.get.mockRejectedValue(new Error('Network Error'))

    await expect(getProductionPlan(2026)).rejects.toThrow('Network Error')
  })

  it('propagates API errors', async () => {
    const error = { response: { status: 403, data: { error: 'Forbidden' } } }
    api.get.mockRejectedValue(error)

    await expect(getProductionPlan(2026)).rejects.toEqual(error)
  })
})
