# T018: Acceptance Criteria

## Functional Acceptance Criteria

### Page Access
- [ ] Route /sales-forecast/upload accessible after authentication
- [ ] User without sales_forecast.upload permission redirected or sees access denied
- [ ] Page displays loading state while fetching months and channels

### Month Selection
- [ ] Month dropdown populated with open months only (is_closed=FALSE)
- [ ] Months displayed in format "YYYYMM (Month Name)" e.g., "202601 (January 2026)"
- [ ] Months sorted descending (newest first)
- [ ] If no open months, show message: "No open months available"
- [ ] Selected month stored in state

### Channel Selection
- [ ] Channel dropdown populated with user's authorized channels
- [ ] All 12 channels shown if user has global upload permission
- [ ] Only owned channels shown if user has channel-specific permission
- [ ] Channels sorted alphabetically
- [ ] Selected channel stored in state

### File Upload
- [ ] Drag-and-drop zone displays with dashed border and icon
- [ ] Hover over zone shows highlighted state
- [ ] Drop file on zone captures file
- [ ] Click zone opens file browser
- [ ] Only .xlsx files accepted (others rejected with error message)
- [ ] File size limited to 10MB (larger files rejected)
- [ ] Selected file name and size displayed
- [ ] Clear/remove file button removes selection
- [ ] Validation error shown below zone if invalid file

### Template Download
- [ ] "Download Template" button visible
- [ ] Button disabled if no channel selected
- [ ] Click downloads template for selected channel
- [ ] Downloaded file named "sales_forecast_template_{channel}.xlsx"
- [ ] Template download works without uploading

### Upload Process
- [ ] "Upload" button disabled until all fields (month, channel, file) filled
- [ ] Click "Upload" starts upload process
- [ ] Loading spinner shown during upload
- [ ] Upload button disabled during upload
- [ ] Form inputs disabled during upload

### Success Handling
- [ ] Successful upload shows success notification: "Successfully uploaded N rows"
- [ ] Form cleared after success (month, channel, file reset)
- [ ] Upload button re-enabled
- [ ] Success message auto-dismisses after 5 seconds

### Error Handling
- [ ] Month closed error shows: "Month {month} is closed for uploads"
- [ ] Permission error shows: "You don't have permission to upload to channel {channel}"
- [ ] Invalid file format shows: "Please upload a valid Excel file (.xlsx)"
- [ ] File too large shows: "File size exceeds 10MB limit"
- [ ] Backend validation errors displayed with details
- [ ] Network errors show: "Upload failed. Please try again."
- [ ] Form remains filled on error (allow retry without re-selecting)

## UI Acceptance Criteria

### Layout
- [ ] Page title: "Upload Sales Forecast"
- [ ] Form sections clearly labeled: Month, Channel, File
- [ ] Template download button prominently placed
- [ ] Upload button at bottom of form
- [ ] Responsive layout: stacks vertically on mobile

### File Dropzone
- [ ] Dashed border with background color
- [ ] Upload icon (cloud upload or file icon)
- [ ] Instruction text: "Drag and drop Excel file here, or click to browse"
- [ ] Accepted format hint: "Only .xlsx files up to 10MB"
- [ ] Active/hover state shows different background color
- [ ] Selected file shows with file icon, name, and size
- [ ] Remove button (X icon) to clear file

### Styling
- [ ] Consistent Material UI theme
- [ ] Primary button for Upload action
- [ ] Outlined button for Download Template
- [ ] Proper spacing between form fields (MUI Grid or Stack)
- [ ] Error messages in red text
- [ ] Success notifications in green

### Progress Indicator
- [ ] Circular or linear progress bar during upload
- [ ] Indeterminate progress (no percentage)
- [ ] Centered or inline with upload button

## Non-Functional Criteria
- [ ] Page loads within 2 seconds
- [ ] File selection responsive (no lag)
- [ ] Upload progress visible for files taking >1 second
- [ ] No console errors or warnings
- [ ] Accessible: keyboard navigation works
- [ ] Screen reader announces errors and success

## How to Verify

### Manual Testing
1. **Access Control:**
   - Login without sales_forecast.upload permission
   - Navigate to /sales-forecast/upload
   - Verify access denied or redirect

2. **Month Selection:**
   - Login with permission
   - Open page
   - Verify month dropdown shows only open months
   - Verify format "202601 (January 2026)"

3. **Channel Selection:**
   - Verify dropdown shows user's authorized channels
   - Select a channel

4. **Template Download:**
   - Select channel "大全聯"
   - Click "Download Template"
   - Verify file downloads with name "sales_forecast_template_大全聯.xlsx"
   - Open file and verify headers

5. **File Upload - Drag and Drop:**
   - Prepare valid .xlsx file
   - Drag file over dropzone
   - Verify hover effect
   - Drop file
   - Verify file name and size displayed

6. **File Upload - Browse:**
   - Click dropzone
   - Verify file browser opens
   - Select .xlsx file
   - Verify file captured

7. **Validation - File Type:**
   - Try to upload .csv file
   - Verify error: "Please upload a valid Excel file"

8. **Validation - File Size:**
   - Try to upload 15MB .xlsx file
   - Verify error: "File size exceeds 10MB limit"

9. **Upload Success:**
   - Select open month, owned channel, valid file
   - Click "Upload"
   - Verify loading spinner
   - Verify success notification with row count
   - Verify form cleared

10. **Upload Error - Closed Month:**
    - Close a month via config page
    - Select that month
    - Attempt upload
    - Verify error message about closed month

11. **Upload Error - Wrong Channel:**
    - Login as user for channel A
    - Manually call API with channel B
    - Verify permission error (or prevent in UI)

12. **Remove File:**
    - Select file
    - Click remove/clear button
    - Verify file removed from state
    - Verify Upload button disabled

13. **Responsive Design:**
    - Resize browser to mobile width
    - Verify layout stacks vertically
    - Verify all elements accessible

### Automated Testing
- Component render tests
- Form validation tests
- API integration tests with mock
- File selection and upload flow tests
