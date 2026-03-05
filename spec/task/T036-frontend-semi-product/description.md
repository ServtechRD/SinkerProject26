# T036: Frontend - Semi Product Management Page

## Context
This task implements the frontend interface for semi-product advance purchase configuration management. Users need a web page to upload Excel files with drag-and-drop support, download templates, view all semi-product configurations in a table, and edit advance days inline.

## Goal
Create a React-based frontend page at /semi-product with Excel upload (drag-and-drop), template download, data table display, and inline editing for advance_days field.

## Scope

### In Scope
- React page component at /semi-product route
- Excel file upload with drag-and-drop zone
- File upload progress indicator
- Template download button
- Data table showing all semi-products
- Inline edit for advance_days column
- Client-side validation for positive integers
- Error handling and user feedback
- Integration with T035 backend APIs
- Material UI v5 components

### Out of Scope
- Advanced Excel preview before upload
- Bulk edit functionality
- Filtering or searching (basic table only)
- Export functionality
- Permission management UI (assumes permissions are set)

## Requirements
- **Route**: /semi-product
- **Components**:
  - SemiProductPage.jsx (main page component)
  - File upload zone with drag-and-drop
  - Data table with columns: Product Code, Product Name, Advance Days, Actions
  - Inline edit for Advance Days
- **API Integration**:
  - Upload: POST /api/semi-product/upload
  - List: GET /api/semi-product
  - Update: PUT /api/semi-product/:id
  - Template: GET /api/semi-product/template
- **Validation**:
  - File type: .xlsx only
  - Advance days: positive integer
- **UI/UX**:
  - Upload area shows file name when selected
  - Progress indicator during upload
  - Success/error toast notifications
  - Confirmation prompt before upload (data will be replaced)
  - Disable edit during save operations
  - Responsive design

## Implementation Notes
- Use Material UI DataGrid or Table component
- Implement drag-and-drop with react-dropzone or native HTML5
- Use axios for API calls in src/api/semiProduct.js
- Toast notifications using notistack or Material UI Snackbar
- Inline edit: Click advance_days cell → input field → save on blur/enter
- Template download triggers browser download
- Show row count after successful upload
- Handle permission errors (403) with appropriate messages
- Use React hooks (useState, useEffect) for state management
- Implement loading states for all async operations

## Files to Change
- Create: `src/pages/semiProduct/SemiProductPage.jsx`
- Create: `src/api/semiProduct.js`
- Update: `src/App.jsx` or routing configuration to add /semi-product route

## Dependencies
- T035: Backend API must be implemented
- T003: Authentication and base layout infrastructure
