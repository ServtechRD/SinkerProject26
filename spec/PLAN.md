# PLAN.md — Sinker Project 26 Implementation Plan

## Overview
A collaboration platform for Sales, Production Planning, and Procurement teams. Integrates sales forecasting, production planning, and purchasing workflows with standardized data formats and automation. Connects to external systems (Tien-Hsin ERP, PDCA).

## Tech Stack
- **Frontend:** React 18 + Vite + Material UI v5
- **Backend:** Java 17 + Spring Boot 3.2.12
- **Database:** MariaDB 10.11
- **Auth:** JWT (24h expiry)
- **Testing:** JUnit 5 + Spring Boot Test (backend), Vitest + React Testing Library (frontend), Playwright (E2E)
- **Deployment:** Docker + docker-compose
- **Code coverage target:** 70%

## Modules (from spec)
- Module 0: User & Permission Management (login, users, roles, permissions)
- Module 1: Sales Forecast Config (month open/close settings)
- Module 2: Sales Forecast Forms (Excel upload, CRUD, versioning)
- Module 3: Sales Forecast Integration (12-channel aggregation, Excel export)
- Module 4: Inventory Integration (ERP inventory + sales + forecast integration)
- Module 5: Production Plan (annual plan by channel/month)
- Module 6: Weekly Production Schedule (Excel upload, PDCA integration)
- Module 7: Semi-Product Settings (advance purchase days)
- Module 8: Material Demand (PDCA-calculated material requirements)
- Module 9: Material Purchase (purchase confirmation, ERP trigger)

## Roles
| Role | Capabilities |
|------|-------------|
| Sales (業務) | Fill/edit sales forecasts (during open period), view own channels, export Excel |
| Production Planner (生管) | Set forecast months, modify all channel versions, view diffs, edit production forms |
| Procurement (採購) | View material demand, confirm purchases, trigger ERP purchase orders |
| Admin (管理者) | Manage all users, roles, system settings |

## Sales Channels (12 total)
PX/大全聯, 家樂福, 愛買, 711, 全家, OK/萊爾富, 好市多, 楓康, 美聯社, 康是美, 電商, 市面經銷

---

## Phase 0 — Base App Shell

| Task | Title | Dependencies |
|------|-------|-------------|
| T001 | DB migration: users, roles, permissions, role_permissions, seed data | — |
| T002 | Backend auth: login endpoint + JWT filter | T001 |
| T003 | Frontend base layout shell (sidebar, router, login page) | T002 |
| T004 | E2E: login flow + protected routes | T002, T003 |

## Phase 1 — Module 0: User & Permission Management

| Task | Title | Dependencies |
|------|-------|-------------|
| T005 | Backend: User CRUD API (list, create, edit, toggle, delete) | T002 |
| T006 | Backend: Role-permission management API | T005 |
| T007 | DB migration: sales_channels_users, login_logs | T001 |
| T008 | Frontend: User management pages (list, create, edit) | T005, T003 |
| T009 | Frontend: Role permission editor page | T006, T003 |
| T010 | Login logging + account lockout logic | T007, T002 |

## Phase 2 — Module 1: Sales Forecast Config

| Task | Title | Dependencies |
|------|-------|-------------|
| T011 | DB migration: sales_forecast_config | T001 |
| T012 | Backend: Sales forecast config API (CRUD + auto-close scheduler) | T011, T002 |
| T013 | Frontend: Sales forecast config page | T012, T003 |

## Phase 3 — Module 2: Sales Forecast Forms

| Task | Title | Dependencies |
|------|-------|-------------|
| T014 | DB migration: sales_forecast | T001 |
| T015 | Backend: Excel upload + ERP product validation API | T014, T012 |
| T016 | Backend: Sales forecast CRUD API (single add/edit/delete) | T014, T012 |
| T017 | Backend: Sales forecast query + version management API | T014 |
| T018 | Frontend: Sales forecast upload page | T015, T013 |
| T019 | Frontend: Sales forecast list + inline edit + version selector | T016, T017, T003 |

## Phase 4 — Module 3: Sales Forecast Integration

| Task | Title | Dependencies |
|------|-------|-------------|
| T020 | Backend: 12-channel integration query API | T017 |
| T021 | Backend: Integration Excel export API | T020 |
| T022 | Frontend: Sales forecast integration page + Excel export | T020, T021, T003 |

## Phase 5 — Module 4: Inventory Integration

| Task | Title | Dependencies |
|------|-------|-------------|
| T023 | DB migration: inventory_sales_forecast | T001 |
| T024 | Backend: Inventory integration API (ERP query + calculation + versioning) | T023, T020 |
| T025 | Backend: Edit modified_subtotal API | T024 |
| T026 | Frontend: Inventory integration page + version selector + inline edit | T024, T025, T003 |

## Phase 6 — Module 5: Production Plan

| Task | Title | Dependencies |
|------|-------|-------------|
| T027 | DB migration: production_plan | T001 |
| T028 | Backend: Production plan query + edit API | T027, T024 |
| T029 | Frontend: Production plan page (yearly grid, inline edit) | T028, T003 |

## Phase 7 — Module 6: Weekly Production Schedule

| Task | Title | Dependencies |
|------|-------|-------------|
| T030 | DB migration: production_weekly_schedule | T001 |
| T031 | Backend: Weekly schedule upload + edit API | T030 |
| T032 | Backend: PDCA API integration service | T031 |
| T033 | Frontend: Weekly schedule page (upload, edit, query) | T031, T003 |

## Phase 8 — Module 7: Semi-Product Settings

| Task | Title | Dependencies |
|------|-------|-------------|
| T034 | DB migration: semi_product_advance_purchase | T001 |
| T035 | Backend: Semi-product settings upload + edit API | T034 |
| T036 | Frontend: Semi-product settings page | T035, T003 |

## Phase 9 — Module 8: Material Demand

| Task | Title | Dependencies |
|------|-------|-------------|
| T037 | DB migration: material_demand | T001 |
| T038 | Backend: Material demand query API (from PDCA results) | T037, T032 |
| T039 | Frontend: Material demand page | T038, T003 |

## Phase 10 — Module 9: Material Purchase

| Task | Title | Dependencies |
|------|-------|-------------|
| T040 | DB migration: material_purchase | T001 |
| T041 | Backend: Material purchase query + BOM expansion API | T040, T038 |
| T042 | Backend: Trigger ERP purchase order API | T041 |
| T043 | Frontend: Material purchase page + ERP trigger button | T041, T042, T003 |

---

## Cross-Cutting Concerns
- **ERP API integration:** Stubbed/mocked initially; real integration when ERP API docs provided
- **PDCA API integration:** Stubbed/mocked initially; real integration when PDCA API docs provided
- **Security:** All endpoints protected by JWT + permission checks; Swagger endpoints whitelisted
- **Validation:** Server-side validation on all inputs; client-side validation for UX
- **Versioning:** Sales forecast and inventory use timestamp-based versioning
- **Excel:** Apache POI for backend Excel generation/parsing

## Pending External Dependencies
- Tien-Hsin ERP API documentation
- PDCA system API documentation
- Product code encoding rules document
- Production capacity & standard work hours document
