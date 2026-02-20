import { useState, useEffect } from 'react'
import { createForecastItem } from '../../api/forecast'
import './AddItemDialog.css'

export default function AddItemDialog({ open, month, channel, onClose, onSuccess }) {
  const [formData, setFormData] = useState({
    category: '',
    spec: '',
    productCode: '',
    productName: '',
    warehouseLocation: '',
    quantity: '',
  })
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!open) {
      setFormData({
        category: '',
        spec: '',
        productCode: '',
        productName: '',
        warehouseLocation: '',
        quantity: '',
      })
      setErrors({})
    }
  }, [open])

  const validateForm = () => {
    const newErrors = {}

    if (!formData.productCode.trim()) {
      newErrors.productCode = '產品代碼為必填'
    }

    if (!formData.productName.trim()) {
      newErrors.productName = '產品名稱為必填'
    }

    if (!formData.quantity) {
      newErrors.quantity = '數量為必填'
    } else {
      const qty = Number(formData.quantity)
      if (isNaN(qty) || qty < 0) {
        newErrors.quantity = '數量必須為正數'
      }
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()

    if (!validateForm()) {
      return
    }

    setSubmitting(true)

    try {
      const payload = {
        month,
        channel,
        category: formData.category || null,
        spec: formData.spec || null,
        productCode: formData.productCode,
        productName: formData.productName,
        warehouseLocation: formData.warehouseLocation || null,
        quantity: Number(formData.quantity),
      }

      await createForecastItem(payload)
      onSuccess()
    } catch (err) {
      if (err.response?.data?.error) {
        setErrors({ general: err.response.data.error })
      } else {
        setErrors({ general: '新增失敗，請重試' })
      }
    } finally {
      setSubmitting(false)
    }
  }

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) {
      setErrors((prev) => {
        const newErrors = { ...prev }
        delete newErrors[field]
        return newErrors
      })
    }
  }

  if (!open) return null

  return (
    <div className="dialog-overlay" onClick={onClose}>
      <div
        className="dialog dialog--add-item"
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-item-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id="add-item-title" className="dialog-title">
          新增項目
        </h3>

        <form onSubmit={handleSubmit} className="add-item-form">
          {errors.general && (
            <div className="form-error" role="alert">
              {errors.general}
            </div>
          )}

          <div className="form-field">
            <label htmlFor="category">類別</label>
            <input
              type="text"
              id="category"
              className="form-input"
              value={formData.category}
              onChange={(e) => handleChange('category', e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="form-field">
            <label htmlFor="spec">規格</label>
            <input
              type="text"
              id="spec"
              className="form-input"
              value={formData.spec}
              onChange={(e) => handleChange('spec', e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="form-field">
            <label htmlFor="productCode">
              產品代碼 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="productCode"
              className={`form-input ${errors.productCode ? 'form-input--error' : ''}`}
              value={formData.productCode}
              onChange={(e) => handleChange('productCode', e.target.value)}
              disabled={submitting}
              required
            />
            {errors.productCode && (
              <span className="field-error" role="alert">
                {errors.productCode}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="productName">
              產品名稱 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="productName"
              className={`form-input ${errors.productName ? 'form-input--error' : ''}`}
              value={formData.productName}
              onChange={(e) => handleChange('productName', e.target.value)}
              disabled={submitting}
              required
            />
            {errors.productName && (
              <span className="field-error" role="alert">
                {errors.productName}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="warehouseLocation">倉儲位置</label>
            <input
              type="text"
              id="warehouseLocation"
              className="form-input"
              value={formData.warehouseLocation}
              onChange={(e) => handleChange('warehouseLocation', e.target.value)}
              disabled={submitting}
            />
          </div>

          <div className="form-field">
            <label htmlFor="quantity">
              數量 <span className="required">*</span>
            </label>
            <input
              type="number"
              id="quantity"
              className={`form-input ${errors.quantity ? 'form-input--error' : ''}`}
              value={formData.quantity}
              onChange={(e) => handleChange('quantity', e.target.value)}
              disabled={submitting}
              required
              min="0"
            />
            {errors.quantity && (
              <span className="field-error" role="alert">
                {errors.quantity}
              </span>
            )}
          </div>

          <div className="dialog-actions">
            <button type="button" className="btn btn--secondary" onClick={onClose} disabled={submitting}>
              取消
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? '處理中...' : '儲存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
