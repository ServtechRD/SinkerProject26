import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  uploadForecast,
  downloadTemplate,
  getForecastVersions,
  getForecastList,
  createForecastItem,
  updateForecastItem,
  deleteForecastItem,
} from '../forecast'
import api from '../axios'

vi.mock('../axios', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('forecast API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('uploadForecast', () => {
    it('sends FormData with file, month, and channel', async () => {
      const mockResponse = { data: { rows_processed: 100 } }
      api.post.mockResolvedValue(mockResponse)

      const file = new File(['test'], 'test.xlsx')
      const month = '202601'
      const channel = '家樂福'

      const result = await uploadForecast(file, month, channel)

      expect(api.post).toHaveBeenCalledTimes(1)
      const callArgs = api.post.mock.calls[0]
      expect(callArgs[0]).toBe('/api/sales-forecast/upload')
      expect(callArgs[1]).toBeInstanceOf(FormData)
      expect(callArgs[2]).toEqual({
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })

      expect(result).toEqual({ rows_processed: 100 })
    })

    it('throws error on API failure', async () => {
      api.post.mockRejectedValue(new Error('Network error'))

      const file = new File(['test'], 'test.xlsx')
      await expect(uploadForecast(file, '202601', '家樂福')).rejects.toThrow('Network error')
    })
  })

  describe('downloadTemplate', () => {
    it('downloads template file for channel', async () => {
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

      await downloadTemplate('家樂福')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/template/家樂福', {
        responseType: 'blob',
      })
      expect(createObjectURL).toHaveBeenCalledWith(expect.any(Blob))
      expect(createElement).toHaveBeenCalledWith('a')
      expect(link.href).toBe('blob:mock-url')
      expect(link.download).toBe('sales_forecast_template_家樂福.xlsx')
      expect(link.click).toHaveBeenCalled()
      expect(appendChild).toHaveBeenCalledWith(link)
      expect(removeChild).toHaveBeenCalledWith(link)
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:mock-url')
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(downloadTemplate('家樂福')).rejects.toThrow('Network error')
    })
  })

  describe('getForecastVersions', () => {
    it('fetches versions with month and channel params', async () => {
      const mockVersions = ['v2', 'v1']
      api.get.mockResolvedValue({ data: mockVersions })

      const result = await getForecastVersions('202601', '家樂福')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/versions', {
        params: { month: '202601', channel: '家樂福' },
      })
      expect(result).toEqual(mockVersions)
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(getForecastVersions('202601', '家樂福')).rejects.toThrow('Network error')
    })
  })

  describe('getForecastList', () => {
    it('fetches forecast data with params', async () => {
      const mockData = [{ id: 1, productCode: 'P001', quantity: 100 }]
      api.get.mockResolvedValue({ data: mockData })

      const result = await getForecastList('202601', '家樂福', 'v1')

      expect(api.get).toHaveBeenCalledWith('/api/sales-forecast', {
        params: { month: '202601', channel: '家樂福', version: 'v1' },
      })
      expect(result).toEqual(mockData)
    })

    it('throws error on API failure', async () => {
      api.get.mockRejectedValue(new Error('Network error'))

      await expect(getForecastList('202601', '家樂福', 'v1')).rejects.toThrow('Network error')
    })
  })

  describe('createForecastItem', () => {
    it('creates new forecast item', async () => {
      const mockItem = { id: 1, productCode: 'P001' }
      api.post.mockResolvedValue({ data: mockItem })

      const payload = {
        month: '202601',
        channel: '家樂福',
        productCode: 'P001',
        productName: '測試產品',
        quantity: 100,
      }

      const result = await createForecastItem(payload)

      expect(api.post).toHaveBeenCalledWith('/api/sales-forecast', payload)
      expect(result).toEqual(mockItem)
    })

    it('throws error on API failure', async () => {
      api.post.mockRejectedValue(new Error('Network error'))

      await expect(createForecastItem({})).rejects.toThrow('Network error')
    })
  })

  describe('updateForecastItem', () => {
    it('updates forecast item', async () => {
      const mockItem = { id: 1, quantity: 200 }
      api.put.mockResolvedValue({ data: mockItem })

      const result = await updateForecastItem(1, { quantity: 200 })

      expect(api.put).toHaveBeenCalledWith('/api/sales-forecast/1', { quantity: 200 })
      expect(result).toEqual(mockItem)
    })

    it('throws error on API failure', async () => {
      api.put.mockRejectedValue(new Error('Network error'))

      await expect(updateForecastItem(1, {})).rejects.toThrow('Network error')
    })
  })

  describe('deleteForecastItem', () => {
    it('deletes forecast item', async () => {
      const mockResponse = { success: true }
      api.delete.mockResolvedValue({ data: mockResponse })

      const result = await deleteForecastItem(1)

      expect(api.delete).toHaveBeenCalledWith('/api/sales-forecast/1')
      expect(result).toEqual(mockResponse)
    })

    it('throws error on API failure', async () => {
      api.delete.mockRejectedValue(new Error('Network error'))

      await expect(deleteForecastItem(1)).rejects.toThrow('Network error')
    })
  })
})
