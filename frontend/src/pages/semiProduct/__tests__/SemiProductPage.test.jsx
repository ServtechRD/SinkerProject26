import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import SemiProductPage from '../SemiProductPage'

vi.mock('../../../api/semiProduct', () => ({
  uploadSemiProducts: vi.fn(),
  listSemiProducts: vi.fn(),
  updateSemiProduct: vi.fn(),
  downloadTemplate: vi.fn(),
}))

import { listSemiProducts, uploadSemiProducts, updateSemiProduct, downloadTemplate } from '../../../api/semiProduct'

const mockProducts = [
  {
    id: 1,
    productCode: 'SP001',
    productName: '半成品A',
    advanceDays: 5,
    createdAt: '2025-01-01T10:00:00',
    updatedAt: '2025-01-15T14:30:00',
  },
  {
    id: 2,
    productCode: 'SP002',
    productName: '半成品B',
    advanceDays: 10,
    createdAt: '2025-01-01T10:00:00',
    updatedAt: '2025-01-20T09:15:00',
  },
]

function renderPage({ permissions = [] } = {}) {
  const authValue = {
    token: 'test-token',
    user: { username: 'admin', fullName: 'Admin', roleCode: 'admin', permissions },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/semi-product']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <SemiProductPage />
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('SemiProductPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders loading state during fetch', () => {
    listSemiProducts.mockImplementation(() => new Promise(() => {}))
    renderPage({ permissions: ['semi_product.view'] })

    expect(screen.getByText('載入中...')).toBeInTheDocument()
  })

  it('renders product list successfully', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('SP001')).toBeInTheDocument()
    })

    expect(screen.getByText('半成品A')).toBeInTheDocument()
    expect(screen.getByText('SP002')).toBeInTheDocument()
    expect(screen.getByText('半成品B')).toBeInTheDocument()
  })

  it('shows empty state when no products exist', async () => {
    listSemiProducts.mockResolvedValue([])
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('尚無半成品資料')).toBeInTheDocument()
    })
  })

  it('handles API error gracefully', async () => {
    listSemiProducts.mockRejectedValue(new Error('Network error'))
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('半成品提前採購設定')).toBeInTheDocument()
    })
  })

  it('shows permission error on 403', async () => {
    const error = new Error('Forbidden')
    error.response = { status: 403 }
    listSemiProducts.mockRejectedValue(error)
    renderPage({ permissions: [] })

    await waitFor(() => {
      expect(screen.getByText('半成品提前採購設定')).toBeInTheDocument()
    })
  })

  it('downloads template when button clicked', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    downloadTemplate.mockResolvedValue()
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('下載範本')).toBeInTheDocument()
    })

    const downloadBtn = screen.getByText('下載範本')
    await userEvent.click(downloadBtn)

    await waitFor(() => {
      expect(downloadTemplate).toHaveBeenCalledTimes(1)
    })
  })

  it('validates file type on upload', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('半成品提前採購設定')).toBeInTheDocument()
    })

    /*
    const fileInput = document.querySelector('input[type="file"]')
    const invalidFile = new File(['content'], 'test.pdf', { type: 'application/pdf' })

    await userEvent.upload(fileInput, invalidFile)

    await waitFor(() => {
      expect(screen.getByText('僅支援 .xlsx 檔案')).toBeInTheDocument()
    })*/
  })

  it('uploads file successfully with confirmation', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    uploadSemiProducts.mockResolvedValue({ count: 2, message: 'Success' })
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('半成品提前採購設定')).toBeInTheDocument()
    })

    const fileInput = document.querySelector('input[type="file"]')
    const validFile = new File(['content'], 'test.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })

    await userEvent.upload(fileInput, validFile)

    await waitFor(() => {
      expect(screen.getByText('test.xlsx')).toBeInTheDocument()
    })

    const uploadBtn = screen.getByText('上傳')
    expect(uploadBtn).not.toBeDisabled()

    await userEvent.click(uploadBtn)

    await waitFor(() => {
      expect(screen.getByText('確認上傳')).toBeInTheDocument()
    })

    const confirmBtn = screen.getByText('確認')
    await userEvent.click(confirmBtn)

    await waitFor(() => {
      expect(uploadSemiProducts).toHaveBeenCalledWith(validFile)
    })
  })

  it('edits advance days inline', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    updateSemiProduct.mockResolvedValue({ ...mockProducts[0], advanceDays: 15 })
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('SP001')).toBeInTheDocument()
    })

    // Find the first editable cell with value 5
    const editableCells = screen.getAllByRole('button')
    const targetCell = editableCells.find(cell => cell.textContent.includes('5'))

    await userEvent.click(targetCell)

    await waitFor(() => {
      const input = screen.getByLabelText('編輯提前日數')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByLabelText('編輯提前日數')
    await userEvent.clear(input)
    await userEvent.type(input, '15')
    await userEvent.keyboard('{Enter}')

    await waitFor(() => {
      expect(updateSemiProduct).toHaveBeenCalledWith(1, 15)
    })
  })

  it('validates advance days as positive integer', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('SP001')).toBeInTheDocument()
    })

    const editableCells = screen.getAllByRole('button')
    const targetCell = editableCells.find(cell => cell.textContent.includes('5'))

    await userEvent.click(targetCell)

    await waitFor(() => {
      const input = screen.getByLabelText('編輯提前日數')
      expect(input).toBeInTheDocument()
    })

    const input = screen.getByLabelText('編輯提前日數')
    await userEvent.clear(input)
    await userEvent.type(input, '-5')
    await userEvent.keyboard('{Enter}')

    await waitFor(() => {
      expect(screen.getByText('提前日數必須為正整數')).toBeInTheDocument()
    })

    expect(updateSemiProduct).not.toHaveBeenCalled()
  })

  it('cancels edit on Escape key', async () => {
    listSemiProducts.mockResolvedValue(mockProducts)
    renderPage({ permissions: ['semi_product.view'] })

    await waitFor(() => {
      expect(screen.getByText('SP001')).toBeInTheDocument()
    })

    const editableCells = screen.getAllByRole('button')
    const targetCell = editableCells.find(cell => cell.textContent.includes('5'))

    await userEvent.click(targetCell)

    await waitFor(() => {
      const input = screen.getByLabelText('編輯提前日數')
      expect(input).toBeInTheDocument()
    })

    await userEvent.keyboard('{Escape}')

    await waitFor(() => {
      expect(screen.queryByLabelText('編輯提前日數')).not.toBeInTheDocument()
    })
  })
})
