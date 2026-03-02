目標：
我們專案已經有 tasks 及完成 backend/frontend。
請你掃描 repo，找到「預測上傳 (forecast upload)」與「週生產排程上傳 (weekly schedule upload)」的實作，依據實際上傳欄位與格式，生成測試用 Excel 檔案與產生器腳本。

掃描範圍（務必找出真實規格來源）：
1) Backend: controller/endpoint、request DTO/Pydantic schema、validation rules、OpenAPI spec
2) Frontend: upload UI/欄位 mapping、欄位名稱與必填、格式提示
3) task docs: 對應的 tasks/*/description.md、acceptance、testplan（找出欄位/格式/範例）

請輸出：
A) 上傳欄位規格文件：docs/upload_excel_spec.md
   - ForecastUpload 欄位清單（順序、型別、必填、允許值、日期格式、唯一鍵規則）
   - WeeklyScheduleUpload 欄位清單（同上）
   - 任何版本欄位/覆蓋策略（若現有實作有）
   - 與 backend DTO / API endpoint 對應關係（哪個 endpoint 吃哪個 sheet）

B) Excel 生成器腳本：
   - 新增 scripts/generate_upload_excels.py（Python + openpyxl）
   - 依據掃描出的欄位與規則自動生成：
     ./testdata/upload_ok.xlsx
     ./testdata/upload_missing_required.xlsx
     ./testdata/upload_bad_types.xlsx
     ./testdata/upload_duplicates.xlsx
     ./testdata/upload_large_5k.xlsx
   - 每個檔案包含兩個 sheets：ForecastUpload、WeeklyScheduleUpload（sheet 名稱以現有上傳程式實作為準）
   - 第一列凍結、header 加粗、欄寬合理
   - 需要的 id 用 UUID、日期/時間符合既有驗證

C) 測試說明：
   - 新增 testdata/README_TESTDATA.md，說明每份 Excel 測什麼情境
   - 若專案已有匯入 API 的 integration test，請新增一個測試至少驗證 upload_ok.xlsx 可成功匯入（或 mock 方式也可），遵循既有測試策略。

執行方式（Docker-first）：
- 不允許要求在 host 裝 python（除非專案已有 python tooling）。
- 若 repo 沒有 python 環境，請用 docker 方式提供可重現執行方法：
  1) 新增一個輕量 docker compose service 或一次性 docker run 命令來跑 scripts/generate_upload_excels.py
  2) 或者提供 Makefile target：make gen-test-excels

最後請在 PR 描述中列出：
- 你找到的 endpoint / DTO / 欄位來源
- 生成器如何對齊上傳規則
- 如何執行與如何驗證