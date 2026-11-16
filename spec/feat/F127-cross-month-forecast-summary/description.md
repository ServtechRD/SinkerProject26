# F127 - 銷售預估跨月彙整查詢（大檔期）

## 背景／痛點
系統原設計為逐月上傳、逐月查看彙總。遇大檔期（中元 6–8 月、過年 CNY 11–2 月），生管與採購需在檔期**之前**
一次評估全期的預估量，目前只能逐月切換彙總表、自行加總，且無法看到各月份業務備註的全貌。

## 需求（方案 B：維持單月上傳，生管端提供跨月一鍵彙整）
1. 在「銷售預估」列表頁新增頁籤「跨月彙整」，選起訖月份（最多 4 個月，可跨年，如 202611～202702）後查詢。
2. 每個品項一列：各月合計（12 通路加總，含贈品）＋全期合計。
3. 範圍內**未關帳月份也納入**，表頭標示「未關帳」。
4. 備註沿用 F126：按鈕「備註」＋彈窗，逐行列出 `[通路/M月] 內容`，**先依月份、再依通路**排序。
5. 唯讀，不可編輯；可匯出 CSV。

## 使用者已確認的決策（2026-11-11）
- 放置位置：銷售預估頁新增頁籤（不改生產表單；生產表單依賴庫存整合版本與 ERP stub，另案處理）。
- 未關帳月份：納入並標記。
- 顯示細度：每月合計＋全期合計（不展開通路 × 月份）。
- 備註排序：先月份、再通路。

## 每月資料來源
| 月份狀態 | 資料來源 |
|---|---|
| 已關帳 | 最新表單版本（`FormSummaryService.getFormSummaryByVersion(month, 最大 version_no)`，含生管編輯結果） |
| 未關帳／無月份設定 | 各通路最新上傳＋禮品（`FormSummaryService.getFormSummaryLegacy(month)`） |

單月數量 = 該品項各通路 `current_qty` 加總（與單月彙總頁一致，已含禮品）。

## 實作要點
### 後端
- 新增 `GET /api/sales-forecast/form-summary/range?start_month=YYYYMM&end_month=YYYYMM`，
  權限同單月彙總：`@PreAuthorize("hasAuthority('sales_forecast.update_after_closed')")`。
- 月份驗證與展開沿用 `ProductionFormService.validateMonthRange` / `listMonthsInRange`
  （起 ≤ 迄、最多 4 個月、跨年），抽成可共用的靜態方法，不重寫。
- `FormSummaryService` 新增 `getFormSummaryRange(start, end)`：逐月呼叫既有單月彙總，**依品號（product_code）合併**；
  品名／規格／中類／庫位取範圍內最後一個有該品號的月份。
- 回應：
  ```json
  {
    "months": [{"month": "202606", "closed": true}, ...],
    "rows": [{
      "product_code": "...", "product_name": "...", "category": "...", "spec": "...", "warehouse_location": "...",
      "month_quantities": {"202606": 300, "202607": 500},
      "total": 800,
      "remarks": [{"channel": "...", "month": "202606", "text": "..."}]
    }]
  }
  ```
  `month_quantities` 對無資料月份填 0；`remarks` 依月份、再依 CHANNEL_ORDER 排序（逐月串接 F126 的 remarks 即可）。
- 不新增資料表、不需 Flyway 遷移、不改上傳與關帳流程。

### 前端
- `ForecastListPage.jsx` 加頁籤切換（單月彙總／跨月彙整），跨月內容放新元件 `CrossMonthSummary.jsx`（同目錄）。
- 起訖月份下拉選單取自月份設定清單（`listConfigs`，含未關帳）；超過 4 個月或起 > 迄時前端先擋並提示。
- 表格欄位：庫位、中類名稱、貨品規格、品名、品號、各月（表頭 `YYYY-MM`，未關帳加標記）、全期合計、備註。
- 備註按鈕＋彈窗與 F126 共用：彈窗抽成同目錄 `RemarkListModal.jsx`，備註文字／CSV 工具抽成 `remarkUtils.js`（樣式沿用 `ForecastList.css`，兩處皆在 sales-forecast 目錄，故不放 components/）。
- 匯出 CSV：欄位同表格，備註多則以換行分隔（沿用 F126 的 CSV 跳脫）。
- UI 用語遵循 `spec/task/X008-ui-text-zh-tw/glossary.md`。

## 相依
- 依賴 F126（`FormSummaryRowDTO.remarks`、`ChannelRemarkDTO`、備註彈窗）。F126 合併前，本任務分支自 F126 分支建立。

## 不在範圍
- 多月份上傳範本（方案 A）。
- 生產表單（`ProductionFormService`）的品項清單與備註修正、ERP 結存／銷貨串接。
- 「未關帳月份取最新版本只取到單一通路」的庫存整合問題（`InventoryIntegrationService.getLatestForecastVersion`），另開 fix。
- 大檔期快捷按鈕（中元／CNY）。
