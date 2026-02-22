# Acceptance Criteria - X008

## UI Text Language
1) All visible UI text for screens/features implemented in T001~T043 is shown in Traditional Chinese (zh-TW).
   This includes:
   - Page titles / headings
   - Sidebar items / navigation labels
   - Buttons
   - Form labels and placeholders
   - Validation / error messages shown to user
   - Dialog titles and contents
   - Toast/snackbar messages
   - Table headers / empty-state texts

2) No new English text is introduced for those areas, except:
   - Proper nouns (product/project name)
   - Technical literals that must remain unchanged (URLs, API paths, codes)

## Functional Non-Regression
3) App can still be used normally:
   - Login flow still works
   - Navigation still works
   - Existing forms submit as before
   - Existing pages render without runtime errors

## Build & Tests
4) Frontend build passes:
   - `cd frontend && npm ci`
   - `cd frontend && npm run build`

5) Existing tests still pass:
   - `cd frontend && npm test` (or the repo’s test command)
   - E2E tests (if present) still pass without changing test selectors.
     - If tests assert exact English strings, update tests to assert zh-TW or use stable selectors.

## Manual Verification Checklist
- Open the app in browser and verify key screens:
  - Login page
  - Dashboard/home
  - Any pages introduced in T001~T043
- Confirm the user sees zh-TW labels across the UI.
