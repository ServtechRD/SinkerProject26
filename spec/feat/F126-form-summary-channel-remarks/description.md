# F126 - 彙總表單備註依通路／月份逐行顯示

## 背景／痛點
業務上傳預估量（含贈品預估）時在 Excel 第 7 欄填寫的備註（如「12月替換新品」「9/30前需生產完成」），
上傳時已正確存入 `sales_forecast.remark` / `gift_sales_forecast.remark`，
但生管在「總體銷售預估量彙總表單」看不到大部分備註。

## 根因
`FormSummaryService` 以品號（productKey）把 12 通路合併成一列：
- `keyToRow.putIfAbsent(pk, rowFrom(r))` 只保留第一個遇到之通路的列與其備註。
- `createFormVersion1Snapshot` / `saveFormSummaryVersion` 寫快照時，只有 `remarkChannelByProductKey` 記錄的單一通路保留備註，其餘通路被寫成空字串。
- 若第一個通路無備註而後續通路有備註，`row.getRemark()` 為空，備註整則遺失。
- 前端 `ForecastListPage.jsx` 每品項只顯示單一 `row.remark`，無通路／月份標示。

## 需求
1. 彙總表單同一品項顯示**所有通路**的非空備註（銷售＋贈品），每則一行，格式：`[通路/M月] 備註內容`。
   - 例：`[全聯/12月] 12月替換新品`
   - 排序依 `CHANNEL_ORDER`。
2. 關帳產生第 1 版快照、生管儲存新版本時，**每個通路的列保留各自備註**，不再只留單一來源通路。
3. 生管於編輯模式可**逐則編輯**各通路備註（使用者決策 (b)），儲存後寫回對應通路列。
4. 匯出 Excel 的備註欄使用相同格式，同一儲存格以換行分隔。

## 使用者已確認的決策（2026-11-10）
- 顯示：備註欄為按鈕「備註」，點擊後彈窗（modal）逐行列出 `[通路/M月] 內容`（2026-11-10 由逐行顯示改為按鈕＋彈窗）。
- 編輯：(b) 逐則可編輯。
- 舊資料：已關帳月份之舊快照**不回補**，只修正新產生的版本。
- 範圍：與「大檔期跨月彙整」分開規格；本任務為單月彙總，月份標示取自該彙總月份。

## 實作要點
### 後端
- `FormSummaryRowDTO` 以 `remarks: List<ChannelRemarkDTO>` 取代原單一 `remark` 欄位，`ChannelRemarkDTO { channel, month, text }`（前後端同步上線，`remark` 已無其他使用者）。
- `getFormSummaryByVersion`、`getFormSummaryLegacy`：逐通路收集非空備註填入 `remarks`；未合併版本（非 `form_v*`）同時收集贈品備註。
- `createFormVersion1Snapshot`、`saveFormSummaryVersion`：移除 `remarkChannelByProductKey` 單一來源邏輯，改為每通路列寫入該通路自己的備註（同一通路銷售與贈品皆有備註時以「；」串接）。
- `SaveFormSummaryVersionRequest.FormSummaryRowEditDTO` 以 `remarks: List<ChannelRemarkDTO>` 取代 `remark`，後端依 `channel` 寫回對應通路列（`month` 忽略）。
- 前端匯出為 CSV，備註欄多則以換行分隔，所有欄位做 CSV 跳脫（含逗號／換行時加雙引號）。
- 不需 Flyway 遷移（`remark` 欄位已存在）。

### 前端
- `ForecastListPage.jsx` 備註欄：有備註時顯示按鈕「備註」，無備註顯示「-」；點擊開啟彈窗，唯讀模式逐行列出 `[通路/M月] 內容`，編輯模式每則一個輸入框（前綴標示通路）；未有備註的通路不顯示輸入框。
- 匯出 Excel 備註欄以 `\n` 串接。
- UI 用語遵循 `spec/task/X008-ui-text-zh-tw/glossary.md`。

## 不在範圍
- 跨月彙整查詢、多月份上傳範本（另開規格）。
- 舊快照備註回補。
- 生產表單（`ProductionFormService`）的備註來源調整。
