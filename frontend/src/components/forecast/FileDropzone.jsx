import { useState } from 'react'
import './FileDropzone.css'

export default function FileDropzone({ file, onFileChange, error, disabled }) {
  const [dragging, setDragging] = useState(false)

  const handleDragOver = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (!disabled) {
      setDragging(true)
    }
  }

  const handleDragLeave = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragging(false)
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragging(false)

    if (disabled) return

    const droppedFiles = e.dataTransfer.files
    if (droppedFiles && droppedFiles.length > 0) {
      onFileChange(droppedFiles[0])
    }
  }

  const handleFileInput = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      onFileChange(e.target.files[0])
    }
  }

  const handleClearFile = () => {
    onFileChange(null)
  }

  const formatFileSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
  }

  return (
    <div className="file-dropzone-wrapper">
      {!file ? (
        <div
          className={`file-dropzone ${dragging ? 'file-dropzone--dragging' : ''} ${disabled ? 'file-dropzone--disabled' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => !disabled && document.getElementById('file-input').click()}
          role="button"
          tabIndex={disabled ? -1 : 0}
          aria-label="上傳檔案區域"
        >
          <div className="file-dropzone-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
              />
            </svg>
          </div>
          <div className="file-dropzone-text">拖放 Excel 檔案到此處，或點擊瀏覽</div>
          <div className="file-dropzone-hint">僅支援 .xlsx 檔案，檔案大小上限 10MB</div>
          <input
            id="file-input"
            type="file"
            accept=".xlsx"
            onChange={handleFileInput}
            style={{ display: 'none' }}
            disabled={disabled}
          />
        </div>
      ) : (
        <div className="file-selected">
          <div className="file-selected-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
          </div>
          <div className="file-selected-info">
            <div className="file-selected-name">{file.name}</div>
            <div className="file-selected-size">{formatFileSize(file.size)}</div>
          </div>
          {!disabled && (
            <button
              type="button"
              className="file-selected-remove"
              onClick={handleClearFile}
              aria-label="移除檔案"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          )}
        </div>
      )}
      {error && <div className="file-dropzone-error">{error}</div>}
    </div>
  )
}
