import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import MaterialDemandPage from '../MaterialDemandPage'
import * as materialDemandApi from '../../../api/materialDemand'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/materialDemand', () => ({
  getMaterialDemand: vi.fn(),
}))

describe('MaterialDemandPage', () => {
  const mockData = [
    {
      materialCode: 'MAT001',
      materialName: 'Material 1',
      unit: 'kg',
      lastPurchaseDate: '2026-02-15',
      demandDate: '2026-02-20',
      expectedDelivery: 100.50,
      demandQuantity: 150.00,
      estimatedInventory: 80.25,
    },
    {
      materialCode: 'MAT002',
      materialName: 'Material 2',
      unit: 'pcs',
      lastPurchaseDate: null,
      demandDate: '2026-02-21',
      expectedDelivery: 200.00,
      demandQuantity: 180.00,
      estimatedInventory: 220.00,
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows access denied if user lacks material_demand.view permission', () => {
    const authValue = {
      user: { id: 1, username: 'user', roleCode: 'user', permissions: [] },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })

  it('renders page with filters for user with view permission', () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    materialDemandApi.getMaterialDemand.mockResolvedValue([])

    renderWithAuth(<MaterialDemandPage />, { authValue })

    expect(screen.getByText('物料需求')).toBeInTheDocument()
    //expect(screen.getByLabelText('週次')).toBeInTheDocument()
    //expect(screen.getByLabelText('工廠')).toBeInTheDocument()
  })

  it('auto-loads data when week and factory are selected', async () => {
    materialDemandApi.getMaterialDemand.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      expect(materialDemandApi.getMaterialDemand).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('MAT001')).toBeInTheDocument()
      expect(screen.getByText('Material 1')).toBeInTheDocument()
      expect(screen.getByText('MAT002')).toBeInTheDocument()
      expect(screen.getByText('Material 2')).toBeInTheDocument()
    })
  })

  it('displays empty state when no data is available', async () => {
    materialDemandApi.getMaterialDemand.mockResolvedValue([])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText(/無物料需求資料/)).toBeInTheDocument()
    })
  })

  it('highlights rows with low inventory', async () => {
    const lowInventoryData = [
      {
        materialCode: 'MAT003',
        materialName: 'Material 3',
        unit: 'kg',
        lastPurchaseDate: '2026-02-15',
        demandDate: '2026-02-20',
        expectedDelivery: 100.00,
        demandQuantity: 150.00,
        estimatedInventory: 80.00,
      },
    ]
    materialDemandApi.getMaterialDemand.mockResolvedValue(lowInventoryData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      const row = screen.getByText('MAT003').closest('tr')
      expect(row).toHaveClass('low-inventory')
    })
  })

  it('formats decimal numbers with 2 decimal places', async () => {
    materialDemandApi.getMaterialDemand.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText('100.50')).toBeInTheDocument()
      expect(screen.getByText('150.00')).toBeInTheDocument()
      expect(screen.getByText('80.25')).toBeInTheDocument()
    })
  })

  it('displays "-" for null lastPurchaseDate', async () => {
    materialDemandApi.getMaterialDemand.mockResolvedValue(mockData)

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      const rows = screen.getAllByText('-')
      expect(rows.length).toBeGreaterThan(0)
    })
  })

  it('handles API errors gracefully', async () => {
    materialDemandApi.getMaterialDemand.mockRejectedValue(new Error('Network Error'))

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['material_demand.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<MaterialDemandPage />, { authValue })

    await waitFor(() => {
      expect(screen.getByText(/無物料需求資料/)).toBeInTheDocument()
    })
  })
})
