import { useState } from 'react'
import './WeekPicker.css'

// Get Monday of the week for a given date
function getMondayOfWeek(date) {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1) // adjust when day is sunday
  return new Date(d.setDate(diff))
}

// Format date as YYYY-MM-DD
function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// Parse YYYY-MM-DD to Date
function parseDate(dateStr) {
  if (!dateStr) return null
  const [year, month, day] = dateStr.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export default function WeekPicker({ value, onChange, disabled }) {
  const [showPicker, setShowPicker] = useState(false)
  const selectedDate = parseDate(value)

  // Get Monday dates for the next 12 weeks
  const getMondays = () => {
    const mondays = []
    const today = new Date()
    const currentMonday = getMondayOfWeek(today)

    for (let i = -4; i < 8; i++) {
      const monday = new Date(currentMonday)
      monday.setDate(currentMonday.getDate() + i * 7)
      mondays.push(monday)
    }

    return mondays
  }

  const mondays = getMondays()

  const handleSelect = (monday) => {
    onChange(formatDate(monday))
    setShowPicker(false)
  }

  const formatWeekLabel = (monday) => {
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)

    const mondayStr = `${monday.getMonth() + 1}/${monday.getDate()}`
    const sundayStr = `${sunday.getMonth() + 1}/${sunday.getDate()}`

    return `${monday.getFullYear()} 第 ${mondayStr} - ${sundayStr} 週`
  }

  const displayValue = selectedDate ? formatWeekLabel(selectedDate) : '請選擇週次'

  return (
    <div className="week-picker">
      <button
        type="button"
        className="week-picker-input"
        onClick={() => !disabled && setShowPicker(!showPicker)}
        disabled={disabled}
      >
        <span>{displayValue}</span>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
      </button>

      {showPicker && (
        <>
          <div className="week-picker-backdrop" onClick={() => setShowPicker(false)} />
          <div className="week-picker-dropdown">
            <div className="week-picker-header">選擇週次（週一開始）</div>
            <div className="week-picker-list">
              {mondays.map((monday) => {
                const isSelected = selectedDate && formatDate(monday) === formatDate(selectedDate)
                return (
                  <button
                    key={formatDate(monday)}
                    type="button"
                    className={`week-picker-item ${isSelected ? 'week-picker-item--selected' : ''}`}
                    onClick={() => handleSelect(monday)}
                  >
                    {formatWeekLabel(monday)}
                  </button>
                )
              })}
            </div>
          </div>
        </>
      )}
    </div>
  )
}
