import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import ForecastEditStatusBanner, { PlannerQtyCell } from '../ForecastEditStatusBanner'

describe('ForecastEditStatusBanner', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('開放中不顯示', () => {
    const { container } = render(<ForecastEditStatusBanner status={{ status: 'OPEN' }} month="202612" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('已截止顯示紅色提示', () => {
    render(<ForecastEditStatusBanner status={{ status: 'CLOSED', autoCloseAt: '2026-12-10T00:00:00' }} month="202612" />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveClass('forecast-edit-status--closed')
    expect(alert).toHaveTextContent('已結束新增')
    expect(alert).toHaveTextContent('2026/12/10 00:00')
  })

  it('補登中顯示橘色提示、期限與剩餘時間', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-12-11T04:00:00Z'))
    render(<ForecastEditStatusBanner
      status={{ status: 'REOPENED', reopenExpiresAt: '2026-12-12T18:00:00', reopenReason: '全聯新品提案追加' }}
      month="202612" />)
    const box = screen.getByRole('status')
    expect(box).toHaveClass('forecast-edit-status--reopen')
    expect(box).toHaveTextContent('補登中')
    expect(box).toHaveTextContent('2026/12/12 18:00')
    expect(box).toHaveTextContent('剩餘 1 天 6 小時')
    expect(box).toHaveTextContent('全聯新品提案追加')
  })

  it('PlannerQtyCell 與業務數量不同時紅字', () => {
    render(
      <table><tbody><tr>
        <PlannerQtyCell item={{ productCode: 'P1', quantity: 120 }} planner={{ P1: 150 }} />
        <PlannerQtyCell item={{ product_code: 'P2', quantity: 5 }} planner={{ P2: 5 }} />
        <PlannerQtyCell item={{ productCode: 'P3', quantity: 5 }} planner={{}} />
      </tr></tbody></table>
    )
    expect(screen.getByText('150')).toHaveClass('planner-qty--changed')
    expect(screen.getByText('5')).not.toHaveClass('planner-qty--changed')
    expect(screen.getByText('-')).toBeInTheDocument()
  })
})
