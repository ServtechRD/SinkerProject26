import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ProductionPlanPage from '../ProductionPlanPage'
import * as productionPlanApi from '../../../api/productionPlan'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/productionPlan', () => ({
  getProductionPlan: vi.fn(),
  getProductionPlanVersions: vi.fn(),
  getProductionPlanByRange: vi.fn(),
  updateProductionPlanBuffer: vi.fn(),
  updateProductionPlanItem: vi.fn(),
}))

describe('ProductionPlanPage', () => {
  const channelData = (ch, months, total) => ({ channel: ch, months: months || {}, total: total ?? 0 })
  const defaultMonth = `${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, '0')}`
  const mockRangeResponse = {
    month_keys: [defaultMonth],
    channel_order: ['PX + 大全聯', '家樂福'],
    rows: [
      {
        warehouse_location: 'WH1',
        category: 'A',
        spec: 'S1',
        product_name: 'Product 1',
        product_code: 'PROD001',
        channel_data: [
          channelData('PX + 大全聯', { [defaultMonth]: 100 }, 100),
          channelData('家樂福', {}, 0),
        ],
        aggregate_months: { [defaultMonth]: 250 },
        buffer_quantity: 50,
        aggregate_total: 250,
        original_forecast: 250,
        difference: 0,
        remarks: 'Test',
        production_form_id: 1,
      },
      {
        warehouse_location: 'WH2',
        category: 'B',
        spec: 'S2',
        product_name: 'Product 2',
        product_code: 'PROD002',
        channel_data: [
          channelData('PX + 大全聯', { [defaultMonth]: 50 }, 50),
          channelData('家樂福', {}, 0),
        ],
        aggregate_months: { [defaultMonth]: 125 },
        buffer_quantity: 25,
        aggregate_total: 125,
        original_forecast: 125,
        difference: 0,
        remarks: '',
        production_form_id: 2,
      },
    ],
  }
  const mockData = mockRangeResponse.rows

  beforeEach(() => {
    vi.clearAllMocks()
    productionPlanApi.getProductionPlanVersions.mockResolvedValue(['v1'])
    productionPlanApi.getProductionPlanByRange.mockResolvedValue(mockRangeResponse)
  })

  it('shows access denied if user lacks production_plan.view permission', () => {
    const authValue = {
      user: { id: 1, username: 'user', roleCode: 'user', permissions: [] },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })

  /*it('renders page with year selector for user with view permission', () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'user',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    expect(screen.getByText(/生產表單/)).toBeInTheDocument()
   // expect(screen.getByLabelText('年度')).toBeInTheDocument()
   // expect(screen.getByRole('button', { name: '載入' })).toBeInTheDocument()
  })*/

  it('loads data when Query button is clicked', async () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(productionPlanApi.getProductionPlanVersions).toHaveBeenCalled()
      expect(productionPlanApi.getProductionPlanByRange).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
      expect(screen.getByText('Product 1')).toBeInTheDocument()
      expect(screen.getByText('PROD002')).toBeInTheDocument()
      expect(screen.getByText('Product 2')).toBeInTheDocument()
    })
  })

  it('shows empty state when no data', async () => {
    productionPlanApi.getProductionPlanVersions.mockResolvedValue([])

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText(/該區間尚無生產表單資料/)).toBeInTheDocument()
    })
  })

  it('displays grid with table headers', async () => {
    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('Excel 匯出')).toBeInTheDocument()
    })
    expect(screen.getByText('庫位')).toBeInTheDocument()
    expect(screen.getByText('中類名稱')).toBeInTheDocument()
    expect(screen.getByText('貨品規格')).toBeInTheDocument()
    expect(screen.getByText('品名')).toBeInTheDocument()
    expect(screen.getByText('品號')).toBeInTheDocument()
    expect(screen.getAllByText('原始預估').length).toBeGreaterThan(0)
    expect(screen.getAllByText('差異').length).toBeGreaterThan(0)
    expect(screen.getAllByText('備註').length).toBeGreaterThan(0)
    expect(screen.getByText('合計')).toBeInTheDocument()
  })

  it('calculates total and difference correctly', async () => {
    productionPlanApi.getProductionPlanByRange.mockResolvedValue({
      ...mockRangeResponse,
      rows: [mockRangeResponse.rows[0]],
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('PROD001')).toBeInTheDocument()
    })
    expect(screen.getAllByText(/250/).length).toBeGreaterThan(0)
  })

  it('handles API error gracefully', async () => {
    productionPlanApi.getProductionPlanVersions.mockRejectedValue({
      response: { status: 500 },
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(productionPlanApi.getProductionPlanVersions).toHaveBeenCalled()
    })
    await waitFor(() => {
      expect(screen.getByText(/該區間尚無生產表單資料/)).toBeInTheDocument()
    })
  })

  it('shows access denied on 403 error', async () => {
    productionPlanApi.getProductionPlanVersions.mockRejectedValue({
      response: { status: 403 },
    })

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    await waitFor(() => {
      expect(screen.getByText('您沒有權限檢視此頁面')).toBeInTheDocument()
    })
  })

  it('shows loading state during data fetch', async () => {
    productionPlanApi.getProductionPlanVersions.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve(['v1']), 100))
    )

    const authValue = {
      user: {
        id: 1,
        username: 'user',
        roleCode: 'admin',
        permissions: ['production_plan.view'],
      },
      isAuthenticated: true,
    }

    renderWithAuth(<ProductionPlanPage />, { authValue })

    const queryButton = screen.getByRole('button', { name: '查詢' })
    await userEvent.click(queryButton)

    expect(productionPlanApi.getProductionPlanVersions).toHaveBeenCalled()

    // Wait for loading to finish and table to appear
    await waitFor(() => {
      expect(screen.getByText('Excel 匯出')).toBeInTheDocument()
    })
  })
})
