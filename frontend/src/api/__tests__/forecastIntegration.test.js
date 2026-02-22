import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  getIntegrationVersions,
  getIntegrationData,
  exportIntegrationExcel,
} from '../forecastIntegration'
import api from '../axios'

vi.mock('../axios', () => ({
  default: {
    get: vi.fn(),
  },
}))

describe('forecastIntegration API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('getIntegrationVersions', () => {
    it('fetches versions with month param', async () => {
      const mockVersions = ['v2', 'v1']
      api.get.mockResolvedValue({ data: mockVersions })

      const result = await getIntegrationVersions('202601')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/integration/versions', {
        params: { month: '202601' },
      })
      expect(result).toEqual(mockVersions)
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(getIntegrationVersions('202601')).rejects.toThrow('Network error')
    })
  })

  describe('getIntegrationData', () => {
    it('fetches integration data with month and version params', async () => {
      const mockData = [
        {
          productCode: 'P001',
          productName: '測試產品',
          pxDaQuanLian: 100,
          jiaLeFu: 50,
          originalSubtotal: 150,
          difference: 10,
        },
      ]
      api.get.mockResolvedValue({ data: mockData })

      const result = await getIntegrationData('202601', 'v1')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/integration', {
        params: { month: '202601', version: 'v1' },
      })
      expect(result).toEqual(mockData)
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(getIntegrationData('202601', 'v1')).rejects.toThrow('Network error')
    })
  })

  describe('exportIntegrationExcel', () => {
    it('downloads Excel file with correct filename', async () => {
      const mockBlob = new Blob(['test'], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      api.get.mockResolvedValue({ data: mockBlob })

      // Mock DOM methods
      const createObjectURL = vi.fn().mockReturnValue('blob:mock-url')
      const revokeObjectURL = vi.fn()
      global.URL.createObjectURL = createObjectURL
      global.URL.revokeObjectURL = revokeObjectURL

      const link = {
        href: '',
        download: '',
        click: vi.fn(),
      }
      const createElement = vi.spyOn(document, 'createElement').mockReturnValue(link)
      const appendChild = vi.spyOn(document.body, 'appendChild').mockImplementation(() => {})
      const removeChild = vi.spyOn(document.body, 'removeChild').mockImplementation(() => {})

      // Mock Date to ensure consistent filename
      beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date("2026-02-22T12:00:00Z"));
      });

      afterEach(() => {
        vi.useRealTimers();
      });
      await exportIntegrationExcel('202601', 'v1')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/integration/export', {
        params: { month: '202601', version: 'v1' },
        responseType: 'blob',
      })
      expect(createObjectURL).toHaveBeenCalledWith(expect.any(Blob))
      expect(createElement).toHaveBeenCalledWith('a')
      expect(link.href).toBe('blob:mock-url')
      expect(link.download).toMatch(/^sales_forecast_integration_202601_\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}\.xlsx$/)
      expect(link.click).toHaveBeenCalled()
      expect(appendChild).toHaveBeenCalledWith(link)
      expect(removeChild).toHaveBeenCalledWith(link)
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(exportIntegrationExcel('202601', 'v1')).rejects.toThrow('Network error')
    })
  })
})
