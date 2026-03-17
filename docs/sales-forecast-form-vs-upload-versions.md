# 銷售預估量表單 vs 銷售預估量上傳-共同編輯界面：版本關聯說明

## 一、兩套版本機制概覽

| 項目 | 銷售預估量表單 | 銷售預估量上傳-共同編輯界面 |
|------|----------------|-----------------------------|
| **頁面** | ForecastListPage（總體表單） | ForecastUploadPage（依通路編輯） |
| **API 版本列表** | `GET /api/sales-forecast/form-versions?month=` | `GET /api/sales-forecast/versions?month=&channel=` |
| **版本識別** | `form_version_no`（整數 1, 2, 3…）＋ 表 `sales_forecast_form_version` | `version`（字串，例：`20260115120000(家樂福)`） |
| **資料來源** | `sales_forecast` 的 `form_version_no` ＋ `version` 欄位（`form_v2` 等） | `sales_forecast` 的 `month` + `channel` + `version` |
| **查詢方式** | 依月份＋表單版本號查「整月、全部通路」 | 依月份＋通路＋版本字串查「單一通路」 |

兩邊都使用同一張表 `sales_forecast`，但用不同欄位與 API 來表示「版本」。

---

## 二、資料表與欄位

- **sales_forecast**
  - `month`, `channel`, `version`（字串）, `form_version_no`（整數, 可 NULL）
  - 一筆資料 = 一個月份、一個通路、一個品項、一個「版本」（由 `version` 或 `form_version_no` 決定所屬版本）

- **sales_forecast_form_version**
  - 僅給「銷售預估量表單」用：`month` + `version_no`（1, 2, 3…）+ `created_at`, `change_reason`
  - 表單的版本選單與修改原因來自這裡，不與上傳的 version 字串混用。

---

## 三、關聯關係

### 1. 表單版本 1（form_version_no = 1）

- **產生時機**：月份「關帳」後，有人第一次在「銷售預估量表單」查詢或需要表單版本時。
- **後端邏輯**：`FormSummaryService.ensureFormVersion1Exists(month, config)`
  - 若該月已有任一 form version，則不做任何事。
  - 否則建立一筆 `sales_forecast_form_version`（version_no = 1）。
  - 對 **CHANNEL_ORDER** 的每個通路：
    - 查該月、該通路在 `sales_forecast` 的 **最新一筆 version（字串）**（即上傳界面產生的版本）。
    - 對該批資料做 **UPDATE**：`form_version_no = 1`（不複製、不新增筆數）。
- **意義**：  
  **表單的「版本 1」= 關帳當下，各通路在「上傳／共同編輯」裡的最新版資料，只做標記成 form 版本 1。**

因此：  
- 表單版本 1 的內容，就是當時「上傳界面」各通路最新版的彙總。  
- 上傳界面之後再改同一月份、同一通路，只會產生新的 version 字串，**不會**自動更新表單的版本 1。

### 2. 表單版本 2、3、…（form_version_no ≥ 2）

- **產生時機**：使用者在「銷售預估量表單」按「儲存」並輸入修改原因時。
- **後端邏輯**：`FormSummaryService.saveFormSummaryVersion(month, request)`
  - 先確保該月已有 form 版本 1（同上）。
  - 在 `sales_forecast_form_version` 新增一筆 version_no = 2（或 3、4…）。
  - 依請求內容 **INSERT 新的一批** `sales_forecast`：
    - `version = "form_v2"`（或 form_v3…）
    - `form_version_no = 2`（或 3…）
    - 每個通路、每個品項各一筆。
- **意義**：  
  表單版本 2 以後 = **只在表單流程產生的新版本**，與上傳界面「再編輯、再上傳」無直接連動；上傳界面不會自動產生 form_v2。

### 3. 上傳界面看到的版本列表

- **API**：`GET /api/sales-forecast/versions?month=&channel=`
- **後端**：`findDistinctVersionsByMonthAndChannel(month, channel)`  
  → 同一月份、同一通路下，`sales_forecast.version` 的**所有相異值**（字串，DESC）。
- **結果**：
  - 上傳／共同編輯產生的 version（例：`20260115120000(家樂福)`）會出現。
  - 若該月份、該通路曾因「表單儲存」而寫入過 `form_v2`、`form_v3` 等，**這些也會出現在上傳的版本下拉選單**（因為同一表、同一 channel）。
- **影響**：  
  上傳界面選「最新版本」時，有可能選到的是表單產生的 `form_v2`，而不是上傳自己產生的時間戳版本，需依產品需求決定是否要在列表裡過濾掉 `form_v*`。

---

## 四、流程簡圖

```
[上傳-共同編輯]
  月份 + 通路 + 上傳/編輯
    → version = "20260115120000(家樂福)" 等（字串）
    → 僅寫入 sales_forecast（version 字串，form_version_no 為 NULL）

[月份關帳]

[表單-第一次要版本時]
  ensureFormVersion1Exists
    → 各通路「當時最新」的 version 字串對應的資料
    → UPDATE form_version_no = 1（不新增資料）
    → 表單「版本 1」= 關帳當下上傳各通路最新版的快照

[表單-使用者編輯後儲存]
  saveFormSummaryVersion
    → INSERT 新資料 version = "form_v2", form_version_no = 2
    → 表單「版本 2」以後與上傳界面無自動同步
```

---

## 五、結論與注意點

1. **表單版本 1** 與 **上傳／共同編輯** 有直接關聯：  
   表單版本 1 = 關帳時，各通路在「上傳」裡的最新版，只做標記（form_version_no=1），不複製。
2. **表單版本 2 以上** 與上傳**沒有**自動關聯：  
   僅在表單內編輯、儲存時產生，上傳界面不會自動出現對應的「表單版次」。
3. **上傳的版本列表** 會包含同月、同通路在 `sales_forecast` 的所有 version，因此也會包含表單產生的 `form_v2`、`form_v3` 等；若希望上傳只看到「上傳產生的版本」，需在後端或前端過濾掉 `version` 以 `form_v` 開頭者。
4. 兩邊都讀寫同一張 **sales_forecast**，差別在：
   - 表單：以 `form_version_no` ＋ `sales_forecast_form_version` 為準。
   - 上傳：以 `month` + `channel` + `version`（字串）為準。
