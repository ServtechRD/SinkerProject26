# CLAUDE.md — SinkerProject26 專案指南

> 本文件提供 Claude Code 理解此專案所需的完整背景知識、技術規格與開發規範。  
> 執行任務前請先閱讀 `agent.md`（任務流程）與 `skill.md`（技術棧與指令）。

---

## 1. 專案概觀

### 1.1 專案目標

SinkerProject26 是一套**製造業供應鏈管理 Web 應用程式**，主要功能包含：

- **銷售預測管理**：各通路銷售預測填寫、版本控制、整合匯出
- **贈品銷售預測**：贈品品項獨立預測流程
- **庫存銷售預測**：整合庫存資料的銷售預測
- **物料需求計算**：依銷售預測自動推算原料需求量
- **物料採購管理**：採購單管理，整合外部 ERP 系統
- **生產計畫**：月度產能分配排程
- **週排程**：週作業計畫上傳與管理
- **半成品管理**：半成品預先採購計畫
- **使用者與角色管理**：RBAC 細粒度權限控制

### 1.2 系統邊界

- 前端為 SPA，部署於 Nginx，透過反向代理與後端溝通
- 後端提供 REST API，整合外部 PDCA 重算系統與 ERP 採購單系統
- 所有 DB Schema 變更透過 Flyway 管理，禁止手動修改

---

## 2. 技術棧

### 2.1 後端

| 技術 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 主要後端語言 |
| Spring Boot | 3.2.12 | Web 框架 |
| Spring Security | (Boot 內建) | JWT 認證 + RBAC |
| Spring Data JPA | (Boot 內建) | ORM 資料層 |
| MariaDB Driver | 3.3.2 | 資料庫連接 |
| Flyway | (Boot 內建) | DB Schema 版本控制 |
| jjwt | 0.12.5 | JWT 令牌產生與驗證 |
| Apache POI | 5.2.5 | Excel (.xlsx) 解析上傳 |
| SpringDoc OpenAPI | 2.5.0 | API 文件 (Swagger UI) |
| Gradle | 8.5 | 構建工具 |
| JUnit 5 | (Boot 內建) | 單元與整合測試 |
| Mockito | (Boot 內建) | Mock 框架 |
| TestContainers | (Boot 內建) | DB 整合測試容器 |
| JaCoCo | 0.8.11 | 測試覆蓋率 |

### 2.2 前端

| 技術 | 版本 | 用途 |
|------|------|------|
| React | 19.2.0 | UI 框架（函數元件 + Hooks） |
| Vite | 7.3.1 | 構建工具 / Dev Server |
| React Router | 7.13.0 | SPA 路由 |
| Axios | 1.13.5 | HTTP 客戶端 |
| Zustand | - | 局部全域狀態管理 |
| Vitest | 4.0.18 | 單元測試 |
| @testing-library/react | 16.3.2 | React 元件測試 |
| Playwright | 1.58.2 | E2E 測試 |
| ESLint | 9.39.1 | 程式碼品質檢查 |

### 2.3 資料庫

- **類型**：MariaDB 10+，UTF8MB4 字元集
- **Schema 管理**：Flyway (28 個遷移版本 V1~V28)
- **JPA 設定**：`ddl-auto: none`（所有 schema 由 Flyway 管理）
- **連接字串**：`jdbc:mariadb://db:3306/app`

### 2.4 基礎建設

- **容器化**：Docker Compose（開發 / 測試 / 覆蓋率三套環境）
- **CI/CD**：GitHub Actions（PR 觸發測試、自動合併）
- **反向代理**：Nginx（前端 + API 轉發）
- **任務管理介面**：Makefile

---

## 3. 專案架構

### 3.1 後端套件結構

```
com.sinker.app/
├── controller/      REST API 端點（13 個 Controller）
├── service/         業務邏輯（25+ Service）
├── repository/      JPA 倉儲介面（15+ Repository）
├── entity/          JPA 實體（20+ Entity）
├── dto/             資料傳輸物件（40+ DTO）
│   ├── auth/        認證相關 DTO
│   ├── forecast/    銷售預測相關 DTO
│   ├── role/        角色相關 DTO
│   └── reference/   參考資料 DTO
├── config/          Spring 設定類（Security, OpenAPI, Async 等）
├── security/        JWT 過濾器、使用者主體
├── exception/       自訂例外類（8 個）
├── validator/       自訂驗證器
├── converter/       JPA 屬性轉換器
├── util/            工具類（IP 解析、Excel 解析）
└── scheduler/       排程任務（AutoCloseScheduler）
```

### 3.2 前端目錄結構

```
frontend/src/
├── pages/           頁面元件（41 個，按功能模組分組）
│   ├── sales-forecast/
│   ├── users/
│   ├── roles/
│   ├── material/
│   ├── production/
│   ├── schedule/
│   ├── semiProduct/
│   └── inventory/
├── components/      通用 UI 元件（15 個）
├── api/             API 客戶端模組（15 個，對應後端 Controller）
├── contexts/        React Context（AuthContext）
├── layouts/         版面元件（MainLayout）
├── state/           Zustand 狀態 Store
└── utils/           工具函式
```

### 3.3 安全架構

- JWT 令牌有效期：24 小時（86400000ms）
- 令牌儲存：`localStorage`（authToken、user）
- 帳號鎖定：連續登入失敗 5 次後鎖定
- 授權粒度：方法級 `@PreAuthorize`，如 `sales_forecast.update_after_closed`
- 前端 Axios 攔截器：401 自動登出並導向 `/login`

---

## 4. 開發規範

### 4.1 命名規則

#### Java 後端

| 類型 | 規則 | 範例 |
|------|------|------|
| 類別名 | PascalCase | `SalesForecastService`, `CreateForecastRequest` |
| 方法名 | camelCase | `createForecast()`, `validateMonthFormat()` |
| 變數名 | camelCase | `userId`, `productCode`, `monthStr` |
| 常數 | UPPER_SNAKE_CASE | `FORM_SUMMARY_CHANNEL_ORDER`, `VALID_CHANNELS` |
| 套件名 | 全小寫 | `com.sinker.app.service` |
| 介面名 | PascalCase（無 I 前綴） | `SalesForecastRepository` |
| DTO 後綴 | `Request` / `Response` / `DTO` | `CreateForecastRequest`, `ForecastResponse` |
| 服務後綴 | `Service` | `SalesForecastService` |
| Controller 後綴 | `Controller` | `SalesForecastController` |
| Repository 後綴 | `Repository` | `SalesForecastRepository` |
| Entity 後綴 | 無後綴 | `SalesForecast`, `User` |

#### 資料庫

| 類型 | 規則 | 範例 |
|------|------|------|
| 表名 | snake_case、複數 | `sales_forecast`, `role_permissions` |
| 欄位名 | snake_case | `product_code`, `created_at`, `is_active` |
| 主鍵 | `id` | `id BIGINT AUTO_INCREMENT` |
| 外鍵欄位 | `{table}_id` | `role_id`, `user_id` |
| 布林欄位 | `is_` 前綴 | `is_active`, `is_locked` |
| 時間欄位 | `_at` 後綴 | `created_at`, `updated_at`, `closed_at` |

#### 前端 JavaScript/React

| 類型 | 規則 | 範例 |
|------|------|------|
| 頁面元件檔案 | PascalCase + `Page.jsx` 後綴 | `ForecastListPage.jsx` |
| 通用元件檔案 | PascalCase + `.jsx` | `ProtectedRoute.jsx`, `Toast.jsx` |
| API 模組 | camelCase + `.js` | `forecast.js`, `materialDemand.js` |
| Hook | `use` 前綴 | `useAuth`, `useToast` |
| 函數名 | camelCase | `handleSubmit`, `formatMonth` |
| 常數 | UPPER_SNAKE_CASE | `PAGE_SIZE_OPTIONS` |
| CSS 類別 | kebab-case | `login-container`, `form-summary-row` |

#### URL / API 路徑

| 類型 | 規則 | 範例 |
|------|------|------|
| REST API 路徑 | kebab-case | `/api/sales-forecast`, `/api/material-demand` |
| 前端路由 | kebab-case | `/sales-forecast/upload`, `/material-demand/form` |
| 查詢參數 | camelCase | `?month=2025-01&channel=7-11` |

### 4.2 後端程式碼撰寫習慣

#### Controller 層

```java
@RestController
@RequestMapping("/api/sales-forecast")
public class SalesForecastController {

    private final SalesForecastService salesForecastService;

    public SalesForecastController(SalesForecastService salesForecastService) {
        this.salesForecastService = salesForecastService;
    }

    @GetMapping("/{month}/{channel}")
    @PreAuthorize("hasAnyRole('admin', 'sales_user')")
    public ResponseEntity<ForecastResponse> getForecast(
            @PathVariable String month,
            @PathVariable String channel,
            @AuthenticationPrincipal JwtUserPrincipal user) {
        ForecastResponse response = salesForecastService.getForecast(month, channel, user.getId());
        return ResponseEntity.ok(response);
    }
}
```

規則：
- 建構子注入（不用 `@Autowired`）
- 每個端點明確標註 `@PreAuthorize`
- Controller 不含業務邏輯，只做轉發
- 使用 `ResponseEntity<T>` 明確控制 HTTP 狀態碼

#### Service 層

```java
@Service
public class SalesForecastService {

    @Transactional
    public ForecastResponse createForecast(CreateForecastRequest request, Long userId) {
        validateMonthFormat(request.getMonth());
        // 業務規則驗證在此層處理
        SalesForecast entity = buildEntity(request, userId);
        SalesForecast saved = salesForecastRepository.save(entity);
        return toResponse(saved);
    }

    private void validateMonthFormat(String month) {
        if (!month.matches("\\d{4}-\\d{2}")) {
            throw new IllegalArgumentException("月份格式錯誤，應為 YYYY-MM");
        }
    }
}
```

規則：
- 資料庫操作加 `@Transactional`
- 私有驗證方法放在 Service 內
- 實體轉換（entity ↔ DTO）由 Service 負責

#### DTO 層

```java
public class CreateForecastRequest {

    @NotBlank(message = "month is required")
    private String month;

    @NotBlank(message = "channel is required")
    private String channel;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0", message = "quantity must be >= 0")
    private BigDecimal quantity;

    // getter / setter
}
```

規則：
- 使用 Bean Validation 註解（`@NotBlank`, `@NotNull`, `@DecimalMin`）
- 錯誤訊息以英文撰寫（API 層面），UI 顯示再轉換

#### Entity 層

```java
@Entity
@Table(name = "sales_forecast")
public class SalesForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String month;  // 格式: YYYY-MM

    @Column(name = "product_code", nullable = false)
    private String productCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
```

規則：
- `ddl-auto: none`，所有 schema 由 Flyway SQL 管理
- 關聯使用 `FetchType.LAZY`（`@ManyToOne` 預設），需要 eager 才指定
- 時間欄位使用 `LocalDateTime`
- 金額/數量使用 `BigDecimal`

### 4.3 錯誤處理方式

#### 自訂例外類別

位於 `com.sinker.app.exception/`：

```
ResourceNotFoundException      → 404 (找不到資源)
AccountLockedException         → 403 (帳號鎖定)
AccountInactiveException       → 403 (帳號停用)
ExcelParseException            → 400 (Excel 解析失敗)
ExternalApiException           → 502 (外部 API 呼叫失敗)
```

#### 錯誤回應格式

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "month is required",
  "path": "/api/sales-forecast"
}
```

#### 前端錯誤處理

```javascript
try {
  await someApiCall()
} catch (err) {
  const message = err.response?.data?.message || err.message || '發生未知錯誤'
  showToast(message, 'error')
} finally {
  setLoading(false)
}
```

規則：
- 所有 API 呼叫加 try/catch
- 使用 Toast 元件顯示錯誤訊息
- `finally` 區塊確保 loading 狀態重置
- 401 錯誤由 Axios 攔截器統一處理（自動登出）

### 4.4 前端元件撰寫習慣

```jsx
export default function ForecastListPage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const { showToast } = useToast()

  useEffect(() => {
    fetchData()
  }, [])

  async function fetchData() {
    setLoading(true)
    try {
      const result = await getForecastList()
      setData(result)
    } catch (err) {
      showToast(err.response?.data?.message || '載入失敗', 'error')
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit(e) {
    e.preventDefault()
    // 處理表單提交
  }

  return (
    <div className="page-container">
      {/* JSX */}
    </div>
  )
}
```

規則：
- 使用函數宣告（`function`），不用箭頭函數定義元件
- `useState` 宣告在函數開頭
- 事件處理函數命名 `handle{Event}`（如 `handleSubmit`, `handleDelete`）
- API 呼叫包在 `async` 函數內，搭配 try/catch/finally
- 不用 `useEffect` 的清理需記得 cleanup（如取消請求）

### 4.5 API 客戶端慣例

```javascript
// api/forecast.js
export async function getForecastList(month, channel) {
  const response = await api.get(`/api/sales-forecast/${month}/${channel}`)
  return response.data
}

export async function createForecast(request) {
  const response = await api.post('/api/sales-forecast', request)
  return response.data
}
```

規則：
- 每個後端 Controller 對應一個 API 模組檔案
- 使用 `/api/...` 相對路徑（不寫 localhost）
- 函數回傳 `response.data`，不暴露 Axios 物件給呼叫端
- 函數命名：`get{Resource}`, `create{Resource}`, `update{Resource}`, `delete{Resource}`

---

## 5. 資料格式規範

| 資料類型 | 格式 | 範例 |
|----------|------|------|
| 月份 | `YYYY-MM` | `2025-01` |
| 日期 | `YYYY-MM-DD` | `2025-01-15` |
| 日期時間 | ISO 8601 | `2025-01-15T10:30:00` |
| 數量/金額 | BigDecimal，精度 10,2 | `1234.56` |
| JSON 鍵名 | camelCase | `productCode`, `createdAt` |

---

## 6. 測試規範

### 6.1 後端測試

```
backend/src/test/java/com/sinker/app/
├── controller/     *IntegrationTest.java（43 個，TestContainers）
├── service/        *ServiceTest.java（Mockito）
├── security/       JWT 相關測試
└── migration/      Flyway 遷移測試
```

**覆蓋率目標**：`service`, `util`, `converter`, `dto.auth` 套件 ≥ 80%

撰寫規則：
- 整合測試使用 `@SpringBootTest` + TestContainers（真實 MariaDB）
- 單元測試使用 `@Mock` + `@InjectMocks` (Mockito)
- 測試類別命名：`{ClassName}Test`（單元）、`{ClassName}IntegrationTest`（整合）

### 6.2 前端測試

```
frontend/src/
├── pages/**/__tests__/*.test.jsx   (Vitest + RTL)
├── contexts/__tests__/
├── layouts/__tests__/
└── api/__tests__/
```

**覆蓋率目標**：lines / branches / functions / statements ≥ 70%

### 6.3 E2E 測試

```
frontend/e2e/
├── login.spec.js           登入流程（必測）
├── forecast.spec.js        核心業務流程
└── global-setup.js         測試環境初始化（SQL fixtures）
```

**執行環境**：Chrome 瀏覽器，失敗時自動截圖/錄影，CI 失敗重試 1 次

---

## 7. 執行環境（Docker-first）

**絕對規則**：主機視為未安裝 Java / Gradle / Node / NPM

所有指令必須透過：

```bash
# 偏好（Makefile）
make dev-up           # 啟動開發環境
make test-compose     # 執行全套測試（後端 + E2E）
make test-down        # 清理測試環境
make coverage-backend # 生成後端覆蓋率報告

# 備用（docker compose exec）
docker compose exec backend_server ./gradlew test
docker compose exec frontend npm run build
```

### Docker Compose 服務名稱

| 服務名稱 | 用途 |
|----------|------|
| `db` | MariaDB 10 |
| `backend_server` | Spring Boot API |
| `frontend` | React Vite Dev Server |
| `backend_unit` | 後端單元測試（測試環境） |
| `e2e` | Playwright E2E 測試 |

---

## 8. Git 工作流程

### 分支命名

```
功能：claude/feat/<TASK-ID>-<slug>   例：claude/feat/F001-login
修正：claude/fix/<TASK-ID>-<slug>    例：claude/fix/X002-account-lock
```

### Commit 格式（Conventional Commits）

```
feat(F001): 新增銷售預測列表頁
fix(X002): 修正帳號鎖定判斷邏輯
test(T005): 新增 SalesForecastService 單元測試
refactor(F003): 拆分 ForecastService 過長方法
chore: 更新 Flyway 遷移腳本
```

### PR 規則

- 一任務 = 一分支 = 一 PR
- 目標分支：`claude/intergration`（注意：非 `integration`，這是 repo 的拼寫）
- Agent 不得自行合併 PR

---

## 9. 安全規範

- 永不停用 JWT 認證或角色檢查
- 不直接 commit 到 `main` 或 `claude/intergration`
- 不強制推送任何共用分支
- DB Schema 變更只透過 Flyway 遷移檔案（位於 `backend/src/main/resources/db/migration/`）
- 外部系統 URL 透過環境變數注入（`IntegrationProperties`），不寫死

---

## 10. 任務管理

任務規格位於：
```
spec/feat/<TASK-ID>-<slug>/
    description.md      任務描述
    acceptance.md       驗收標準
    status.todo         未開始
    status.doing        進行中
    status.done         已完成
```

UI 文字術語：遵循 `spec/task/X008-ui-text-zh-tw/glossary.md`，不自行造詞。

---

## 11. 輸出偏好（給 Claude）

- **回覆語言**：繁體中文
- **程式碼**：必須提供完整版本，不省略任何部分（不用 `// ...existing code...` 佔位）
- **架構建議**：先簡述方案與取捨，等使用者確認後再實作
- **程式碼風格**：遵循本文件第 4 節規範，不隨意新增抽象層
- **注釋**：預設不加注釋，僅在 WHY 非顯而易見時加一行說明
- **任務範圍**：只做任務要求的事，不擴增功能、不重構無關程式碼

---

*最後更新：2026-05-11*
