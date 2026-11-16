import { describe, it, expect } from 'vitest'
import { formatTaipeiDateTime, formatLocalDateTime, toDateTimeLocalValue, remainingText } from '../dateTime'

describe('dateTime utils', () => {
  it('formatTaipeiDateTime 把無時區時間當 UTC 轉台灣時間', () => {
    expect(formatTaipeiDateTime('2026-12-08T09:40:00')).toBe('2026/12/08 17:40:00')
    expect(formatTaipeiDateTime(null)).toBe('-')
  })

  it('formatLocalDateTime 原樣顯示台灣時間', () => {
    expect(formatLocalDateTime('2026-12-10T00:00:00')).toBe('2026/12/10 00:00')
    expect(formatLocalDateTime('2026-12-10T18:30')).toBe('2026/12/10 18:30')
    expect(formatLocalDateTime(null)).toBe('-')
  })

  it('toDateTimeLocalValue 截到分鐘', () => {
    expect(toDateTimeLocalValue('2026-12-10T18:30:00')).toBe('2026-12-10T18:30')
    expect(toDateTimeLocalValue(null)).toBe('')
  })

  it('remainingText 以台灣時間計算剩餘時間', () => {
    const now = new Date('2026-12-11T04:00:00Z') // 台灣 12/11 12:00
    expect(remainingText('2026-12-12T18:00:00', now)).toBe('剩餘 1 天 6 小時')
    expect(remainingText('2026-12-11T15:20:00', now)).toBe('剩餘 3 小時 20 分')
    expect(remainingText('2026-12-11T11:00:00', now)).toBe('已到期')
  })
})
