import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ErpVendorSyncPage from '../ErpVendorSyncPage'
import * as integrationsApi from '../../../api/integrations'
import { renderWithAuth } from '../../../test/helpers'

vi.mock('../../../api/integrations', () => ({
  syncErpVendors: vi.fn(),
  getErpVendorSyncStatus: vi.fn(),
}))

describe('ErpVendorSyncPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows access denied message for user without user.view permission', async () => {
    const authValue = {
      user: { id: 1, username: 'user', roleCode: 'user', permissions: [] },
      isAuthenticated: true,
    }
    integrationsApi.getErpVendorSyncStatus.mockResolvedValue(null)

    renderWithAuth(<ErpVendorSyncPage />, { authValue })

    expect(screen.getByText('您沒有權限執行此操作。')).toBeInTheDocument()
  })

  it('renders idle status and triggers sync on button click', async () => {
    const authValue = {
      user: { id: 1, username: 'admin', roleCode: 'admin', permissions: ['user.view'] },
      isAuthenticated: true,
    }
    integrationsApi.getErpVendorSyncStatus.mockResolvedValue({
      running: false,
      lastStartedAt: null,
      lastFinishedAt: null,
      lastResult: null,
      lastError: null,
    })
    integrationsApi.syncErpVendors.mockResolvedValue({ message: '同步已開始' })

    renderWithAuth(<ErpVendorSyncPage />, { authValue })

    await waitFor(() => expect(screen.getByText('閒置')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: '開始同步' }))

    await waitFor(() => expect(integrationsApi.syncErpVendors).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(screen.getByText('同步已開始，可離開此頁面')).toBeInTheDocument())
  })

  it('shows conflict toast when sync already running (409)', async () => {
    const authValue = {
      user: { id: 1, username: 'admin', roleCode: 'admin', permissions: ['user.view'] },
      isAuthenticated: true,
    }
    integrationsApi.getErpVendorSyncStatus.mockResolvedValue({
      running: false,
      lastResult: null,
      lastError: null,
    })
    integrationsApi.syncErpVendors.mockRejectedValue({ response: { status: 409 } })

    renderWithAuth(<ErpVendorSyncPage />, { authValue })

    await waitFor(() => expect(screen.getByText('閒置')).toBeInTheDocument())
    await userEvent.click(screen.getByRole('button', { name: '開始同步' }))

    await waitFor(() => expect(screen.getByText('同步已在執行中，請稍後')).toBeInTheDocument())
  })
})
