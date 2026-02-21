import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ToastProvider } from '../../Toast'
import AddItemDialog from '../AddItemDialog'

vi.mock('../../../api/forecast', () => ({
  createForecastItem: vi.fn(),
}))

import { createForecastItem } from '../../../api/forecast'

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

describe('AddItemDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
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

    expect(screen.getByLabelText(/類別/)).toBeInTheDocument()
    expect(screen.getByLabelText(/規格/)).toBeInTheDocument()
    expect(screen.getByLabelText(/產品代碼/)).toBeInTheDocument()
    expect(screen.getByLabelText(/產品名稱/)).toBeInTheDocument()
    expect(screen.getByLabelText(/倉儲位置/)).toBeInTheDocument()
    expect(screen.getByLabelText(/數量/)).toBeInTheDocument()
  })

  it('shows required indicators for mandatory fields', () => {
    renderDialog()

    const productCodeLabel = screen.getByLabelText(/產品代碼/)
    const productNameLabel = screen.getByLabelText(/產品名稱/)
    const quantityLabel = screen.getByLabelText(/數量/)

    expect(productCodeLabel.closest('.form-field')).toHaveTextContent('*')
    expect(productNameLabel.closest('.form-field')).toHaveTextContent('*')
    expect(quantityLabel.closest('.form-field')).toHaveTextContent('*')
  })

  it('validates required fields', async () => {
    renderDialog()

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(screen.getByText('產品代碼為必填')).toBeInTheDocument()
      expect(screen.getByText('產品名稱為必填')).toBeInTheDocument()
      expect(screen.getByText('數量為必填')).toBeInTheDocument()
    })

    expect(createForecastItem).not.toHaveBeenCalled()
  })

  it('validates quantity is positive number', async () => {
    renderDialog()

    const productCodeInput = screen.getByLabelText(/產品代碼/)
    const productNameInput = screen.getByLabelText(/產品名稱/)
    const quantityInput = screen.getByLabelText(/數量/)

    await userEvent.type(productCodeInput, 'P001')
    await userEvent.type(productNameInput, '測試產品')
    await userEvent.type(quantityInput, '-10')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(screen.getByText('數量必須為正數')).toBeInTheDocument()
    })

    expect(createForecastItem).not.toHaveBeenCalled()
  })

  it('submits form with valid data', async () => {
    createForecastItem.mockResolvedValue({ id: 1 })
    const onSuccess = vi.fn()

    renderDialog({ onSuccess })

    const productCodeInput = screen.getByLabelText(/產品代碼/)
    const productNameInput = screen.getByLabelText(/產品名稱/)
    const quantityInput = screen.getByLabelText(/數量/)

    await userEvent.type(productCodeInput, 'P001')
    await userEvent.type(productNameInput, '測試產品')
    await userEvent.type(quantityInput, '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(createForecastItem).toHaveBeenCalledWith({
        month: '202601',
        channel: '家樂福',
        category: null,
        spec: null,
        productCode: 'P001',
        productName: '測試產品',
        warehouseLocation: null,
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

    await userEvent.type(screen.getByLabelText(/類別/), '飲料')
    await userEvent.type(screen.getByLabelText(/規格/), '500ml')
    await userEvent.type(screen.getByLabelText(/產品代碼/), 'P001')
    await userEvent.type(screen.getByLabelText(/產品名稱/), '測試產品')
    await userEvent.type(screen.getByLabelText(/倉儲位置/), 'A1')
    await userEvent.type(screen.getByLabelText(/數量/), '100')

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

    await userEvent.type(screen.getByLabelText(/產品代碼/), 'P001')
    await userEvent.type(screen.getByLabelText(/產品名稱/), '測試產品')
    await userEvent.type(screen.getByLabelText(/數量/), '100')

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

    await userEvent.type(screen.getByLabelText(/產品代碼/), 'P001')
    await userEvent.type(screen.getByLabelText(/產品名稱/), '測試產品')

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

    const productCodeInput = screen.getByLabelText(/產品代碼/)
    const productNameInput = screen.getByLabelText(/產品名稱/)

    expect(productCodeInput).toHaveValue('')
    expect(productNameInput).toHaveValue('')
  })

  it('disables inputs during submission', async () => {
    createForecastItem.mockImplementation(() => new Promise(() => {}))

    renderDialog()

    await userEvent.type(screen.getByLabelText(/產品代碼/), 'P001')
    await userEvent.type(screen.getByLabelText(/產品名稱/), '測試產品')
    await userEvent.type(screen.getByLabelText(/數量/), '100')

    const saveButton = screen.getByText('儲存')
    await userEvent.click(saveButton)

    await waitFor(() => {
      expect(screen.getByLabelText(/產品代碼/)).toBeDisabled()
      expect(screen.getByLabelText(/產品名稱/)).toBeDisabled()
      expect(screen.getByLabelText(/數量/)).toBeDisabled()
      expect(screen.getByText('處理中...')).toBeInTheDocument()
    })
  })
})
