import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import {
  uploadSemiProducts,
  listSemiProducts,
  updateSemiProduct,
  downloadTemplate,
} from '../../api/semiProduct'
import FileDropzone from '../../components/forecast/FileDropzone'
import ConfirmDialog from '../../components/ConfirmDialog'
import './SemiProduct.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

export default function SemiProductImportPage() {
  const { user } = useAuth()
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

  const canView = hasPermission(user, 'semi_product.view')
  const canUpload = hasPermission(user, 'semi_product.upload')
  const canEdit = hasPermission(user, 'semi_product.edit')

  const fetchProducts = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listSemiProducts()
      setProducts(Array.isArray(data) ? data : [])
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限檢視此頁面')
      } else {
        toast.error('無法載入半成品資料')
      }
      setProducts([])
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    if (canView) fetchProducts()
  }, [canView, fetchProducts])

  const handleFileChange = (file) => {
    setFileError('')
    if (!file) {
      setSelectedFile(null)
      return
    }
    const fileName = file.name.toLowerCase()
    if (!fileName.endsWith('.csv') && !fileName.endsWith('.xlsx')) {
      setFileError('僅支援 .csv 或 .xlsx 檔案')
      return
    }
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
      const count = response?.count ?? response?.recordsInserted ?? 0
      toast.success(`成功上傳 ${count} 筆半成品資料`)
      setSelectedFile(null)
      setFileError('')
      await fetchProducts()
    } catch (err) {
      if (err.response?.data?.details && Array.isArray(err.response.data.details)) {
        toast.error(err.response.data.details.join('; '))
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
    if (!canEdit) return
    setEditingId(product.id)
    setEditValue(String(product.advanceDays ?? ''))
    setEditError('')
  }

  const handleEditCancel = () => {
    setEditingId(null)
    setEditValue('')
    setEditError('')
  }

  const validateAdvanceDays = (value) => {
    if (value === '') return '提前日數不可為空'
    const num = parseInt(value, 10)
    if (Number.isNaN(num)) return '請輸入有效的數字'
    if (num <= 0) return '提前日數必須為正整數'
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
    if (e.key === 'Enter') handleEditSave(productId)
    else if (e.key === 'Escape') handleEditCancel()
  }

  const handleExportCsv = () => {
    if (!products.length) {
      toast.info('無資料可匯出')
      return
    }
    const BOM = '\uFEFF'
    const headers = ['品號', '品名', '提前日數']
    const rows = products.map((p) => [
      p.productCode ?? '',
      p.productName ?? '',
      p.advanceDays ?? '',
    ])
    const csv = BOM + [headers.join(','), ...rows.map((r) => r.join(','))].join('\r\n')
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '半成品提前採購設定.csv'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  const canUploadSubmit = selectedFile && !fileError && !uploading

  if (!canView) {
    return (
      <div className="semi-product-page">
        <h1>半成品提前採購設定表單-匯入</h1>
        <div className="semi-product-empty" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  return (
    <div className="semi-product-page">
      <h1>半成品提前採購設定表單-匯入</h1>

      <section className="semi-product-upload-section">
        <h2>上傳區</h2>
        <p className="semi-product-upload-hint">請上傳 CSV 或 Excel (.xlsx) 檔案，欄位：品號、品名、提前日數</p>
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
            {downloadingTemplate ? '下載中...' : '範本下載'}
          </button>
          {canUpload && (
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleUploadClick}
              disabled={!canUploadSubmit}
            >
              {uploading ? (
                <>
                  <span className="upload-spinner" />
                  上傳中...
                </>
              ) : (
                '上傳'
              )}
            </button>
          )}
        </div>
      </section>

      <section className="semi-product-result-section">
        <h2>目前半成品提前採購設定</h2>
        <div className="semi-product-result-toolbar">
          <button
            type="button"
            className="btn btn--outline"
            onClick={fetchProducts}
            disabled={loading}
          >
            {loading ? '查詢中...' : '重新查詢'}
          </button>
          <button
            type="button"
            className="btn btn--outline"
            onClick={handleExportCsv}
            disabled={!products.length}
          >
            Excel 匯出 (CSV)
          </button>
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
                  {canEdit && <th>操作</th>}
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
                            onKeyDown={(e) => handleEditKeyDown(e, product.id)}
                            disabled={saving}
                            autoFocus
                            aria-label="編輯提前日數"
                          />
                          {editError && <div className="edit-error">{editError}</div>}
                        </div>
                      ) : (
                        <span>{product.advanceDays}</span>
                      )}
                    </td>
                    {canEdit && (
                      <td>
                        {editingId === product.id ? (
                          <>
                            <button
                              type="button"
                              className="btn btn--primary btn-sm"
                              onClick={() => handleEditSave(product.id)}
                              disabled={saving}
                            >
                              儲存
                            </button>
                            <button
                              type="button"
                              className="btn btn--outline btn-sm"
                              onClick={handleEditCancel}
                              disabled={saving}
                            >
                              取消
                            </button>
                          </>
                        ) : (
                          <button
                            type="button"
                            className="btn btn--outline btn-sm"
                            onClick={() => handleEditStart(product)}
                          >
                            編輯
                          </button>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

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
