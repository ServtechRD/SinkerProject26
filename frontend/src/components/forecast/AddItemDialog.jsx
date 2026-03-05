import { useState, useEffect, useRef } from 'react'
import { createForecastItem } from '../../api/forecast'
import { getCategories, getProducts } from '../../api/reference'
import './AddItemDialog.css'

const MIN_PRODUCT_CHARS = 3

export default function AddItemDialog({ open, month, channel, onClose, onSuccess }) {
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
  const [categories, setCategories] = useState([])
  const [products, setProducts] = useState([])
  const [categoryOpen, setCategoryOpen] = useState(false)
  const [categoryFilter, setCategoryFilter] = useState('')
  const [productOpen, setProductOpen] = useState(false)
  const [productFilter, setProductFilter] = useState('')
  const categoryRef = useRef(null)
  const productRef = useRef(null)

  useEffect(() => {
    if (!open) return
    getCategories()
      .then((data) => setCategories(Array.isArray(data) ? data : []))
      .catch(() => setCategories([]))
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
      setCategoryFilter('')
      setProductFilter('')
      setCategoryOpen(false)
      setProductOpen(false)
    }
  }, [open])

  const filteredCategories = categoryFilter.trim()
    ? categories.filter((c) => (c.name || '').toLowerCase().includes(categoryFilter.trim().toLowerCase()))
    : categories

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
      if (categoryRef.current && !categoryRef.current.contains(e.target)) setCategoryOpen(false)
      if (productRef.current && !productRef.current.contains(e.target)) closeProductDropdown()
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [open, productFilterTrim, formData.productCode, products])

  const validateForm = () => {
    const newErrors = {}
    if (!formData.productCode.trim()) {
      newErrors.productCode = '品號為必填'
    }
    if (!formData.productName.trim()) {
      newErrors.productName = '請由品號選擇以帶出品名'
    }
    const qty = Number(formData.quantity)
    if (isNaN(qty) || qty < 0) {
      newErrors.quantity = '箱數小計不可為負數'
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
      const created = await createForecastItem(payload)
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

  const selectCategory = (name) => {
    handleChange('category', name)
    setCategoryFilter('')
    setCategoryOpen(false)
  }

  const selectProduct = (p) => {
    handleChange('productCode', p.code)
    handleChange('productName', p.name)
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
        productName: exact ? exact.name : prev.productName,
      }
    })
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

          <div className="form-field" ref={categoryRef}>
            <label htmlFor="category">中類名稱</label>
            <input
              type="text"
              id="category"
              className="form-input"
              value={categoryOpen ? categoryFilter : formData.category}
              onChange={(e) => {
                setCategoryFilter(e.target.value)
                setCategoryOpen(true)
              }}
              onFocus={() => setCategoryOpen(true)}
              disabled={submitting}
              placeholder="輸入以篩選..."
              autoComplete="off"
            />
            {categoryOpen && (
              <ul className="add-item-dropdown">
                {filteredCategories.length === 0 ? (
                  <li className="add-item-dropdown-empty">無符合項目</li>
                ) : (
                  filteredCategories.slice(0, 100).map((c) => (
                    <li
                      key={c.id}
                      className="add-item-dropdown-item"
                      onClick={() => selectCategory(c.name)}
                    >
                      {c.name}
                    </li>
                  ))
                )}
              </ul>
            )}
          </div>

          <div className="form-field">
            <label htmlFor="spec">貨品規格</label>
            <input
              type="text"
              id="spec"
              className="form-input"
              value={formData.spec}
              onChange={(e) => handleChange('spec', e.target.value)}
              disabled={submitting}
            />
          </div>

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
                if (!productOpen) handleChange('productName', '')
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
            <label htmlFor="productName">
              品名 <span className="required">*</span>
            </label>
            <input
              type="text"
              id="productName"
              className={`form-input form-input--readonly ${errors.productName ? 'form-input--error' : ''}`}
              value={formData.productName}
              readOnly
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
              className="form-input"
              value={formData.warehouseLocation}
              onChange={(e) => handleChange('warehouseLocation', e.target.value)}
              disabled={submitting}
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
