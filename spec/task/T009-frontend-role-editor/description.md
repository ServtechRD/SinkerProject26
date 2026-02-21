# T009: Frontend Role Permission Editor

## Context
This task implements the role management interface in React, integrating with the backend role and permission API (T006). It provides administrators with the ability to view roles and edit role permissions through a structured interface with module-grouped checkboxes.

## Goal
Create role management pages including a list view of roles and a permission editor that allows administrators to easily assign/remove permissions from roles using module-grouped checkboxes with module-level select-all functionality.

## Scope

### In Scope
- Role list page showing all roles
- Role detail/edit page with permission checkboxes
- Permissions grouped by module (user, role, sales_forecast, etc.)
- Module-level "Select All" checkbox for each module
- Individual permission checkboxes
- Save changes button
- Cancel button (discard changes)
- Loading states
- Success/error notifications
- System role protection (cannot edit code)

### Out of Scope
- Create new roles
- Delete roles
- Role activation/deactivation
- Role assignment to users (handled in user management T008)
- Permission CRUD (permissions are seeded, not managed)
- Role search/filter (small dataset, not needed)
- Audit log of permission changes

## Requirements
- List page shows all roles with columns: Code, Name, Description, System Role, Actions
- Edit button navigates to role edit page
- Edit page displays role info (code, name, description) at top
- Name and description are editable
- Code is read-only (displayed but not editable)
- Permissions displayed below role info
- Permissions grouped by module (10 modules)
- Each module has collapsible/expandable section (optional) or always visible
- Each module has "Select All" checkbox to toggle all permissions in that module
- Individual permission checkboxes show permission name and code
- Selecting/deselecting permissions updates state
- "Select All" checkbox indeterminate state if some (not all) permissions selected
- Save button sends updated permissions to API
- Cancel button returns to list without saving
- Loading spinner during save
- Success toast on successful save
- Error toast on failure
- Redirect to list after successful save (optional, or stay on page)

## Implementation Notes
- Use React Router for navigation
- Fetch roles on list page mount
- Fetch role detail with permissions on edit page mount
- Group permissions by module in state (use reduce or group-by logic)
- Use useState for permission selections (Set or array of IDs)
- Calculate "Select All" state: all selected → checked, none selected → unchecked, some selected → indeterminate
- Clicking module "Select All" toggles all permissions in that module
- Clicking individual permission updates selection
- Submit sends array of permission IDs to PUT /api/roles/:id
- Use controlled checkboxes
- Disable form during submission
- Show loading skeleton while loading role/permissions

## Files to Change
- src/pages/roles/RoleListPage.jsx (new)
- src/pages/roles/RoleEditPage.jsx (new)
- src/components/PermissionModuleGroup.jsx (new - optional, for better organization)
- src/api/roles.js (new)
- src/router.jsx (update to add routes)
- src/components/Sidebar.jsx (update if adding Roles menu item)

## Dependencies
- T006: Requires backend role and permission API
- T003: Requires base layout and routing
