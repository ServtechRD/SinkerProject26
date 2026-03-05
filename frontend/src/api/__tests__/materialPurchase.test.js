import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getMaterialPurchase, triggerErp } from '../materialPurchase'
import api from '../axios'

vi.mock('../axios', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

describe('materialPurchase API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getMaterialPurchase', () => {
    it('calls GET /api/material-purchase with correct parameters', async () => {
      const mockData = [
        {
          id: 1,
          productCode: 'PROD001',
          productName: 'Product 1',
          quantity: 100.50,
        },
      ]

      api.get.mockResolvedValue({ data: mockData })

      const result = await getMaterialPurchase('2026-02-17', 'F1')

      expect(api.get).toHaveBeenCalledWith('/api/material-purchase?week_start=2026-02-17&factory=F1')
      expect(result).toEqual(mockData)
    })

    it('handles API errors', async () => {
      api.get.mockRejectedValue(new Error('Network Error'))

      await expect(getMaterialPurchase('2026-02-17', 'F1')).rejects.toThrow('Network Error')
    })
  })

  describe('triggerErp', () => {
    it('calls POST /api/material-purchase/:id/trigger-erp with correct id', async () => {
      const mockData = {
        id: 1,
        productCode: 'PROD001',
        isErpTriggered: true,
        erpOrderNo: 'ERP-2026-001',
      }

      api.post.mockResolvedValue({ data: mockData })

      const result = await triggerErp(1)

      expect(api.post).toHaveBeenCalledWith('/api/material-purchase/1/trigger-erp')
      expect(result).toEqual(mockData)
    })

    it('handles 409 conflict error', async () => {
      api.post.mockRejectedValue({
        response: {
          status: 409,
          data: { message: 'Already triggered' },
        },
      })

      await expect(triggerErp(1)).rejects.toMatchObject({
        response: {
          status: 409,
        },
      })
    })

    it('handles API errors', async () => {
      api.post.mockRejectedValue(new Error('Network Error'))

      await expect(triggerErp(1)).rejects.toThrow('Network Error')
    })
  })
})
