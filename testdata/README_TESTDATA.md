# 測試數據目錄說明

本目錄包含自動生成的 Excel 測試檔案，用於測試 **預測上傳 (Sales Forecast Upload)** 和 **週生產排程上傳 (Weekly Schedule Upload)** 功能。

## 📋 檔案列表

### 預測上傳 (Sales Forecast Upload)

| 檔案名稱 | 測試情境 | 預期結果 | 使用方式 |
|---------|---------|---------|---------|
| `upload_forecast_ok.xlsx` | 正常上傳 (10 行) | ✅ 成功上傳 | 驗證基本上傳功能 |
| `upload_forecast_missing_required.xlsx` | 缺失必填欄位 | ❌ 驗證錯誤 | 測試品號、箱數小計必填驗證 |
| `upload_forecast_bad_types.xlsx` | 錯誤資料型別 | ❌ 驗證錯誤 | 測試負數、零、文字數量驗證 |
| `upload_forecast_duplicates.xlsx` | 重複產品代碼 | ⚠️  重複覆蓋 | 測試重複資料處理 |
| `upload_forecast_large_5k.xlsx` | 效能測試 (5000 行) | ✅ 成功上傳 | 測試大檔案處理效能 |

### 週生產排程上傳 (Weekly Schedule Upload)

| 檔案名稱 | 測試情境 | 預期結果 | 使用方式 |
|---------|---------|---------|---------|
| `upload_schedule_ok.xlsx` | 正常上傳 (20 行) | ✅ 成功上傳 | 驗證基本上傳功能 |
| `upload_schedule_missing_required.xlsx` | 缺失必填欄位 | ❌ 驗證錯誤 | 測試所有欄位必填驗證 |
| `upload_schedule_bad_types.xlsx` | 錯誤資料型別 | ❌ 驗證錯誤 | 測試無效日期、負數量驗證 |
| `upload_schedule_duplicates.xlsx` | 重複產品+日期組合 | ⚠️  重複覆蓋 | 測試重複資料處理 |
| `upload_schedule_large_5k.xlsx` | 效能測試 (5000 行) | ✅ 成功上傳 | 測試大檔案處理效能 |

## 🚀 如何重新生成測試檔案

### 方法 1: 使用 Makefile (推薦)

```bash
# 在專案根目錄執行
make gen-test-excels
```

### 方法 2: 使用 Shell 腳本

```bash
# 在專案根目錄執行
./scripts/gen-test-excels.sh
```

### 方法 3: 使用 Docker 直接執行

```bash
docker run --rm \
  -v $(pwd)/scripts:/scripts \
  -v $(pwd)/testdata:/output \
  python:3.11-slim \
  bash -c "pip install -q openpyxl && python /scripts/generate_upload_excels.py"
```

## 🧪 如何使用測試檔案進行 API 測試

### 前置準備

1. 啟動開發環境：
```bash
make dev-up
```

2. 取得 JWT Token：
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r .token)
```

### 預測上傳測試

#### 測試正常上傳
```bash
curl -X POST http://localhost:8080/api/sales-forecast/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_forecast_ok.xlsx" \
  -F "month=202602" \
  -F "channel=家樂福"
```

**預期結果**: HTTP 200, 成功上傳 10 筆資料

#### 測試缺失必填欄位
```bash
curl -X POST http://localhost:8080/api/sales-forecast/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_forecast_missing_required.xlsx" \
  -F "month=202602" \
  -F "channel=家樂福"
```

**預期結果**: HTTP 400, 錯誤訊息包含 "品號 (product_code) is required" 或 "箱數小計 (quantity) is required"

#### 測試錯誤資料型別
```bash
curl -X POST http://localhost:8080/api/sales-forecast/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_forecast_bad_types.xlsx" \
  -F "month=202602" \
  -F "channel=家樂福"
```

**預期結果**: HTTP 400, 錯誤訊息包含 "箱數小計 (quantity) must be positive"

#### 測試效能 (大檔案)
```bash
time curl -X POST http://localhost:8080/api/sales-forecast/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_forecast_large_5k.xlsx" \
  -F "month=202602" \
  -F "channel=家樂福"
```

**預期結果**: HTTP 200, 成功上傳 5000 筆資料，執行時間 < 10 秒

### 週生產排程上傳測試

#### 測試正常上傳
```bash
curl -X POST http://localhost:8080/api/weekly-schedule/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_schedule_ok.xlsx" \
  -F "week_start=2026-03-02" \
  -F "factory=工廠A"
```

**預期結果**: HTTP 200, 成功上傳 20 筆資料

#### 測試缺失必填欄位
```bash
curl -X POST http://localhost:8080/api/weekly-schedule/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_schedule_missing_required.xlsx" \
  -F "week_start=2026-03-02" \
  -F "factory=工廠A"
```

**預期結果**: HTTP 400, 錯誤訊息包含 "is required"

#### 測試錯誤資料型別
```bash
curl -X POST http://localhost:8080/api/weekly-schedule/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_schedule_bad_types.xlsx" \
  -F "week_start=2026-03-02" \
  -F "factory=工廠A"
```

**預期結果**: HTTP 400, 錯誤訊息包含 "invalid format" 或 "must be >= 0"

#### 測試效能 (大檔案)
```bash
time curl -X POST http://localhost:8080/api/weekly-schedule/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@testdata/upload_schedule_large_5k.xlsx" \
  -F "week_start=2026-03-02" \
  -F "factory=工廠A"
```

**預期結果**: HTTP 200, 成功上傳 5000 筆資料，執行時間 < 10 秒

## 📝 注意事項

### 預測上傳特殊規則
- ✅ 檔案格式：僅接受 `.xlsx`
- ✅ 欄位順序：**固定順序**（中類名稱、貨品規格、品號、品名、庫位、箱數小計）
- ✅ 必填欄位：品號、箱數小計
- ✅ 數量限制：箱數小計必須 **> 0**（不可為 0）

### 週生產排程特殊規則
- ✅ 檔案格式：接受 `.xlsx` 或 `.xls`
- ✅ 欄位順序：**動態檢測**（透過中文 header 名稱，順序不固定）
- ✅ 必填欄位：所有欄位均必填（需求日期、品號、品名、庫位、箱數小計）
- ✅ 數量限制：箱數小計必須 **>= 0**（可以為 0）
- ✅ 日期限制：week_start 必須是星期一
- ✅ 日期格式：支援 Excel 日期格式或 YYYY-MM-DD 字串

### 資料覆蓋策略
- ⚠️  **預測上傳**：上傳時會刪除相同 `month` + `channel` 的舊資料
- ⚠️  **週排程上傳**：上傳時會刪除相同 `week_start` + `factory` 的舊資料

### 權限要求
- 🔒 預測上傳：需要 `MANAGE_SALES_FORECAST` 權限
- 🔒 週排程上傳：需要 `MANAGE_WEEKLY_SCHEDULE` 權限

## 🔍 測試檔案內容預覽

### upload_forecast_ok.xlsx
```
中類名稱 | 貨品規格      | 品號  | 品名     | 庫位 | 箱數小計
---------|--------------|-------|---------|------|--------
飲料類   | 330ml罐裝    | P001  | 可口可樂 | A區  | 125.50
零食類   | 500ml寶特瓶  | P002  | 雪碧     | B區  | 150.50
日用品   | 1500ml大瓶   | P003  | 七喜     | C區  | 175.50
... (共 10 行)
```

### upload_schedule_ok.xlsx
```
需求日期    | 品號  | 品名     | 庫位 | 箱數小計
-----------|-------|---------|------|--------
2026-03-02 | P001  | 可口可樂 | A區  | 95.75
2026-03-03 | P002  | 雪碧     | B區  | 110.75
2026-03-04 | P003  | 七喜     | C區  | 125.75
... (共 20 行，涵蓋一週)
```

## 📚 參考文件

- **欄位規格文件**: [docs/upload_excel_spec.md](../docs/upload_excel_spec.md)
- **生成器腳本**: [scripts/generate_upload_excels.py](../scripts/generate_upload_excels.py)
- **Backend Parser**:
  - 預測上傳: `backend/src/main/java/com/sinker/app/service/ExcelParserService.java`
  - 週排程上傳: `backend/src/main/java/com/sinker/app/service/WeeklyScheduleExcelParser.java`

## 🛠️ 疑難排解

### 問題 1: Docker 執行失敗
**錯誤**: `openpyxl is not installed`

**解決方式**: 確保在 Docker 命令中包含 `pip install openpyxl`

### 問題 2: 檔案權限問題
**錯誤**: `Permission denied`

**解決方式**: 確保 `testdata/` 目錄有寫入權限
```bash
chmod -R 755 testdata/
```

### 問題 3: API 測試返回 401
**錯誤**: `Unauthorized`

**解決方式**: 重新取得有效的 JWT token
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r .token)
```

### 問題 4: API 測試返回 403
**錯誤**: `Forbidden`

**解決方式**: 確保使用的帳號具有對應權限（admin 帳號擁有所有權限）

---

**最後更新**: 2026-02-25
