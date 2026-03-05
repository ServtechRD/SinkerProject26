# Excel 上傳欄位規格文件

## 文件目的

本文件詳細描述 SinkerProject26 專案中兩個 Excel 上傳功能的欄位規格、驗證規則及錯誤處理方式。此規格基於實際的 backend parser 實作，確保測試數據與系統要求完全一致。

**最後更新日期**: 2026-02-25

---

## 1. 預測上傳 (Sales Forecast Upload)

### 1.1 API Endpoint

**URL**: `POST /api/sales-forecast/upload`

**請求參數**:
- `file` (MultipartFile, 必填) - Excel 檔案 (.xlsx 格式)
- `month` (String, 必填) - 預測月份，格式 YYYYMM (例: "202602")
- `channel` (String, 必填) - 銷售通路

**有效通路清單** (共 12 個):
- 家樂福
- 大潤發
- 愛買
- 全聯
- 頂好
- 美廉社
- 7-11
- 全家
- 萊爾富
- OK
- 電商
- 其他

### 1.2 Excel 欄位規格

| 順序 | 中文名稱 | 系統欄位名 | 資料型別 | 必填 | 長度限制 | 驗證規則 |
|------|---------|-----------|---------|------|---------|---------|
| 0    | 中類名稱 | category  | VARCHAR | ❌   | 100     | 可選欄位 |
| 1    | 貨品規格 | spec      | VARCHAR | ❌   | 200     | 可選欄位 |
| 2    | 品號     | product_code | VARCHAR | ✅ | 50      | 必填，不可空白 |
| 3    | 品名     | product_name | VARCHAR | ❌ | 200     | 可選欄位 |
| 4    | 庫位     | warehouse_location | VARCHAR | ❌ | 50 | 可選欄位 |
| 5    | 箱數小計 | quantity  | DECIMAL(10,2) | ✅ | -       | 必填，必須 > 0 |

**重要規則**:
- ✅ **固定欄位順序**: Excel 欄位必須按照上述 0-5 的順序排列
- ✅ **檔案格式**: 僅接受 `.xlsx` 格式
- ✅ **Header 行**: 必須有 header 行（第一行），至少包含 6 個欄位
- ✅ **數量驗證**: quantity 必須為正數（> 0），不可為 0 或負數
- ✅ **資料型別**: quantity 可為數值或字串（會自動轉換），其他欄位均為字串
- ✅ **空行處理**: 自動跳過空行（所有欄位均為空白）

### 1.3 範例數據行

**Header 行**:
```
中類名稱 | 貨品規格 | 品號 | 品名 | 庫位 | 箱數小計
```

**正常數據行**:
```
飲料類 | 330ml罐裝 | P001 | 可口可樂 | A區 | 100.50
零食類 | 150g袋裝 | P002 | 洋芋片 | B區 | 250.00
```

**最小必填數據行** (僅填必填欄位):
```
 |  | P003 |  |  | 50.75
```

### 1.4 錯誤情境與預期錯誤訊息

| 錯誤情境 | Excel 內容 | 預期錯誤訊息 |
|---------|-----------|-------------|
| 缺失品號 | 品號欄位為空 | `Row X: 品號 (product_code) is required` |
| 缺失數量 | 箱數小計欄位為空 | `Row X: 箱數小計 (quantity) is required` |
| 數量為零 | 箱數小計 = 0 | `Row X: 箱數小計 (quantity) must be positive` |
| 數量為負數 | 箱數小計 = -10 | `Row X: 箱數小計 (quantity) must be positive` |
| 數量為文字 | 箱數小計 = "abc" | `Row X: 箱數小計 (quantity) must be a valid number` |
| 檔案格式錯誤 | .xls, .csv 等 | `Invalid file format. Only .xlsx files are accepted` |
| 欄位不足 | 少於 6 個欄位 | `Excel file is missing required columns. Expected 6 columns: 中類名稱, 貨品規格, 品號, 品名, 庫位, 箱數小計` |
| 空檔案 | 僅有 header 無數據 | `Excel file has no data rows (only header or empty)` |

**實作參考**: `/backend/src/main/java/com/sinker/app/service/ExcelParserService.java` (lines 21-28, 123-168)

---

## 2. 週生產排程上傳 (Weekly Schedule Upload)

### 2.1 API Endpoint

**URL**: `POST /api/weekly-schedule/upload`

**請求參數**:
- `file` (MultipartFile, 必填) - Excel 檔案 (.xlsx 或 .xls 格式)
- `week_start` (String, 必填) - 週開始日期，格式 YYYY-MM-DD (例: "2026-03-02")，**必須是星期一**
- `factory` (String, 必填) - 工廠名稱

### 2.2 Excel 欄位規格

| 中文名稱 | 系統欄位名 | 資料型別 | 必填 | 長度限制 | 驗證規則 |
|---------|-----------|---------|------|---------|---------|
| 需求日期 | demand_date | DATE | ✅ | - | 必填，支援 Excel 日期或 YYYY-MM-DD 字串 |
| 品號 | product_code | VARCHAR | ✅ | 50 | 必填，不可空白 |
| 品名 | product_name | VARCHAR | ✅ | 200 | 必填，不可空白 |
| 庫位 | warehouse_location | VARCHAR | ✅ | 50 | 必填，不可空白 |
| 箱數小計 | quantity | DECIMAL(10,2) | ✅ | - | 必填，必須 >= 0 (可為 0) |

**重要規則**:
- ✅ **動態欄位順序**: 欄位順序不固定，系統透過中文 header 名稱檢測欄位位置
- ✅ **檔案格式**: 接受 `.xlsx` 或 `.xls` 格式
- ✅ **Header 行**: 必須有 header 行（第一行），包含所有必需的中文欄位名稱
- ✅ **所有欄位必填**: 與預測上傳不同，所有 5 個欄位均為必填
- ✅ **數量驗證**: quantity 必須 >= 0 (可以為 0，與預測上傳不同)
- ✅ **日期格式**: 支援 Excel 日期格式或 YYYY-MM-DD 字串
- ✅ **空行處理**: 自動跳過空行

### 2.3 範例數據行

**Header 行** (順序可變):
```
需求日期 | 品號 | 品名 | 庫位 | 箱數小計
```

**正常數據行**:
```
2026-03-02 | P001 | 可口可樂 | A區 | 100.50
2026-03-03 | P002 | 雪碧 | B區 | 0.00
```

**日期為 Excel 格式**:
```
45353 | P003 | 七喜 | C區 | 50.00
(Excel 會自動將日期儲存為數值格式)
```

### 2.4 錯誤情境與預期錯誤訊息

| 錯誤情境 | Excel 內容 | 預期錯誤訊息 |
|---------|-----------|-------------|
| 缺失需求日期 | 需求日期欄位為空 | `Row X: 需求日期 is required` |
| 缺失品號 | 品號欄位為空 | `Row X: 品號 is required` |
| 缺失品名 | 品名欄位為空 | `Row X: 品名 is required` |
| 缺失庫位 | 庫位欄位為空 | `Row X: 庫位 is required` |
| 缺失數量 | 箱數小計欄位為空 | `Row X: 箱數小計 is required` |
| 數量為負數 | 箱數小計 = -10 | `Row X: 箱數小計 must be >= 0` |
| 無效日期格式 | 需求日期 = "abc" | `Row X: 需求日期 invalid format` |
| 檔案格式錯誤 | .csv, .txt 等 | `Invalid file format. Only .xlsx and .xls files are accepted` |
| 缺失必需欄位 | header 缺少必需欄位 | `Excel file missing required columns: [欄位名稱]` |
| 空檔案 | 僅有 header 無數據 | `Excel file has no data rows (only header or empty)` |

**實作參考**: `/backend/src/main/java/com/sinker/app/service/WeeklyScheduleExcelParser.java` (lines 24-36, 174-252)

---

## 3. 對應關係總結

### 3.1 Entity 對應表

| 功能 | Entity 類別 | 資料表名稱 |
|------|-----------|-----------|
| 預測上傳 | `com.sinker.app.entity.SalesForecast` | `sales_forecast` |
| 週生產排程 | `com.sinker.app.entity.WeeklySchedule` | `production_weekly_schedule` |

### 3.2 Controller 對應表

| 功能 | Controller 類別 | Endpoint |
|------|----------------|----------|
| 預測上傳 | `com.sinker.app.controller.SalesForecastUploadController` | `POST /api/sales-forecast/upload` |
| 週生產排程 | `com.sinker.app.controller.WeeklyScheduleController` | `POST /api/weekly-schedule/upload` |

### 3.3 Parser 對應表

| 功能 | Parser 類別 | 欄位檢測方式 |
|------|------------|-------------|
| 預測上傳 | `com.sinker.app.service.ExcelParserService` | 固定順序 (COL_CATEGORY=0, ..., COL_QUANTITY=5) |
| 週生產排程 | `com.sinker.app.service.WeeklyScheduleExcelParser` | 動態檢測 (透過中文 header 名稱匹配) |

---

## 4. 測試建議

### 4.1 正常情境測試
- ✅ 上傳包含所有必填欄位的正常檔案
- ✅ 上傳僅填寫必填欄位的檔案（預測上傳適用）
- ✅ 上傳包含 0 數量的檔案（週排程適用）

### 4.2 驗證錯誤測試
- ❌ 缺失必填欄位
- ❌ 數量為負數或零（預測）/ 數量為負數（週排程）
- ❌ 無效的資料型別
- ❌ 檔案格式錯誤

### 4.3 效能測試
- 📊 上傳大檔案 (5000+ 行)
- 📊 上傳包含重複資料的檔案

### 4.4 邊界測試
- 🔍 超過欄位長度限制的字串
- 🔍 極大或極小的數值
- 🔍 特殊字元處理

---

## 5. 更新歷史

| 日期 | 版本 | 變更內容 | 作者 |
|------|------|---------|------|
| 2026-02-25 | 1.0 | 初始版本，完整規格文件 | Claude |

---

**注意**: 本文件基於實際 backend 程式碼生成，請在修改 parser 邏輯後同步更新此文件。
