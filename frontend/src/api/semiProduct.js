import api from './axios'

export function uploadSemiProducts(file) {
  const formData = new FormData()
  formData.append('file', file)

  return api
    .post('/api/semi-product/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((r) => r.data)
}

export function listSemiProducts() {
  return api.get('/api/semi-product').then((r) => r.data)
}

export function updateSemiProduct(id, advanceDays) {
  return api
    .put(`/api/semi-product/${id}`, { advanceDays })
    .then((r) => r.data)
}

export function downloadTemplate() {
  return api
    .get('/api/semi-product/template', {
      responseType: 'blob',
    })
    .then((r) => {
      const blob = new Blob([r.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'semi_product_template.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    })
}
