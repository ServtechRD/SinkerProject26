# R4 截止日與鎖定邏輯（含開放補登）設計

- 任務編號：F129（分支 `feat/F129-forecast-cutoff-reopen`）
- 需求來源：`成偉_銷售預估平台_第一階段調整_需求確認.pdf` 第 17–21 頁（R4）、待確認事項第 5、6、7 項
- 既有拆解：`docs/superpowers/plans/成偉修改計畫書.md` R4 章節（本文件取代其 R4 的資料結構與流程設計）
- 日期：2026-11-16

## 1. 目標

截止時間一到自動鎖定，業務與生管看到同一份數字。截止後只有兩條路：

1. **生管直接修正**：修改原因必填，產生新的生管表單版本，表單維持鎖定。
2. **開放補登**：只對指定通路開放，有到期時間，到期自動鎖回並把補登資料併成新的生管版本。

整張表單結束新增後不可還原（沿用現行規則）。所有鎖定、開放、修正動作都留異動紀錄。

## 2. 已確認的決策

| # | 決策 | 結論 |
|---|------|------|
| D1 | 範圍 | R4-1～R4-5 一次做完 |
| D2 | 「補登中」狀態的存法 | 方案 A：`sales_forecast_config.is_closed` 不變，另建補登期間表，以「月份＋通路」為單位 |
| D3 | 補登資料何時變成生管新版本 | 補登**結束時**（到期或生管提前結束）才產生；同一次處理到的多個通路合併成一個版本 |
| D4 | 一次開放多通路 | 可以複選；同一交易內每通路各建一筆，任一通路不合規則則整批不開 |
| D5 | 工作天計算 | 只排除週六、週日，不處理國定假日 |
| D6 | Email 通知 | 本版不做（平台無寄信機制，PDF 待確認第 6 項），視窗也不放勾選框 |
| D7 | 修改原因 | 必填（PDF R4-3 / 待確認第 7 項，依規格預設） |
| D8 | 檔期（R2-A） | 不在本範圍，只處理單月表單 |

## 3. 資料結構（Flyway `V35__forecast_cutoff_reopen.sql`）

### 3.1 `sales_forecast_config`
- 新增 `auto_close_at DATETIME NOT NULL`（台灣時間）。
- 以 `auto_close_date` 當天 00:00 回填後，移除 `auto_close_date`。

### 3.2 新表 `sales_forecast_reopen_window`
| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | INT PK AI | |
| `month` | VARCHAR(7) | 對應 `sales_forecast_config.month` |
| `channel` | VARCHAR(50) | 通路名稱（R3 主檔上線後改通路代碼） |
| `expires_at` | DATETIME | 補登截止時間（台灣時間） |
| `reason` | VARCHAR(200) | 開放原因，必填 |
| `opened_by` | BIGINT | 開放者 user id |
| `opened_at` | DATETIME | |
| `ended_at` | DATETIME NULL | 到期或提前結束時寫入；NULL 代表補登中 |
| `ended_by` | BIGINT NULL | 提前結束者；到期自動鎖回為 NULL |
| `merged_version` | VARCHAR(20) NULL | 結束時產生的生管版本（如 `form_v3`） |

索引：`(month, channel, ended_at)`。

### 3.3 新表 `sales_forecast_form_log`（只新增，不提供修改、刪除端點）
| 欄位 | 型別 | 說明 |
|------|------|------|
| `id` | INT PK AI | |
| `month` | VARCHAR(7) | |
| `channel` | VARCHAR(50) NULL | NULL = 全部通路 |
| `action` | VARCHAR(30) | `AUTO_CLOSE`、`MANUAL_CLOSE`、`REOPEN`、`REOPEN_END`、`CORRECTION` |
| `operator_id` | BIGINT NULL | NULL = 系統 |
| `detail` | VARCHAR(500) | 原因、期限、產生的版本等 |
| `created_at` | DATETIME | |

### 3.4 權限 35
- `sales_forecast_config.reopen`「開放補登」，預設授予 `production_planner`（生管）與 `admin`。

## 4. 後端流程

### 4.1 截止（R4-1）
- `CreateMonthsRequest`、`UpdateConfigRequest`、`ConfigResponse` 改用 `autoCloseAt`（`LocalDateTime`，台灣時間）。
- `AutoCloseScheduler` 改為每分鐘執行（`cron = "0 * * * * ?"`）：
  - 關閉 `is_closed = false AND auto_close_at <= now` 的月份，沿用既有關閉流程（含產生 `form_v1`），寫 `AUTO_CLOSE`。
  - 處理到期補登（見 4.4）。
- 生管手動結束寫 `MANUAL_CLOSE`；結束後不可改回開放（既有規則保留）。
- 現在時間一律取 `LocalDateTime.now(ZoneId.of("Asia/Taipei"))`，集中在一個 `Clock` bean，方便測試。

### 4.2 業務上傳／編輯前的統一檢查
- `SalesForecastConfigService.assertBusinessEditable(month, channel)`：
  - 月份未設定 → 404（沿用）。
  - `is_closed = false` → 允許。
  - 該月份＋通路有 `ended_at IS NULL AND expires_at > now` 的補登 → 允許。
  - 其餘 → 403「表單已截止」。
- 取代下列 4 處 `isClosed` 檢查：`SalesForecastUploadService`、`GiftSalesForecastUploadService`、`SalesForecastService.validateMonthOpen`、`GiftSalesForecastService` 對應處。
- 有 `sales_forecast.update_after_closed` 權限者略過檢查的既有行為不變。

### 4.3 開放補登（R4-4）
- `POST /api/sales-forecast/config/{id}/reopen`，`@PreAuthorize("hasAuthority('sales_forecast_config.reopen')")`。
- 請求：`{ channels: string[], expiresAt: datetime, reason: string }`，`channels` 非空、`reason` `@NotBlank` 且 ≤200 字。
- 檢查（任一不過 → 400，整批不建立）：
  - 表單已結束新增。
  - `now < expiresAt <= addWorkingDays(now, 3)`（D5：跳過週六日，保留時分）。
  - 每個通路是合法通路，且目前沒有進行中的補登（訊息列出衝突通路）。
- 每通路建一筆 `sales_forecast_reopen_window`，寫一筆 `REOPEN`。
- `POST /api/sales-forecast/config/{id}/reopen/{windowId}/end`（權限 35）：生管提前結束單一通路，立即走 4.4。

### 4.4 補登結束與合併
- 觸發：排程找出 `ended_at IS NULL AND expires_at <= now` 的補登；或提前結束端點。
- 同一月份、同一次處理到的通路合併處理：
  1. 取該月最新生管版本 `form_vN` 的所有列。
  2. 這些通路的列，改用業務最新上傳資料（銷售＋禮品，組法同 `createFormVersion1Snapshot` 的單通路部分）。
  3. 存成 `form_vN+1`，修改原因「補登：<通路清單>／<開放原因>」。
  4. 每筆補登寫 `ended_at`、`ended_by`、`merged_version`，各寫一筆 `REOPEN_END`。
- 修正 `SalesForecastUploadService` 清空舊資料：`deleteByMonthAndChannel` 改為只刪非 `form_v%` 的列，避免補登上傳洗掉生管版本。

### 4.5 生管直接修正（R4-3）
- `SaveFormSummaryVersionRequest.changeReason` 加 `@NotBlank`。
- `FormSummaryService.saveFormSummaryVersion` 成功後寫 `CORRECTION`（detail 含版本號與原因）。

### 4.6 查詢
- 既有列表 `GET /api/sales-forecast/config` 的 `ConfigResponse` 加 `activeReopenCount`（進行中補登的通路數）。
- `GET /api/sales-forecast/config/{id}/detail`（`sales_forecast_config.view`）：
  - `config`（含狀態、`autoCloseAt`、`closedAt`、關閉方式＝最近一筆 `AUTO_CLOSE`／`MANUAL_CLOSE` 紀錄）。
  - `channels[]`：`channel`、`salesLastUploadAt`、`giftLastUploadAt`（排除 `form_v%`）、`activeWindow`（id、expiresAt、reason，或 null）、`correctionCount`（最新生管版本與 `form_v1` 相比該通路數量不同的品項數）。
  - 通路清單沿用 `FormSummaryService.CHANNEL_ORDER`（R3 主檔上線後改讀主檔）。
- `GET /api/sales-forecast/config/{id}/logs`（`sales_forecast_config.view`）：異動紀錄，新到舊；操作者以使用者顯示名稱回傳，系統動作回「系統」。
- 業務上傳頁查詢（既有 `GET` 銷售／禮品資料端點）回應增加：
  - `editStatus`：`OPEN` / `CLOSED` / `REOPENED`，以及 `REOPENED` 時的 `reopenExpiresAt`、`reopenReason`、`autoCloseAt`。
  - 每列 `plannerLatestQuantity`：最新生管版本中該通路該品項的數量（表單未結束時為 null）。

### 4.7 錯誤處理
- 規則違反 → 400，`message` 為中文（例：「PX+大全聯 已在補登中」「補登期限不可超過 2026/12/15 18:00」）；前端沿用「優先顯示後端 message」。
- 權限不足 → 403。找不到月份 → 404。
- 合併失敗時整筆交易回滾，補登維持未結束，排程下一分鐘重試並記 ERROR log。

## 5. 前端

### 5.1 表單設定列表 `ForecastConfigPage`
- 截止欄顯示日期加時間。
- 月份為連結，進入明細頁。
- 已結束且有補登中通路時，狀態旁加橘色標籤「補登中 N 通路」（列表 API 回應加 `activeReopenCount`）。

### 5.2 `CreateMonthsDialog`、`EditConfigDialog`
- 截止改 `<input type="datetime-local">`。
- 已結束月份不提供改回開放。
- 更新這兩支元件的 12 個過時測試，及 `api/__tests__/forecastConfig.test.js` 1 個過時測試。

### 5.3 表單明細頁 `ForecastConfigDetailPage`（路由 `/sales-forecast/config/:id`，PDF 第 20 頁）
- 頁首：狀態標籤、截止時間與方式；表單已結束且有權限 35 時顯示「開放補登」。
- 各通路狀態表：通路、業務最後上傳（銷售／禮品，未上傳紅字）、補登狀態（期限、剩餘時間）、生管修正筆數；補登中且有權限 35 時顯示「提前結束」。
- 開放補登視窗 `ReopenDialog`：通路複選（補登中的不可選）、補登截止時間（顯示上限，驗證交後端）、開放原因必填、選到 `correctionCount > 0` 的通路時顯示黃色覆蓋提醒。
- 異動紀錄表：時間、操作者、動作、通路、內容。

### 5.4 業務上傳頁 `ForecastUploadPage`、`GiftForecastUploadPage`（PDF 第 19 頁）
- `CLOSED`：紅色提示列（狀態、截止時間、「如需異動請洽生管」），上傳、新增、編輯反灰。
- `REOPENED`：橘色提示列（補登期限、剩餘時間、開放原因），按鈕可用。
- 表單已結束時加「生管最新版數量」欄，與業務數量不同時紅字。
- 提示列擴充既有 `SalesForecastFillDeadlineBanner`。

### 5.5 生管表單 `ForecastListPage`
- 「生管備註（修改原因）」必填，未填不可儲存。

## 6. 測試與驗收

### 後端單元（Mockito）
- 工作天：週一、週五、週六、週日開放的上限計算。
- `assertBusinessEditable`：開放中／已結束／本通路補登中／補登已到期／他通路補登中。
- 開放補登：表單未結束、期限超過上限、期限已過、通路已在補登中 → 整批失敗；成功時每通路一筆＋紀錄。
- 合併：多通路合成一版、其他通路沿用前版數量、上傳不刪 `form_v%`。
- 排程：自動截止寫 `AUTO_CLOSE`；到期補登結束並合併。
- 修改原因空白 → 驗證失敗。

### 後端整合（TestContainers）
- 截止 → 開放補登 → 業務補登上傳 → 提前結束 → 檢查 `form_vN+1` 數量與異動紀錄。
- 無權限 35 呼叫開放補登 → 403。

### 前端（Vitest）
- 明細頁：通路狀態、按鈕依權限與狀態顯示、異動紀錄。
- `ReopenDialog`：必填、覆蓋提醒、送出參數。
- 上傳頁：紅／橘提示列與按鈕反灰、生管最新版數量紅字。
- `ForecastListPage`：修改原因必填。
- 修好 13 個過時測試。

### 驗收條件
- 後端 `./gradlew test` 全綠（docker，`-p sinker_test`）。
- 前端 `vitest run` 全綠、改動檔案 ESLint 通過。
- 開發環境實測：截止後業務上傳被擋 → 開放補登後可上傳 → 提前結束後生管表單出現新版本、明細頁有對應紀錄。

## 7. 不在本範圍
- Email 通知（D6）、國定假日（D5）、檔期截止（D8）、通路主檔（R3）。
