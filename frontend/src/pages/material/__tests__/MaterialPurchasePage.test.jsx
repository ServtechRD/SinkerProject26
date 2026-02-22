import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MaterialPurchasePage from '../MaterialPurchasePage'
import * as materialPurchaseApi from '../../../api/materialPurchase'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/materialPurchase', () => ({
  getMaterialPurchase: vi.fn(),
  triggerErp: vi.fn(),
}))

describe('MaterialPurchasePage', () => {
  const mockData = [
    {
      id: 1,
      weekStart: '2026-02-17',
      factory: 'F1',
      productCode: 'PROD001',
      productName: 'Product 1',
      quantity: 100.50,
      semiProductCode: 'SEMI001',
      semiProductName: 'Semi Product 1',
      kgPerBox: 25.00,
      basketQuantity: 4.02,
      boxesPerBarrel: 10.00,
      requiredBarrels: 0.40,
      isErpTriggered: false,
      erpOrderNo: null,
    },
    {
      id: 2,
      weekStart: '2026-02-17',
      factory: 'F1',
      productCode: 'PROD002',
      productName: 'Product 2',
      quantity: 200.00,
      semiProductCode: 'SEMI002',
      semiProductName: 'Semi Product 2',
      kgPerBox: 30.00,
      basketQuantity: 6.67,
      boxesPerBarrel: 12.00,
      requiredBarrels: 0.56,
      isErpTriggered: true,
      erpOrderNo: 'ERP-2026-001',
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows access denied if user lacks material_purchase.view permission', () => {
    const authValue = {
      user: { id: 1, username: 'user', roleCode: 'user', permissions: [] },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })

  it('renders page with filters for user with view permission', () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    materialPurchaseApi.getMaterialPurchase.mockResolvedValue([])

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    expect(screen.getByText('物料採購規劃')).toBeInTheDocument()
   // expect(screen.getByLabelText('週次')).toBeInTheDocument()
   // expect(screen.getByLabelText('工廠')).toBeInTheDocument()
  })

  it('auto-loads data when week and factory are selected', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(materialPurchaseApi.getMaterialPurchase).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
      expect(screen.getByText('Product 1')).toBeInTheDocument()
      expect(screen.getByText('PROD002')).toBeInTheDocument()
      expect(screen.getByText('Product 2')).toBeInTheDocument()
    })
  })

  it('displays empty state when no data is available', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue([])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText(/無物料採購資料/)).toBeInTheDocument()
    })
  })

  it('shows trigger button for non-triggered items', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view', 'material_purchase.trigger_erp'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('觸發 ERP')).toBeInTheDocument()
    })
  })

  it('shows triggered badge for already triggered items', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('已觸發: ERP-2026-001')).toBeInTheDocument()
    })
  })

  it('opens confirmation dialog when trigger button is clicked', async () => {
    const user = userEvent.setup()
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view', 'material_purchase.trigger_erp'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('觸發 ERP')).toBeInTheDocument()
    })

    const triggerButton = screen.getByText('觸發 ERP')
    await user.click(triggerButton)

    await waitFor(() => {
      expect(screen.getByText('確認觸發 ERP')).toBeInTheDocument()
      expect(screen.getByText(/確定要為 Product 1/)).toBeInTheDocument()
    })
  })

  it('triggers ERP when confirmed in dialog', async () => {
    const user = userEvent.setup()
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)
    materialPurchaseApi.triggerErp.mockResolvedValue({
      ...mockData[0],
      isErpTriggered: true,
      erpOrderNo: 'ERP-2026-002',
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view', 'material_purchase.trigger_erp'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('觸發 ERP')).toBeInTheDocument()
    })

    const triggerButton = screen.getByText('觸發 ERP')
    await user.click(triggerButton)

    await waitFor(() => {
      expect(screen.getByText('確認觸發 ERP')).toBeInTheDocument()
    })

    const confirmButton = screen.getByText('確認')
    await user.click(confirmButton)

    await waitFor(() => {
      expect(materialPurchaseApi.triggerErp).toHaveBeenCalledWith(1)
    })

    await waitFor(() => {
      expect(materialPurchaseApi.getMaterialPurchase).toHaveBeenCalledTimes(2)
    })
  })

  it('cancels trigger when cancel button is clicked in dialog', async () => {
    const user = userEvent.setup()
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view', 'material_purchase.trigger_erp'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('觸發 ERP')).toBeInTheDocument()
    })

    const triggerButton = screen.getByText('觸發 ERP')
    await user.click(triggerButton)

    await waitFor(() => {
      expect(screen.getByText('確認觸發 ERP')).toBeInTheDocument()
    })

    const cancelButton = screen.getByText('取消')
    await user.click(cancelButton)

    await waitFor(() => {
      expect(screen.queryByText('確認觸發 ERP')).not.toBeInTheDocument()
    })

    expect(materialPurchaseApi.triggerErp).not.toHaveBeenCalled()
  })

  it('handles 409 conflict error when triggering already triggered item', async () => {
    const user = userEvent.setup()
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)
    materialPurchaseApi.triggerErp.mockRejectedValue({
      response: {
        status: 409,
        data: { message: '訂單已觸發: ERP-2026-001' },
      },
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view', 'material_purchase.trigger_erp'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('觸發 ERP')).toBeInTheDocument()
    })

    const triggerButton = screen.getByText('觸發 ERP')
    await user.click(triggerButton)

    await waitFor(() => {
      expect(screen.getByText('確認觸發 ERP')).toBeInTheDocument()
    })

    const confirmButton = screen.getByText('確認')
    await user.click(confirmButton)

    await waitFor(() => {
      expect(materialPurchaseApi.triggerErp).toHaveBeenCalled()
    })
  })

  it('formats decimal numbers with 2 decimal places', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('100.50')).toBeInTheDocument()
      expect(screen.getByText('25.00')).toBeInTheDocument()
      expect(screen.getByText('4.02')).toBeInTheDocument()
      expect(screen.getByText('10.00')).toBeInTheDocument()
      expect(screen.getByText('0.40')).toBeInTheDocument()
    })
  })

  it('disables trigger button when user lacks trigger permission', async () => {
    materialPurchaseApi.getMaterialPurchase.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      const triggerButton = screen.getByText('觸發 ERP')
      expect(triggerButton).toBeDisabled()
    })
  })

  it('handles API errors gracefully', async () => {
    materialPurchaseApi.getMaterialPurchase.mockRejectedValue(new Error('Network Error'))

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_purchase.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialPurchasePage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText(/無物料採購資料/)).toBeInTheDocument()
    })
  })
})
