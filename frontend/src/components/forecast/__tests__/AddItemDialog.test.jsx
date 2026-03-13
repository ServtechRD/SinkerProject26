import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastProvider } from '../../Toast'
import AddItemDialog from '../AddItemDialog'

vi.mock('../../../api/forecast', () => ({
  createForecastItem: vi.fn(),
}))

vi.mock('../../../api/reference', () => ({
  getProducts: vi.fn(),
}))

import { createForecastItem } from '../../../api/forecast'
import { getProducts } from '../../../api/reference'

function renderDialog({ open = true, month = '202601', channel = '家樂福', onClose = vi.fn(), onSuccess = vi.fn() } = {}) {
  return render(
    <ToastProvider>
      <AddItemDialog
        open={open}
        month={month}
        channel={channel}
        onClose={onClose}
        onSuccess={onSuccess}
      />
    </ToastProvider>
  )
}

const mockProduct = {
  code: 'P001',
  name: '測試產品',
  categoryName: '飲料',
  spec: '500ml',
  warehouseLocation: 'A1',
}

describe('AddItemDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getProducts.mockResolvedValue([mockProduct])
  })

  it('does not render when open is false', () => {
    renderDialog({ open: false })
    expect(screen.queryByText('新增項目')).not.toBeInTheDocument()
  })

  it('renders dialog when open is true', () => {
    renderDialog({ open: true })
    expect(screen.getByText('新增項目')).toBeInTheDocument()
  })

  it('renders all form fields', () => {
    renderDialog()

    expect(screen.getByLabelText(/中類名稱/)).toBeInTheDocument()
    expect(screen.getByLabelText(/貨品規格/)).toBeInTheDocument()
    expect(screen.getByLabelText(/品號/)).toBeInTheDocument()
    expect(screen.getByLabelText(/品名/)).toBeInTheDocument()
    expect(screen.getByLabelText(/庫位/)).toBeInTheDocument()
    expect(screen.getByLabelText(/箱數小計/)).toBeInTheDocument()
  })

  it('shows required indicators for mandatory fields', () => {
    renderDialog()

    const productCodeLabel = screen.getByLabelText(/品號/)
    const productNameLabel = screen.getByLabelText(/品名/)
    const quantityLabel = screen.getByLabelText(/箱數小計/)

    expect(productCodeLabel.closest('.form-field')).toHaveTextContent('*')
    expect(productNameLabel.closest('.form-field')).toHaveTextContent('*')
    expect(quantityLabel.closest('.form-field')).toHaveTextContent('*')
  })

  it('validates required fields', async () => {
    renderDialog()

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    /*
    await waitFor(() => {
      expect(screen.getByText('產品代碼為必填')).toBeInTheDocument()
      expect(screen.getByText('產品名稱為必填')).toBeInTheDocument()
      expect(screen.getByText('數量為必填')).toBeInTheDocument()
    })*/

    expect(createForecastItem).not.toHaveBeenCalled()
  })

  it('validates quantity is positive number', async () => {
    renderDialog()

    const productCodeInput = screen.getByLabelText(/品號/)
    await userEvent.clear(productCodeInput)
    await userEvent.type(productCodeInput, 'P001')

    await waitFor(() => {
      expect(screen.getByRole('listitem')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByRole('listitem'))

    const quantityInput = screen.getByLabelText(/箱數小計/)
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '-10')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    expect(createForecastItem).not.toHaveBeenCalled()
  })

  it('submits form with valid data', async () => {
    createForecastItem.mockResolvedValue({ id: 1 })
    const onSuccess = vi.fn()

    renderDialog({ onSuccess })

    const productCodeInput = screen.getByLabelText(/品號/)
    await userEvent.clear(productCodeInput)
    await userEvent.type(productCodeInput, 'P001')

    await waitFor(() => {
      expect(screen.getByRole('listitem')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByRole('listitem'))

    const quantityInput = screen.getByLabelText(/箱數小計/)
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(createForecastItem).toHaveBeenCalledWith({
        month: '202601',
        channel: '家樂福',
        category: '飲料',
        spec: '500ml',
        productCode: 'P001',
        productName: '測試產品',
        warehouseLocation: 'A1',
        quantity: 100,
      })
    })

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalled()
    })
  })

  it('submits form with all fields filled', async () => {
    createForecastItem.mockResolvedValue({ id: 1 })
    const onSuccess = vi.fn()

    renderDialog({ onSuccess })

    const productCodeInput = screen.getByLabelText(/品號/)
    await userEvent.clear(productCodeInput)
    await userEvent.type(productCodeInput, 'P001')

    await waitFor(() => {
      expect(screen.getByRole('listitem')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByRole('listitem'))

    const quantityInput = screen.getByLabelText(/箱數小計/)
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(createForecastItem).toHaveBeenCalledWith({
        month: '202601',
        channel: '家樂福',
        category: '飲料',
        spec: '500ml',
        productCode: 'P001',
        productName: '測試產品',
        warehouseLocation: 'A1',
        quantity: 100,
      })
    })

    await waitFor(() => {
      expect(onSuccess).toHaveBeenCalled()
    })
  })

  it('handles API error', async () => {
    createForecastItem.mockRejectedValue({
      response: { data: { error: '產品代碼已存在' } },
    })

    renderDialog()

    const productCodeInput = screen.getByLabelText(/品號/)
    await userEvent.clear(productCodeInput)
    await userEvent.type(productCodeInput, 'P001')

    await waitFor(() => {
      expect(screen.getByRole('listitem')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByRole('listitem'))

    const quantityInput = screen.getByLabelText(/箱數小計/)
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(screen.getByText('產品代碼已存在')).toBeInTheDocument()
    })
  })

  it('calls onClose when cancel button clicked', async () => {
    const onClose = vi.fn()
    renderDialog({ onClose })

    const cancelButton = screen.getByText('取消')
    await userEvent.click(cancelButton)

    expect(onClose).toHaveBeenCalled()
  })

  it('clears form when dialog reopened', async () => {
    const { rerender } = renderDialog({ open: true })

    await userEvent.type(screen.getByLabelText(/品號/), 'P001')
    await userEvent.type(screen.getByLabelText(/品名/), '測試產品')

    rerender(
      <ToastProvider>
        <AddItemDialog
          open={false}
          month="202601"
          channel="家樂福"
          onClose={vi.fn()}
          onSuccess={vi.fn()}
        />
      </ToastProvider>
    )

    rerender(
      <ToastProvider>
        <AddItemDialog
          open={true}
          month="202601"
          channel="家樂福"
          onClose={vi.fn()}
          onSuccess={vi.fn()}
        />
      </ToastProvider>
    )

    const productCodeInput = screen.getByLabelText(/品號/)
    const productNameInput = screen.getByLabelText(/品名/)

    expect(productCodeInput).toHaveValue('')
    expect(productNameInput).toHaveValue('')
  })

  it('disables inputs during submission', async () => {
    createForecastItem.mockImplementation(() => new Promise(() => {}))

    renderDialog()

    const productCodeInput = screen.getByLabelText(/品號/)
    await userEvent.clear(productCodeInput)
    await userEvent.type(productCodeInput, 'P001')

    await waitFor(() => {
      expect(screen.getByRole('listitem')).toBeInTheDocument()
    })
    await userEvent.click(screen.getByRole('listitem'))

    const quantityInput = screen.getByLabelText(/箱數小計/)
    await userEvent.clear(quantityInput)
    await userEvent.type(quantityInput, '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(screen.getByLabelText(/品號/)).toBeDisabled()
      expect(screen.getByText('處理中...')).toBeInTheDocument()
    })
    expect(screen.getByLabelText(/箱數小計/)).toBeDisabled()
  })
})
