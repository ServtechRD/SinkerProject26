import { useState, useEffect, useCallback } from 'react'
import { uploadSemiProducts, listSemiProducts, updateSemiProduct, downloadTemplate } from '../../api/semiProduct'
import { useToast } from '../../components/Toast'
import FileDropzone from '../../components/forecast/FileDropzone'
import ConfirmDialog from '../../components/ConfirmDialog'
import './SemiProduct.css'

export default function SemiProductPage() {
  const toast = useToast()

  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedFile, setSelectedFile] = useState(null)
  const [fileError, setFileError] = useState('')
  const [uploading, setUploading] = useState(false)
  const [downloadingTemplate, setDownloadingTemplate] = useState(false)
  const [showUploadConfirm, setShowUploadConfirm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [editValue, setEditValue] = useState('')
  const [editError, setEditError] = useState('')
  const [saving, setSaving] = useState(false)

  const fetchProducts = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listSemiProducts()
      setProducts(data)
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限檢視此頁面')
      } else {
        toast.error('無法載入半成品資料')
      }
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    fetchProducts()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleFileChange = (file) => {
    setFileError('')

    if (!file) {
      setSelectedFile(null)
      return
    }

    // Validate file type
    const fileName = file.name.toLowerCase()
    if (!fileName.endsWith('.xlsx')) {
      setFileError('僅支援 .xlsx 檔案')
      return
    }

    // Validate file size (10MB)
    const maxSize = 10 * 1024 * 1024
    if (file.size > maxSize) {
      setFileError('檔案大小超過 10MB 上限')
      return
    }

    setSelectedFile(file)
  }

  const handleDownloadTemplate = async () => {
    setDownloadingTemplate(true)
    try {
      await downloadTemplate()
    } catch (err) {
      toast.error('下載範本失敗')
    } finally {
      setDownloadingTemplate(false)
    }
  }

  const handleUploadClick = () => {
    if (!selectedFile || fileError) return
    setShowUploadConfirm(true)
  }

  const handleUploadConfirm = async () => {
    setShowUploadConfirm(false)
    if (!selectedFile) return

    setUploading(true)
    try {
      const response = await uploadSemiProducts(selectedFile)
      const count = response.count || response.rows_processed || 0
      toast.success(`成功上傳 ${count} 筆半成品資料`)
      setSelectedFile(null)
      setFileError('')
      await fetchProducts()
    } catch (err) {
      if (err.response?.data?.details && Array.isArray(err.response.data.details)) {
        const errorMessages = err.response.data.details.join('; ')
        toast.error(errorMessages)
      } else if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('上傳失敗，請重試')
      }
    } finally {
      setUploading(false)
    }
  }

  const handleEditStart = (product) => {
    setEditingId(product.id)
    setEditValue(String(product.advanceDays || ''))
    setEditError('')
  }

  const handleEditCancel = () => {
    setEditingId(null)
    setEditValue('')
    setEditError('')
  }

  const validateAdvanceDays = (value) => {
    if (value === '') {
      return '提前日數不可為空'
    }
    const num = parseInt(value, 10)
    if (isNaN(num)) {
      return '請輸入有效的數字'
    }
    if (num <= 0) {
      return '提前日數必須為正整數'
    }
    return ''
  }

  const handleEditSave = async (productId) => {
    const error = validateAdvanceDays(editValue)
    if (error) {
      setEditError(error)
      return
    }

    setSaving(true)
    try {
      const advanceDays = parseInt(editValue, 10)
      await updateSemiProduct(productId, advanceDays)
      setProducts((prev) =>
        prev.map((p) => (p.id === productId ? { ...p, advanceDays } : p))
      )
      toast.success('提前日數更新成功')
      setEditingId(null)
      setEditValue('')
      setEditError('')
    } catch (err) {
      if (err.response?.data?.error) {
        toast.error(err.response.data.error)
      } else {
        toast.error('更新失敗')
      }
    } finally {
      setSaving(false)
    }
  }

  const handleEditKeyDown = (e, productId) => {
    if (e.key === 'Enter') {
      handleEditSave(productId)
    } else if (e.key === 'Escape') {
      handleEditCancel()
    }
  }

  const canUpload = selectedFile && !fileError && !uploading

  return (
    <div className="semi-product-page">
      <h1>半成品提前採購設定</h1>

      <div className="semi-product-upload-section">
        <div className="upload-area">
          <FileDropzone
            file={selectedFile}
            onFileChange={handleFileChange}
            error={fileError}
            disabled={uploading}
          />
        </div>

        <div className="upload-actions">
          <button
            type="button"
            className="btn btn--outline"
            onClick={handleDownloadTemplate}
            disabled={downloadingTemplate}
          >
            {downloadingTemplate ? '下載中...' : '下載範本'}
          </button>
          <button
            type="button"
            className="btn btn--primary"
            onClick={handleUploadClick}
            disabled={!canUpload}
          >
            {uploading ? (
              <>
                <span className="upload-spinner"></span>
                上傳中...
              </>
            ) : (
              '上傳'
            )}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="semi-product-loading" role="status">
          載入中...
        </div>
      ) : products.length === 0 ? (
        <div className="semi-product-empty">尚無半成品資料</div>
      ) : (
        <div className="semi-product-table-wrap">
          <table className="semi-product-table">
            <thead>
              <tr>
                <th>品號</th>
                <th>品名</th>
                <th>提前日數</th>
                <th>最後更新時間</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>{product.productCode}</td>
                  <td>{product.productName}</td>
                  <td className="editable-cell">
                    {editingId === product.id ? (
                      <div className="edit-input-wrapper">
                        <input
                          type="text"
                          className={`edit-input ${editError ? 'edit-input--error' : ''}`}
                          value={editValue}
                          onChange={(e) => setEditValue(e.target.value)}
                          onBlur={() => handleEditSave(product.id)}
                          onKeyDown={(e) => handleEditKeyDown(e, product.id)}
                          disabled={saving}
                          autoFocus
                          aria-label="編輯提前日數"
                        />
                        {editError && <div className="edit-error">{editError}</div>}
                      </div>
                    ) : (
                      <div
                        className="editable-value"
                        onClick={() => handleEditStart(product)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            handleEditStart(product)
                          }
                        }}
                        aria-label={`編輯提前日數，當前值：${product.advanceDays}`}
                      >
                        {product.advanceDays}
                        <span className="edit-icon">✎</span>
                      </div>
                    )}
                  </td>
                  <td>
                    {product.updatedAt
                      ? new Date(product.updatedAt).toLocaleString('zh-TW', {
                          year: 'numeric',
                          month: '2-digit',
                          day: '2-digit',
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConfirmDialog
        open={showUploadConfirm}
        title="確認上傳"
        message="上傳檔案將會取代所有現有的半成品設定資料。確定要繼續嗎？"
        onConfirm={handleUploadConfirm}
        onCancel={() => setShowUploadConfirm(false)}
        loading={uploading}
        confirmText="確認"
        confirmButtonClass="btn--primary"
      />
    </div>
  )
}
