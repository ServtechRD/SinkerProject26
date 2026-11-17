# F129 銷售預估截止鎖定與開放補登（R4）

設計文件：`docs/superpowers/specs/2026-11-16-r4-cutoff-reopen-design.md`
需求來源：成偉銷售預估平台第一階段調整，R4（PDF 第 17–21 頁）。

## 目標
截止時間一到自動鎖定，業務與生管看到同一份數字。截止後只有兩條路：
1. 生管直接修正：修改原因必填，產生新的生管表單版本，表單維持鎖定。
2. 開放補登：只對指定通路開放，有到期時間，到期自動鎖回並把補登資料併成新的生管版本。

整張表單結束新增後不可還原。所有鎖定、開放、修正動作都留異動紀錄。

## 範圍
- 截止改為「日期＋時間」（`autoCloseAt`，台灣時間）；排程每分鐘執行自動截止與補登到期處理。
- 業務上傳／編輯前統一檢查（`assertBusinessEditable`）：未結束、或該通路補登中才可寫入，其餘 403。
- 開放補登：可複選通路、期限最多 3 個工作天（排除週六日）、原因必填；可提前結束單一通路。
- 補登結束時，多通路合併成一個新的生管版本（`form_vN+1`），上傳不再洗掉 `form_v%` 資料。
- 生管儲存新版本必填修改原因，寫入「生管直接修正」紀錄。
- 新增權限 35 `sales_forecast_config.reopen`、表 `sales_forecast_reopen_window`、`sales_forecast_form_log`（Flyway V35、V36）。
- 新端點：`POST /config/{id}/reopen`、`POST /config/{id}/reopen/{windowId}/end`、`GET /config/{id}/detail`、`GET /config/{id}/logs`、`GET /config/edit-status`。
- 前端：表單明細頁、開放補登對話框、業務上傳頁紅／橘提示列與生管最新版數量、生管表單修改原因必填。

## 不在範圍
Email 通知、國定假日、檔期截止（R2-A）、通路主檔（R3）。
