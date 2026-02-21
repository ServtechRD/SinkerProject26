import api from './axios'

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
    .get(`/api/weekly-schedule/template/${factory}`, {
      responseType: 'blob',
    })
    .then((r) => {
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
