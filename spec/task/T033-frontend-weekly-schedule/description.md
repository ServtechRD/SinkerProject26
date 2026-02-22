# T033 — Frontend: Weekly Production Schedule Page

## 1. Context
Module 6 requires a frontend page for production planners to manage weekly production schedules. Planners upload Excel files or manually edit schedule entries. Each upload/edit triggers PDCA material calculation on the backend.

## 2. Goal
Build the `/weekly-schedule` page with:
- Week picker (Monday-only selection)
- Factory dropdown selector
- Excel upload with drag-and-drop and template download
- Data table showing schedule entries with inline editing
- Auto-trigger of PDCA calculation on save (handled by backend)

## 3. Scope

### In Scope
- Weekly schedule page at `/weekly-schedule`
- Week picker component constrained to Mondays
- Factory dropdown selector
- Drag-and-drop Excel upload area
- Template download button
- Data table with columns: demand_date, product_code, product_name, warehouse_location, quantity
- Inline edit for demand_date and quantity fields
- Save triggers backend API which runs PDCA
- Add to sidebar navigation

### Out of Scope
- PDCA integration logic (backend T032)
- Material demand display (T039)
- Factory master data management

## 4. Requirements
- Week picker must only allow selecting Mondays
- Factory dropdown values can be hardcoded initially (or fetched from config API if available)
- Excel upload: accept only .xlsx files
- Template download per factory
- Data table: sortable by demand_date, product_code
- Inline edit: click cell to edit demand_date or quantity
- Save button sends PUT to backend
- Loading states during upload and save
- Error display for upload validation failures (show which rows failed)
- Permission guard: only users with weekly_schedule.view/upload/edit permissions

## 5. Implementation Notes
- Use MUI DatePicker configured to disable non-Monday dates
- Use existing FileDropzone component pattern (from T018)
- Data table with MUI DataGrid or custom table with edit mode
- Upload API returns success/error summary — display in alert/snackbar
- After successful upload, refresh the data table

## 6. Files to Change
- `frontend/src/pages/schedule/WeeklySchedulePage.jsx` (new)
- `frontend/src/api/weeklySchedule.js` (new)
- `frontend/src/components/WeekPicker.jsx` (new, reusable)
- `frontend/src/router.jsx` (add route)
- `frontend/src/components/Sidebar.jsx` (add nav item)

## 7. Dependencies
- T031 (Backend weekly schedule API)
- T003 (Base layout — sidebar, router, auth guard)
