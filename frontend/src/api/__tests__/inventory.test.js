import { describe, it, expect, vi, beforeEach } from 'vitest'
import { getInventoryIntegration, updateModifiedSubtotal } from '../inventory'
import api from '../axios'

vi.mock('../axios')

describe('inventory API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getInventoryIntegration', () => {
    it('should call API with month only', async () => {
      const mockData = [{ id: 1, productCode: 'P001' }]
      api.get.mockResolvedValue({ data: mockData })

      const result = await getInventoryIntegration('2026-01', null, null, null)

      expect(api.get).toHaveBeenCalledWith('/api/inventory-integration', {
        params: { month: '2026-01' }
      })
      expect(result).toEqual(mockData)
    })

    it('should call API with month and date range', async () => {
      const mockData = [{ id: 1, productCode: 'P001' }]
      api.get.mockResolvedValue({ data: mockData })

      const result = await getInventoryIntegration('2026-01', '2026-01-10', '2026-01-20', null)

      expect(api.get).toHaveBeenCalledWith('/api/inventory-integration', {
        params: {
          month: '2026-01',
          startDate: '2026-01-10',
          endDate: '2026-01-20'
        }
      })
      expect(result).toEqual(mockData)
    })

    it('should call API with month and version', async () => {
      const mockData = [{ id: 1, productCode: 'P001' }]
      api.get.mockResolvedValue({ data: mockData })

      const result = await getInventoryIntegration('2026-01', null, null, 'v20260115120000')

      expect(api.get).toHaveBeenCalledWith('/api/inventory-integration', {
        params: {
          month: '2026-01',
          version: 'v20260115120000'
        }
      })
      expect(result).toEqual(mockData)
    })
  })

  describe('updateModifiedSubtotal', () => {
    it('should call PUT API with id and value', async () => {
      const mockData = { id: 1, modifiedSubtotal: 200.50 }
      api.put.mockResolvedValue({ data: mockData })

      const result = await updateModifiedSubtotal(1, 200.50)

      expect(api.put).toHaveBeenCalledWith('/api/inventory-integration/1', {
        modifiedSubtotal: 200.50
      })
      expect(result).toEqual(mockData)
    })

    it('should handle null value', async () => {
      const mockData = { id: 1, modifiedSubtotal: null }
      api.put.mockResolvedValue({ data: mockData })

      const result = await updateModifiedSubtotal(1, null)

      expect(api.put).toHaveBeenCalledWith('/api/inventory-integration/1', {
        modifiedSubtotal: null
      })
      expect(result).toEqual(mockData)
    })
  })
})
