# T018: Frontend - Forecast Upload Page

## Context
React 18 + Material UI v5 application with JWT authentication. This task creates the frontend interface for uploading Excel files containing sales forecast data.

## Goal
Implement a frontend page at /sales-forecast/upload that allows users to upload Excel files for their designated channels, with validation, progress feedback, and template download functionality.

## Scope

### In Scope
- Route /sales-forecast/upload in React Router
- Month dropdown showing only open months (is_closed=FALSE)
- Channel dropdown filtered by user's authorized channels
- Drag-and-drop file upload zone with file browser fallback
- Excel template download button
- Upload progress indicator
- Success/error feedback
- File validation (type, size)
- Integration with backend upload API

### Out of Scope
- Excel file preview before upload
- Batch upload (multiple files)
- Upload history or archive viewing
- Data editing within upload page
- Automatic retry on failure

## Requirements
- Fetch open months from /api/sales-forecast/config (filter is_closed=FALSE)
- Fetch user's authorized channels from authentication context or API
- Month selector: dropdown with format "YYYYMM (Month Name)" e.g., "202601 (January 2026)"
- Channel selector: dropdown with 12 channel names (filtered by user permissions)
- File upload zone:
  - Drag-and-drop area with visual feedback on hover
  - Click to browse file alternative
  - Accept only .xlsx files
  - Display selected file name and size
  - Clear/remove file button
- File size limit: 10MB (client-side validation)
- "Download Template" button calls GET /api/sales-forecast/template/:channel
- "Upload" button:
  - Disabled until month, channel, and file selected
  - Shows loading spinner during upload
  - Calls POST /api/sales-forecast/upload
- Success notification: "Successfully uploaded N rows"
- Error notification: Display error messages from backend
- Permission check: require sales_forecast.upload permission
- Responsive layout for mobile devices

## Implementation Notes
- Use MUI Select for month and channel dropdowns
- Use custom FileDropzone component or react-dropzone library
- Use MUI LinearProgress or CircularProgress for upload indicator
- API service: src/api/forecast.js with uploadForecast() and downloadTemplate() functions
- Use FormData for multipart file upload
- Handle file validation: check extension (.xlsx) and MIME type
- Display file size in human-readable format (KB, MB)
- Template download: create blob and trigger browser download
- Clear form after successful upload
- Use MUI Snackbar or Alert for notifications
- Show validation errors inline below each field
- Store selected month/channel in component state

## Files to Change
- `src/pages/forecast/ForecastUploadPage.jsx` (new)
- `src/components/forecast/FileDropzone.jsx` (new)
- `src/api/forecast.js` (new or update from T016)
- `src/routes/index.jsx` (update - add route)
- `src/App.jsx` (update if needed)

## Dependencies
- T015: Backend upload API must be implemented
- T013: Forecast config API for fetching open months
- T003: Frontend authentication and permission system
- Material UI v5
- React Router
- Axios for HTTP requests
