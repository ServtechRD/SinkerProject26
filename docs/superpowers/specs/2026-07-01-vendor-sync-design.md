# 廠商主檔資料同步 (Vendor Sync) 設計

- 日期：2026-07-01
- 狀態：待實作

## 背景與目標

目前系統會從外部 ERP 系統同步商品主檔（`ErpProductSyncClient` / `ErpProductSyncService`），但沒有廠商（供應商）主檔。需要新增一支「廠商資料同步」功能，定期／手動從 ERP 拉取廠商清單存進本機資料庫，之後供其他功能（例如採購單建立時選廠商）查詢使用。

外部 API 目前已知的回應格式（陣列，無分頁包裝）：

```json
[
  {
    "CusNo": "00",
    "Name": "總公司",
    "SysDate": "2010-05-03 14:16:34",
    "ModifyDate": "2025-07-22 14:25:00"
  }
]
```

- `SysDate` / `ModifyDate` 可能不存在（null 或缺欄位）。
- Request 參數格式尚未最終確認，本設計先用最小可行欄位（`IsOnlyUpdate` / `NowPage` / `PageSize`），待確認實際過濾欄位（如廠商名稱搜尋）後再擴充，不影響本次的核心同步與儲存邏輯。

## 架構總覽

沿用既有商品同步的架構模式（`Config → Client → Service → Controller`），另外新增一個獨立的查詢 Controller 供其他功能查詢廠商清單：

```
IntegrationProperties.ErpVendor (設定: enabled / vendorListUrl)
        │
ErpVendorSyncClient  ──POST──▶  外部 ERP 廠商列表 API
        │  (回傳 List<VendorSyncItem>，純陣列、無分頁包裝)
ErpVendorSyncService (@Async，逐頁呼叫直到某頁筆數 < pageSize，upsert 進 vendor 表)
        │
IntegrationController (POST /api/integrations/erp/vendor-sync, GET .../status)
VendorController        (GET /api/vendors — 查詢用)
        │
frontend: ErpVendorSyncPage.jsx (仿 ErpProductSyncPage，手動觸發 + 狀態輪詢)
```

### 為什麼這樣切

- 同步觸發端點併入既有 `IntegrationController`：跟 PDCA 重算、ERP 採購單、商品同步放在一起，符合現有慣例（該 Controller 專職「觸發對外整合」）。
- 查詢另開 `VendorController`：對應「廠商」這個資源本身，符合現有慣例（如 `ProductController` 之於商品）。

## 資料庫 (Flyway Migration)

新增 `backend/src/main/resources/db/migration/V34__create_vendor.sql`（目前最新版號為 V33）：

```sql
CREATE TABLE vendor (
    id            INT             NOT NULL AUTO_INCREMENT,
    code          VARCHAR(50)     NOT NULL,
    name          VARCHAR(500)    NOT NULL,
    sys_date      DATETIME        NULL,
    modify_date   DATETIME        NULL,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vendor_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- `code`（對應 `CusNo`）唯一，是 upsert 用的自然鍵，比照 `product.code`。
- 欄位命名直接對應 response 欄位（`sys_date` / `modify_date`），不加 `erp_` 前綴。
- 兩個日期欄位皆可為 NULL，因 ERP 回傳時不一定會有。
- charset / collation 與現有 `product` 表一致。

## 後端 DTO

放在 `backend/src/main/java/com/sinker/app/dto/erp/`：

**`VendorSyncItem`**（外部 API 回應陣列中的單筆項目，全部 `String`，日期格式 `"yyyy-MM-dd HH:mm:ss"`）
- `CusNo`, `Name`, `SysDate`, `ModifyDate`

**`VendorListRequest`**（送給外部 API 的 request body）
- `IsOnlyUpdate` (boolean)
- `NowPage` (int, 預設 1)
- `PageSize` (int, 預設 1000)
- 過濾欄位（如廠商名稱）暫不加，待確認實際格式後再擴充

**`VendorSyncParam`**（Controller 接收的觸發參數）
- `IsOnlyUpdate` (Boolean, 預設 false)
- `PageSize` (Integer, 預設 1000)

## Entity / Repository

`backend/src/main/java/com/sinker/app/entity/Vendor.java`：`id`, `code`, `name`, `sysDate`, `modifyDate`, `createdAt`, `updatedAt`（比照 `Product` entity 寫法）。

`VendorRepository extends JpaRepository<Vendor, Integer>`：
- `findByCodeIn(Collection<String> codes)` — upsert 比對用
- `findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name)` — 查詢 API 用的關鍵字搜尋；`VendorService` 呼叫時同一個 `keyword` 值會同時傳入 `code` 與 `name` 兩個參數，達到「比對代號或名稱任一符合」的效果

## 設定 (IntegrationProperties / application.yml)

`IntegrationProperties` 新增 `ErpVendor` 內部類別（`enabled`, `vendorListUrl`），比照 `ErpProduct`。

`application.yml` 新增：

```yaml
erp-vendor:
  enabled: ${ERP_VENDOR_ENABLED:false}
  vendor-list-url: ${ERP_VENDOR_LIST_URL:}
```

## 同步邏輯 (ErpVendorSyncClient / ErpVendorSyncService)

**`ErpVendorSyncClient`**：結構同 `ErpProductSyncClient`，差異在回傳型別為 `List<VendorSyncItem>`（用 `ParameterizedTypeReference<List<VendorSyncItem>>` 接純陣列回應，而非包了 `Datas/NowPage/TotalPage` 的物件）。401 時清除 token 快取並重試一次。

**`ErpVendorSyncService`**：
- `syncVendorsAsync(param)`：`@Async`，用 `AtomicBoolean running` 防止重複觸發；記錄 `lastStartedAt` / `lastFinishedAt` / `lastResult` / `lastError`。
- 分頁迴圈：因回應無 `TotalPage`，改用「本頁筆數 < pageSize 即為最後一頁」判斷是否繼續呼叫下一頁。
- `upsertPage`：以 `CusNo` 轉大寫作為比對 key（避免 DB collation 大小寫不分導致誤判為新廠商而重複 INSERT，同商品同步的既有作法）；命中則更新 `name` / `sysDate` / `modifyDate` / `updatedAt`，未命中且 `isOnlyUpdate=false` 則新增。
- `SysDate` / `ModifyDate` 字串以 `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")` 解析為 `LocalDateTime`；空值或解析失敗時該欄位存 `null` 並記 warning log，不中斷整體同步。
- `getSyncStatus()` 回傳格式同商品同步：`running`, `lastStartedAt`, `lastFinishedAt`, `lastResult`(`totalFetched`/`totalSaved`/`totalPages`/`elapsedMs`), `lastError`。

## API 與權限

**同步觸發**（加進既有 `IntegrationController`，`/api/integrations` 底下）

| Method | Path | 權限 | 說明 |
|---|---|---|---|
| POST | `/erp/vendor-sync` | `ROLE_ADMIN` | 觸發同步；同步進行中時回 409 |
| GET | `/erp/vendor-sync/status` | `ROLE_ADMIN` | 查詢同步狀態 |

**查詢**（新增 `VendorController`，`/api/vendors`）

| Method | Path | 權限 | 說明 |
|---|---|---|---|
| GET | `/api/vendors?keyword=` | 登入即可 | 依廠商代號/名稱模糊查詢，回傳清單供下拉選用 |

`VendorController` → `VendorService.searchVendors(keyword)` → `VendorRepository`，三層標準結構。回傳 `VendorResponse`（`id`, `code`, `name`）。

## 前端

- `frontend/src/pages/erp/ErpVendorSyncPage.jsx`：比照 `ErpProductSyncPage.jsx` 結構——觸發按鈕、狀態輪詢（3 秒一次直到 `running=false`）、顯示 `lastResult`/`lastError`。
- `frontend/src/api/integrations.js` 新增 `syncErpVendors()`、`getErpVendorSyncStatus()`。
- `frontend/src/api/vendors.js`（新檔）新增 `getVendorList(keyword)`，給查詢下拉使用。
- `router.jsx` 新增路由、`Sidebar.jsx` 新增入口，與商品同步頁面並列。

## 錯誤處理

- 外部 API 呼叫失敗（非 401）：例外往上拋，`ErpVendorSyncService` 捕捉後記錄 `lastError`，前端顯示錯誤訊息，不中斷應用程式。
- 401：清除 token 快取重試一次；重試仍失敗則視為一般錯誤處理。
- 單筆日期解析失敗：不拋例外，該筆該欄位存 null，記 warning log，繼續處理其餘資料。
- 同步進行中重複觸發：Controller 回 409，不啟動第二個同步流程。

## 測試策略

- **後端單元測試** `ErpVendorSyncServiceTest`（Mockito，mock `ErpVendorSyncClient` + `VendorRepository`）：
  - 分頁迴圈在回傳筆數 < pageSize 時停止
  - `isOnlyUpdate=true` 時只更新既有廠商、不新增
  - 日期解析失敗時對應欄位為 null，且不拋例外中斷同步
  - `code` 大小寫視為同一筆（不重複新增）
- **Controller 整合測試**：
  - `IntegrationControllerIntegrationTest` 補上 vendor-sync 的 409（已在執行中）與 202（成功觸發）情境
  - 新增 `VendorControllerIntegrationTest`：驗證查詢結果、未登入回 401
- **前端測試**：`ErpVendorSyncPage.test.jsx` 比照 `ErpProductSyncPage` 既有測試模式，驗證觸發按鈕與狀態顯示。

## 範圍界定（本次不做）

- Request 過濾欄位（如依廠商名稱查詢 ERP）：待確認實際格式後另行擴充，不影響本次核心同步流程。
- 廠商資料與採購單的實際串接（如建立採購單時改用下拉選擇廠商）：僅提供查詢 API，實際串接留待後續任務。
- 排程自動同步：本次僅支援手動觸發，不加 cron 排程。
