import { useState, useEffect, useCallback } from 'react'
import { listConfigs } from '../../api/forecastConfig'
import {
  getIntegrationVersions,
  getIntegrationData,
  exportIntegrationExcel,
} from '../../api/forecastIntegration'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import './ForecastIntegration.css'

function hasPermission(user, perm) {
  if (user?.permissions && Array.isArray(user.permissions)) {
    return user.permissions.includes(perm)
  }
  if (user?.roleCode === 'admin') return true
  return false
}

function formatMonth(monthStr) {
  if (!monthStr || monthStr.length !== 6) return monthStr
  const year = monthStr.substring(0, 4)
  const month = monthStr.substring(4, 6)
  const monthNames = [
    'January',
    'February',
    'March',
    'April',
    'May',
    'June',
    'July',
    'August',
    'September',
    'October',
    'November',
    'December',
  ]
  const monthIndex = parseInt(month, 10) - 1
  const monthName = monthNames[monthIndex] || ''
  return `${monthStr} (${monthName} ${year})`
}

function formatNumber(value) {
  if (value == null || value === '') return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
}

function getDifferenceClass(value) {
  const num = Number(value)
  if (isNaN(num)) return ''
  if (num > 0) return 'difference-positive'
  if (num < 0) return 'difference-negative'
  return 'difference-zero'
}

export default function ForecastIntegrationPage() {
  const { user } = useAuth()
  const toast = useToast()

  const [configs, setConfigs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)

  const [selectedMonth, setSelectedMonth] = useState('')
  const [selectedVersion, setSelectedVersion] = useState('')

  const [versions, setVersions] = useState([])
  const [loadingVersions, setLoadingVersions] = useState(false)

  const [integrationData, setIntegrationData] = useState([])
  const [loadingData, setLoadingData] = useState(false)

  const [exporting, setExporting] = useState(false)

  const canView = hasPermission(user, 'sales_forecast.view')

  const fetchConfigs = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listConfigs()
      const sorted = [...data].sort((a, b) =>
        b.month > a.month ? 1 : b.month < a.month ? -1 : 0
      )
      setConfigs(sorted)
      setAccessDenied(false)
    } catch (err) {
      if (err.response?.status === 403) {
        setAccessDenied(true)
      } else {
        toast.error('無法載入月份設定')
      }
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => {
    fetchConfigs()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const fetchVersions = useCallback(
    async (month) => {
      if (!month) return

      setLoadingVersions(true)
      try {
        const data = await getIntegrationVersions(month)
        setVersions(data)

        if (data.length > 0) {
          setSelectedVersion(data[0])
        } else {
          setSelectedVersion('')
        }
      } catch (err) {
        toast.error('無法載入版本資料')
        setVersions([])
        setSelectedVersion('')
      } finally {
        setLoadingVersions(false)
      }
    },
    [toast]
  )

  useEffect(() => {
    if (selectedMonth) {
      fetchVersions(selectedMonth)
    } else {
      setVersions([])
      setSelectedVersion('')
    }
  }, [selectedMonth]) // eslint-disable-line react-hooks/exhaustive-deps

  const fetchData = useCallback(
    async (month, version) => {
      if (!month || !version) return

      setLoadingData(true)
      try {
        const data = await getIntegrationData(month, version)
        setIntegrationData(data)
      } catch (err) {
        if (err.response?.status === 403) {
          toast.error('您沒有權限檢視此資料')
        } else {
          toast.error('無法載入整合資料')
        }
        setIntegrationData([])
      } finally {
        setLoadingData(false)
      }
    },
    [toast]
  )

  useEffect(() => {
    if (selectedMonth && selectedVersion) {
      fetchData(selectedMonth, selectedVersion)
    } else {
      setIntegrationData([])
    }
  }, [selectedMonth, selectedVersion]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleMonthChange = (month) => {
    setSelectedMonth(month)
    setSelectedVersion('')
    setVersions([])
    setIntegrationData([])
  }

  const handleVersionChange = (version) => {
    setSelectedVersion(version)
  }

  const handleExport = async () => {
    if (!selectedMonth || !selectedVersion) {
      toast.error('請選擇月份與版本')
      return
    }

    setExporting(true)
    try {
      await exportIntegrationExcel(selectedMonth, selectedVersion)
      toast.success('Excel 檔案已下載')
    } catch (err) {
      if (err.response?.status === 403) {
        toast.error('您沒有權限匯出資料')
      } else {
        toast.error('匯出失敗，請重試')
      }
    } finally {
      setExporting(false)
    }
  }

  if (accessDenied || (!loading && !canView)) {
    return (
      <div className="forecast-integration-page">
        <h1>銷售預測整合 - 12 通路</h1>
        <div className="forecast-access-denied" role="alert">
          您沒有權限檢視此頁面
        </div>
      </div>
    )
  }

  const latestVersion = versions.length > 0 ? versions[0] : null

  return (
    <div className="forecast-integration-page">
      <div className="forecast-page-header">
        <h1>銷售預測整合 - 12 通路</h1>
        {selectedMonth && selectedVersion && (
          <button
            className="btn btn--primary"
            onClick={handleExport}
            disabled={exporting || loadingData}
          >
            {exporting ? '匯出中...' : '匯出 Excel'}
          </button>
        )}
      </div>

      {loading ? (
        <div className="forecast-loading" role="status">
          載入中...
        </div>
      ) : (
        <>
          <div className="forecast-filters">
            <div className="filter-field">
              <label htmlFor="month-select">月份</label>
              <select
                id="month-select"
                className="form-select"
                value={selectedMonth}
                onChange={(e) => handleMonthChange(e.target.value)}
              >
                <option value="">請選擇月份</option>
                {configs.map((c) => (
                  <option key={c.id} value={c.month}>
                    {formatMonth(c.month)}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-field">
              <label htmlFor="version-select">版本</label>
              <select
                id="version-select"
                className="form-select"
                value={selectedVersion}
                onChange={(e) => handleVersionChange(e.target.value)}
                disabled={!selectedMonth || loadingVersions}
              >
                <option value="">請選擇版本</option>
                {versions.map((v) => (
                  <option key={v} value={v}>
                    {v} {v === latestVersion ? '(Latest)' : ''}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {selectedVersion && (
            <div className="forecast-info">
              版本: {selectedVersion} | 產品數: {integrationData?.length}
            </div>
          )}

          {loadingData ? (
            <div className="forecast-loading" role="status">
              載入中...
            </div>
          ) : selectedMonth && selectedVersion ? (
            integrationData?.length === 0 ? (
              <div className="forecast-empty">所選月份與版本無整合資料</div>
            ) : (
              <div className="forecast-table-wrap">
                <table className="forecast-integration-table">
                  <thead>
                    <tr>
                      <th>庫位</th>
                      <th>中類名稱</th>
                      <th>貨品規格</th>
                      <th>品名</th>
                      <th>品號</th>
                      <th className="align-right">PX/大全聯</th>
                      <th className="align-right">家樂福</th>
                      <th className="align-right">愛買</th>
                      <th className="align-right">7-11</th>
                      <th className="align-right">全家</th>
                      <th className="align-right">OK/萊爾富</th>
                      <th className="align-right">好市多</th>
                      <th className="align-right">楓康</th>
                      <th className="align-right">美聯社</th>
                      <th className="align-right">康是美</th>
                      <th className="align-right">電商</th>
                      <th className="align-right">市面經銷</th>
                      <th className="align-right">原始小計</th>
                      <th className="align-right">差異</th>
                      <th>備註</th>
                    </tr>
                  </thead>
                  <tbody>
                    {integrationData?.map((item, index) => (
                      <tr key={index}>
                        <td>{item.warehouseLocation || '-'}</td>
                        <td>{item.category || '-'}</td>
                        <td>{item.spec || '-'}</td>
                        <td>{item.productName || '-'}</td>
                        <td>{item.productCode || '-'}</td>
                        <td className="align-right">{formatNumber(item.pxDaQuanLian)}</td>
                        <td className="align-right">{formatNumber(item.jiaLeFu)}</td>
                        <td className="align-right">{formatNumber(item.aiMai)}</td>
                        <td className="align-right">{formatNumber(item.sevenEleven)}</td>
                        <td className="align-right">{formatNumber(item.quanJia)}</td>
                        <td className="align-right">{formatNumber(item.okLaiErFu)}</td>
                        <td className="align-right">{formatNumber(item.haoShiDuo)}</td>
                        <td className="align-right">{formatNumber(item.fengKang)}</td>
                        <td className="align-right">{formatNumber(item.meiLianShe)}</td>
                        <td className="align-right">{formatNumber(item.kangShiMei)}</td>
                        <td className="align-right">{formatNumber(item.dianShang)}</td>
                        <td className="align-right">{formatNumber(item.shiFanJingXiao)}</td>
                        <td className="align-right">{formatNumber(item.originalSubtotal)}</td>
                        <td className={`align-right ${getDifferenceClass(item.difference)}`}>
                          {formatNumber(item.difference)}
                        </td>
                        <td>{item.remarks || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : (
            <div className="forecast-empty">請選擇月份與版本以檢視資料</div>
          )}
        </>
      )}
    </div>
  )
}
