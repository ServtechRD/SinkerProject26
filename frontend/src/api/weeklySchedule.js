import api from './axios'

export function getWeeklyScheduleFactories() {
  return api.get('/api/weekly-schedule/factories').then((r) => r.data)
}

export function uploadWeeklySchedule(file, weekStart, factory) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('week_start', weekStart)
  formData.append('factory', factory)

  return api
    .post('/api/weekly-schedule/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((r) => r.data)
}

export function downloadWeeklyScheduleTemplate(factory) {
  return api
    .get(`/api/weekly-schedule/template/${encodeURIComponent(factory)}`, {
      responseType: 'blob',
    })
    .then((r) => {
      const contentType = r.headers?.['content-type'] || ''
      if (contentType.includes('application/json')) {
        return r.data.text().then((text) => {
          let msg = '下載範本失敗'
          try {
            const body = JSON.parse(text)
            if (body.message) msg = body.message
          } catch (_) {}
          throw new Error(msg)
        })
      }
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `weekly_schedule_template_${factory}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}

export function getWeeklySchedule(weekStart, factory) {
  return api
    .get('/api/weekly-schedule', {
      params: { week_start: weekStart, factory },
    })
    .then((r) => r.data)
}

export function updateScheduleEntry(id, data) {
  return api.put(`/api/weekly-schedule/${id}`, data).then((r) => r.data)
}
