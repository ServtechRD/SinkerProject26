import { useState, useEffect, useRef } from 'react'
import { createForecastItem } from '../../api/forecast'
import { getProducts } from '../../api/reference'
import './AddItemDialog.css'

const MIN_PRODUCT_CHARS = 3

export default function AddItemDialog({ open, month, channel, onClose, onSuccess, createItemApi }) {
  const [formData, setFormData] = useState({
    category: '',
    spec: '',
    productCode: '',
    productName: '',
    warehouseLocation: '',
    quantity: 0,
  })
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [products, setProducts] = useState([])
  const [productOpen, setProductOpen] = useState(false)
  const [productFilter, setProductFilter] = useState('')
  const productRef = useRef(null)

  useEffect(() => {
    if (!open) return
    getProducts()
      .then((data) => setProducts(Array.isArray(data) ? data : []))
      .catch(() => setProducts([]))
  }, [open])

  useEffect(() => {
    if (!open) {
      setFormData({
        category: '',
        spec: '',
        productCode: '',
        productName: '',
        warehouseLocation: '',
        quantity: 0,
      })
      setErrors({})
      setProductFilter('')
      setProductOpen(false)
    }
  }, [open])

  const productFilterTrim = productFilter.trim()
  const filteredProducts =
    productFilterTrim.length >= MIN_PRODUCT_CHARS
      ? products.filter(
          (p) =>
            (p.code || '').toLowerCase().includes(productFilterTrim.toLowerCase()) ||
            (p.name || '').toLowerCase().includes(productFilterTrim.toLowerCase())
        )
      : []

  useEffect(() => {
    function handleClickOutside(e) {
      if (productRef.current && !productRef.current.contains(e.target)) closeProductDropdown()
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [open, productFilterTrim, formData.productCode, products])

  const validateForm = () => {
    const newErrors = {}
    const codeTrim = formData.productCode.trim()
    const nameTrim = formData.productName.trim()
    if (!codeTrim || !nameTrim) {
      newErrors.general = '未正確選到品號，無法新增'
    } else {
      const qty = Number(formData.quantity)
      if (isNaN(qty) || qty < 0) {
        newErrors.quantity = '箱數小計不可為負數'
      }
    }
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validateForm()) return
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
      const createItem = createItemApi || createForecastItem
      const created = await createItem(payload)
      onSuccess(created)
    } catch (err) {
      setErrors({ general: err.response?.data?.error || '新增失敗，請重試' })
    } finally {
      setSubmitting(false)
    }
  }

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) setErrors((prev) => ({ ...prev, [field]: undefined }))
  }

  const selectProduct = (p) => {
    handleChange('productCode', p.code)
    handleChange('category', p.categoryName || '')
    handleChange('spec', p.spec || '')
    handleChange('productName', p.name || '')
    handleChange('warehouseLocation', p.warehouseLocation || '')
    setProductFilter(p.code)
    setProductOpen(false)
  }

  const closeProductDropdown = () => {
    setProductOpen(false)
    setFormData((prev) => {
      if (productFilterTrim === prev.productCode) return prev
      const exact = products.find((p) => (p.code || '').trim() === productFilterTrim)
      return {
        ...prev,
        productCode: productFilterTrim,
        category: exact?.categoryName ?? prev.category,
        spec: exact?.spec ?? prev.spec,
        productName: exact ? exact.name : prev.productName,
        warehouseLocation: exact?.warehouseLocation ?? prev.warehouseLocation,
      }
    })
  }

  if (!open) return null

  return (
    <div className="dialog-overlay">
      <div
        className="dialog dialog--add-item"
        role="dialog"
        aria-modal="true"
        aria-labelledby="add-item-title"
      >
        <div className="dialog-title-row">
          <h3 id="add-item-title" className="dialog-title">
            新增項目
          </h3>
          <button
            type="button"
            className="dialog-close-btn"
            onClick={onClose}
            disabled={submitting}
            aria-label="關閉"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="add-item-form">
          {errors.general && (
            <div className="form-error" role="alert">
              {errors.general}
            </div>
          )}

          <div className="form-field" ref={productRef}>
            <label htmlFor="productCode">
              品號 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="productCode"
              className={`form-input ${errors.productCode ? 'form-input--error' : ''}`}
              value={productOpen ? productFilter : formData.productCode}
              onChange={(e) => {
                setProductFilter(e.target.value)
                setProductOpen(true)
                handleChange('category', '')
                handleChange('spec', '')
                handleChange('productName', '')
                handleChange('warehouseLocation', '')
              }}
              onFocus={() => {
                setProductOpen(true)
                if (formData.productCode) setProductFilter(formData.productCode)
              }}
              disabled={submitting}
              placeholder="輸入至少 3 碼以查詢"
              autoComplete="off"
            />
            {errors.productCode && (
              <span className="field-error" role="alert">
                {errors.productCode}
              </span>
            )}
            {productOpen && productFilterTrim.length >= MIN_PRODUCT_CHARS && (
              <ul className="add-item-dropdown">
                {filteredProducts.length === 0 ? (
                  <li className="add-item-dropdown-empty">無符合項目</li>
                ) : (
                  filteredProducts.slice(0, 50).map((p) => (
                    <li
                      key={p.code}
                      className="add-item-dropdown-item"
                      onClick={() => selectProduct(p)}
                    >
                      <span className="add-item-product-code">{p.code}</span>
                      <span className="add-item-product-name">{p.name}</span>
                    </li>
                  ))
                )}
              </ul>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="category">中類名稱</label>
            <input
              type="text"
              id="category"
              className="form-input form-input--readonly"
              value={formData.category}
              readOnly
              tabIndex={-1}
              onFocus={(e) => e.target.blur()}
              disabled={submitting}
              placeholder="依品號選擇後自動帶出"
            />
          </div>

          <div className="form-field">
            <label htmlFor="spec">貨品規格</label>
            <input
              type="text"
              id="spec"
              className="form-input form-input--readonly"
              value={formData.spec}
              readOnly
              tabIndex={-1}
              onFocus={(e) => e.target.blur()}
              disabled={submitting}
              placeholder="依品號選擇後自動帶出"
            />
          </div>

          <div className="form-field">
            <label htmlFor="productName">
              品名 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="productName"
              className={`form-input form-input--readonly ${errors.productName ? 'form-input--error' : ''}`}
              value={formData.productName}
              readOnly
              tabIndex={-1}
              onFocus={(e) => e.target.blur()}
              disabled={submitting}
              placeholder="依品號選擇後自動帶出"
            />
            {errors.productName && (
              <span className="field-error" role="alert">
                {errors.productName}
              </span>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="warehouseLocation">庫位</label>
            <input
              type="text"
              id="warehouseLocation"
              className="form-input form-input--readonly"
              value={formData.warehouseLocation}
              readOnly
              tabIndex={-1}
              onFocus={(e) => e.target.blur()}
              disabled={submitting}
              placeholder="依品號選擇後自動帶出"
            />
          </div>

          <div className="form-field">
            <label htmlFor="quantity">
              箱數小計 <span className="required">*</span>
            </label>
            <input
              type="number"
              id="quantity"
              className={`form-input ${errors.quantity ? 'form-input--error' : ''}`}
              value={formData.quantity}
              onChange={(e) => handleChange('quantity', e.target.value)}
              disabled={submitting}
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
