# X008 Fix - Convert all visible UI text (T001~T043) to Traditional Chinese (zh-TW)

## Background
For tasks T001 through T043, the UI has been implemented and contains visible text in English (or mixed languages).
We need to standardize all user-facing UI copy to **Traditional Chinese (繁體中文 / zh-TW)**.

## Goal
Update all visible UI strings rendered by the frontend for features completed in T001~T043 to Traditional Chinese.

## Scope
Frontend only:
- Replace/translate UI labels, button text, page titles, sidebar menu items, form placeholders, validation messages, empty-state messages, dialogs, toast/snackbar messages, table headers, and any visible static strings.
- Do not change backend APIs, DB, auth logic, or routing behavior.

## Requirements
1) Convert all visible UI text for features implemented in T001~T043 to Traditional Chinese (zh-TW).
2) Keep functionality unchanged (same DOM structure as much as possible, do not break selectors).
3) Prefer a centralized approach:
   - If the project already uses i18n (e.g., i18next), add/update a `zh-TW` resource file and switch default language to `zh-TW`.
   - If no i18n exists, a minimal approach is acceptable:
     - Create a single `src/i18n/zh-TW.ts` dictionary and replace hardcoded strings with constants.
4) Do not translate:
   - API paths, URLs, code identifiers, JSON keys, DB fields, environment variable names.
5) Keep technical terms consistent:
   - Login: 登入
   - Logout: 登出
   - Username/Email: 帳號 / Email（依現有 UI 欄位語意）
   - Password: 密碼
   - Submit: 送出
   - Cancel: 取消
   - Save: 儲存
   - Delete: 刪除
   - Edit: 編輯
   - Search: 搜尋
   - Loading: 載入中
   - Error: 錯誤
6) Sidebar & common layout text must be updated:
   - Dashboard -> 儀表板
   - Users -> 使用者
   - Forecast Config -> 預測設定
   - Forecast Upload -> 預測上傳
   (If additional menu items exist from T001~T043, translate them as well.)

## Expected Files to Change
- Frontend components/pages that render visible UI text
- (Optional) A centralized dictionary/resource file (recommended)

## Notes
- Keep translations natural for Taiwan users.
- Avoid Mainland China wording (use 繁體中文).
