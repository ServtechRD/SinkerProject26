import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { uploadForecast, downloadTemplate } from '../forecast'
import api from '../axios'

vi.mock('../axios', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
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
})
