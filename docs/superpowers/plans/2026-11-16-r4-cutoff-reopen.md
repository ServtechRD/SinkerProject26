# F129 R4 截止日與鎖定邏輯（含開放補登）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 截止時間（日期＋時間）一到自動鎖定；之後只能由生管直接修正（原因必填），或對指定通路開放補登（上限 3 個工作天、到期自動鎖回並併成新的生管版本），所有動作留異動紀錄。

**Architecture:** `sales_forecast_config.is_closed` 不變；新增 `sales_forecast_reopen_window`（月份＋通路一筆）與 `sales_forecast_form_log`。業務上傳／編輯統一走 `ForecastEditGuard.assertBusinessEditable(config, channel)`。補登結束（到期或提前結束）時由 `FormSummaryService.mergeReopenedChannels` 以最新生管版本為底、替換補登通路數量，產生 `form_vN+1`。排程改每分鐘跑「自動截止」與「補登到期」。

**Tech Stack:** Spring Boot 3.2 / JPA / Flyway / MariaDB；React 19 + Vite + Vitest + RTL。

**設計文件:** `docs/superpowers/specs/2026-11-16-r4-cutoff-reopen-design.md`

## Global Constraints

- 回覆、程式註解一律繁體中文；commit 訊息不加任何 Claude 署名或 `Co-Authored-By`。
- Docker-first：主機沒有 Java／Node。所有指令在 WSL 執行：`wsl.exe -d Ubuntu-24.04 -- bash -lc 'cd ~/SinkerProject26 && <指令>'`。commit 在 WSL 做；push 用 Windows git（本計畫不 push）。
- 測試 compose 一律加 `-p sinker_test`，否則會清掉開發容器。以下用簡寫：
  - `BE_UNIT "<pattern>"` = `docker compose -p sinker_test -f docker-compose.test.yml build backend_unit && docker compose -p sinker_test -f docker-compose.test.yml run --rm --no-deps backend_unit sh ./gradlew test --tests "<pattern>"`
  - `BE_IT "<pattern>"` = 同上但**不加** `--no-deps`（需要 db）；跑完 `docker compose -p sinker_test -f docker-compose.test.yml down -v`
  - `FE_TEST <files>` = `docker run --rm -v "$PWD/frontend:/app" -w /app node:20 npx vitest run <files>`
  - `FE_LINT <files>` = `docker run --rm -v "$PWD/frontend:/app" -w /app node:20 npx eslint <files>`
- 分支：`feat/F129-forecast-cutoff-reopen`（已建立，疊在 F127 上）。不得 commit 到 `main`／`claude/intergration`。
- DB 變更只透過 Flyway；目前最新為 `V34__create_vendor.sql`，本計畫新增 V35、V36。
- 不得停用 JWT 或 `@PreAuthorize`；每個新端點都要標權限。
- 新增欄位的時間（`auto_close_at`、`expires_at`、`opened_at`、`ended_at`、log 的 `created_at`）一律為**台灣時間**，用注入的 `Clock`（`Asia/Taipei`）取得；前端用 `formatLocalDateTime` 原樣顯示。既有欄位（`closed_at`、`sales_forecast.created_at`）維持原行為，前端用 `formatTaipeiDateTime`。
- 通路清單沿用 `FormSummaryService.CHANNEL_ORDER`（R3 通路主檔上線後改讀主檔）。
- 錯誤：規則違反丟 `IllegalArgumentException`（中文訊息）→ 400；業務被鎖丟 `org.springframework.security.access.AccessDeniedException` → 403。

---

### Task 0: 記錄測試基線

**Files:** 無（只記錄）

- [ ] **Step 1: 跑後端全部單元測試並記錄失敗清單**

Run: `BE_UNIT "*"`（從 `~/SinkerProject26`）
把 `FAILED` 的測試類別名稱存到 scratchpad `be_baseline.txt`。已知 `FlywayMigrationTest` 仍引用 `auto_close_day`，屬既有失敗。

- [ ] **Step 2: 跑前端全部測試並記錄**

Run: `FE_TEST`（不帶檔名）
Expected: 13 failed（`CreateMonthsDialog.test.jsx` 6、`EditConfigDialog.test.jsx` 6、`api/__tests__/forecastConfig.test.js` 1），其餘通過。存到 scratchpad `fe_baseline.txt`。

驗收：後續每個 Task 結束時，失敗清單只能比基線少，不能多。

---

### Task 1: 基礎建設：V35（補登期間表、異動紀錄表、權限 35）、Clock、異動紀錄服務

**Files:**
- Create: `backend/src/main/resources/db/migration/V35__forecast_reopen_window_and_form_log.sql`
- Create: `backend/src/main/java/com/sinker/app/config/ClockConfig.java`
- Create: `backend/src/main/java/com/sinker/app/entity/FormLogAction.java`
- Create: `backend/src/main/java/com/sinker/app/entity/SalesForecastFormLog.java`
- Create: `backend/src/main/java/com/sinker/app/entity/SalesForecastReopenWindow.java`
- Create: `backend/src/main/java/com/sinker/app/repository/SalesForecastFormLogRepository.java`
- Create: `backend/src/main/java/com/sinker/app/repository/SalesForecastReopenWindowRepository.java`
- Create: `backend/src/main/java/com/sinker/app/dto/forecast/FormLogDTO.java`
- Create: `backend/src/main/java/com/sinker/app/service/FormLogService.java`
- Test: `backend/src/test/java/com/sinker/app/service/FormLogServiceTest.java`

**Interfaces:**
- Produces:
  - `enum FormLogAction { AUTO_CLOSE, MANUAL_CLOSE, REOPEN, REOPEN_END, CORRECTION }`
  - `FormLogService.record(String month, String channel, FormLogAction action, Long operatorId, String detail)`（channel／operatorId 可為 null）
  - `FormLogService.list(String month) : List<FormLogDTO>`（新到舊）
  - `record FormLogDTO(Integer id, LocalDateTime createdAt, String operatorName, String action, String channel, String detail)`
  - `SalesForecastReopenWindowRepository`：
    - `Optional<SalesForecastReopenWindow> findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter(String month, String channel, LocalDateTime now)`
    - `boolean existsByMonthAndChannelAndEndedAtIsNull(String month, String channel)`
    - `List<SalesForecastReopenWindow> findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime now)`
    - `List<SalesForecastReopenWindow> findByMonthAndEndedAtIsNull(String month)`
    - `long countByMonthAndEndedAtIsNull(String month)`
  - `SalesForecastFormLogRepository.findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(String month, Collection<FormLogAction> actions) : Optional<SalesForecastFormLog>`
  - Spring bean `java.time.Clock`（Asia/Taipei）

- [ ] **Step 1: 寫 Flyway V35**

```sql
-- V35: R4 補登期間、表單異動紀錄、權限 35「開放補登」

CREATE TABLE sales_forecast_reopen_window (
    id             INT          NOT NULL AUTO_INCREMENT,
    month          VARCHAR(7)   NOT NULL,
    channel        VARCHAR(50)  NOT NULL,
    expires_at     DATETIME     NOT NULL,
    reason         VARCHAR(200) NOT NULL,
    opened_by      BIGINT       NOT NULL,
    opened_at      DATETIME     NOT NULL,
    ended_at       DATETIME     NULL,
    ended_by       BIGINT       NULL,
    merged_version VARCHAR(20)  NULL,
    PRIMARY KEY (id),
    KEY idx_reopen_window_month_channel (month, channel, ended_at),
    KEY idx_reopen_window_expires (ended_at, expires_at)
);

CREATE TABLE sales_forecast_form_log (
    id          INT          NOT NULL AUTO_INCREMENT,
    month       VARCHAR(7)   NOT NULL,
    channel     VARCHAR(50)  NULL,
    action      VARCHAR(30)  NOT NULL,
    operator_id BIGINT       NULL,
    detail      VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_form_log_month (month, created_at)
);

INSERT INTO permissions (code, name, module, description) VALUES
('sales_forecast_config.reopen', '開放補登', 'sales_forecast_config', '已結束新增的月份，對指定通路開放補登');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('production_planner', 'admin') AND p.code = 'sales_forecast_config.reopen';
```

- [ ] **Step 2: ClockConfig**

```java
package com.sinker.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** 截止、補登、異動紀錄一律以台灣時間判斷，不受容器時區影響；測試可替換為固定時鐘 */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Taipei"));
    }
}
```

- [ ] **Step 3: 實體與 enum**

`FormLogAction.java`:
```java
package com.sinker.app.entity;

/** 表單異動紀錄動作 */
public enum FormLogAction {
    AUTO_CLOSE, MANUAL_CLOSE, REOPEN, REOPEN_END, CORRECTION
}
```

`SalesForecastFormLog.java`:
```java
package com.sinker.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_forecast_form_log")
public class SalesForecastFormLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(length = 50)
    private String channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormLogAction action;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public FormLogAction getAction() { return action; }
    public void setAction(FormLogAction action) { this.action = action; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

`SalesForecastReopenWindow.java`:
```java
package com.sinker.app.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_forecast_reopen_window")
public class SalesForecastReopenWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 7)
    private String month;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "opened_by", nullable = false)
    private Long openedBy;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "ended_by")
    private Long endedBy;

    @Column(name = "merged_version", length = 20)
    private String mergedVersion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getOpenedBy() { return openedBy; }
    public void setOpenedBy(Long openedBy) { this.openedBy = openedBy; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public Long getEndedBy() { return endedBy; }
    public void setEndedBy(Long endedBy) { this.endedBy = endedBy; }
    public String getMergedVersion() { return mergedVersion; }
    public void setMergedVersion(String mergedVersion) { this.mergedVersion = mergedVersion; }
}
```

- [ ] **Step 4: Repository 與 DTO**

`SalesForecastFormLogRepository.java`:
```java
package com.sinker.app.repository;

import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalesForecastFormLogRepository extends JpaRepository<SalesForecastFormLog, Integer> {

    List<SalesForecastFormLog> findByMonthOrderByCreatedAtDescIdDesc(String month);

    Optional<SalesForecastFormLog> findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(
            String month, Collection<FormLogAction> actions);
}
```

`SalesForecastReopenWindowRepository.java`:
```java
package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecastReopenWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalesForecastReopenWindowRepository extends JpaRepository<SalesForecastReopenWindow, Integer> {

    /** 該月份＋通路目前有效（未結束且未到期）的補登 */
    Optional<SalesForecastReopenWindow> findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter(
            String month, String channel, LocalDateTime now);

    /** 未結束（含已到期但排程尚未處理）的補登，用於阻擋重複開放 */
    boolean existsByMonthAndChannelAndEndedAtIsNull(String month, String channel);

    List<SalesForecastReopenWindow> findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime now);

    List<SalesForecastReopenWindow> findByMonthAndEndedAtIsNull(String month);

    long countByMonthAndEndedAtIsNull(String month);
}
```

`FormLogDTO.java`:
```java
package com.sinker.app.dto.forecast;

import java.time.LocalDateTime;

/** 表單異動紀錄；createdAt 為台灣時間，operatorName 系統動作為「系統」 */
public record FormLogDTO(Integer id, LocalDateTime createdAt, String operatorName,
                         String action, String channel, String detail) {
}
```

- [ ] **Step 5: 寫失敗測試 FormLogServiceTest**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.entity.User;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormLogServiceTest {

    // 2026-12-10 08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));

    @Mock private SalesForecastFormLogRepository repository;
    @Mock private UserRepository userRepository;

    private FormLogService service;

    @BeforeEach
    void setUp() {
        service = new FormLogService(repository, userRepository, CLOCK);
    }

    @Test
    void record_savesWithTaipeiTimeAndTruncatesDetail() {
        service.record("202612", "家樂福", FormLogAction.REOPEN, 5L, "x".repeat(600));

        ArgumentCaptor<SalesForecastFormLog> captor = ArgumentCaptor.forClass(SalesForecastFormLog.class);
        verify(repository).save(captor.capture());
        SalesForecastFormLog saved = captor.getValue();
        assertEquals("202612", saved.getMonth());
        assertEquals("家樂福", saved.getChannel());
        assertEquals(FormLogAction.REOPEN, saved.getAction());
        assertEquals(5L, saved.getOperatorId());
        assertEquals(500, saved.getDetail().length());
        assertEquals(LocalDateTime.of(2026, 12, 10, 8, 0), saved.getCreatedAt());
    }

    @Test
    void list_mapsOperatorNamesAndSystem() {
        SalesForecastFormLog byUser = log(1, FormLogAction.REOPEN, 5L);
        SalesForecastFormLog bySystem = log(2, FormLogAction.AUTO_CLOSE, null);
        when(repository.findByMonthOrderByCreatedAtDescIdDesc("202612")).thenReturn(List.of(byUser, bySystem));
        User u = new User();
        u.setId(5L);
        u.setUsername("pm");
        u.setFullName("陳美玲");
        when(userRepository.findAllById(Set.of(5L))).thenReturn(List.of(u));

        List<FormLogDTO> result = service.list("202612");

        assertEquals(2, result.size());
        assertEquals("陳美玲", result.get(0).operatorName());
        assertEquals("REOPEN", result.get(0).action());
        assertEquals("系統", result.get(1).operatorName());
    }

    private static SalesForecastFormLog log(int id, FormLogAction action, Long operatorId) {
        SalesForecastFormLog l = new SalesForecastFormLog();
        l.setId(id);
        l.setMonth("202612");
        l.setAction(action);
        l.setOperatorId(operatorId);
        l.setCreatedAt(LocalDateTime.of(2026, 12, 10, 8, 0));
        return l;
    }
}
```

若 `User` 沒有 `setId`／`setFullName`，改用其既有 setter 名稱（先 `grep -n "public void set" backend/src/main/java/com/sinker/app/entity/User.java` 確認）。

- [ ] **Step 6: 跑測試確認失敗**

Run: `BE_UNIT "*FormLogServiceTest"`
Expected: 編譯失敗（`FormLogService` 不存在）。

- [ ] **Step 7: 實作 FormLogService**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 表單異動紀錄：只新增、不提供修改與刪除 */
@Service
public class FormLogService {

    private static final int DETAIL_MAX = 500;

    private final SalesForecastFormLogRepository repository;
    private final UserRepository userRepository;
    private final Clock clock;

    public FormLogService(SalesForecastFormLogRepository repository, UserRepository userRepository, Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(String month, String channel, FormLogAction action, Long operatorId, String detail) {
        SalesForecastFormLog entry = new SalesForecastFormLog();
        entry.setMonth(month);
        entry.setChannel(channel);
        entry.setAction(action);
        entry.setOperatorId(operatorId);
        entry.setDetail(detail != null && detail.length() > DETAIL_MAX ? detail.substring(0, DETAIL_MAX) : detail);
        entry.setCreatedAt(LocalDateTime.now(clock));
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<FormLogDTO> list(String month) {
        List<SalesForecastFormLog> logs = repository.findByMonthOrderByCreatedAtDescIdDesc(month);
        Set<Long> ids = logs.stream().map(SalesForecastFormLog::getOperatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> names.put(u.getId(),
                    u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername()));
        }
        return logs.stream().map(l -> new FormLogDTO(
                l.getId(),
                l.getCreatedAt(),
                l.getOperatorId() == null ? "系統" : names.getOrDefault(l.getOperatorId(), "使用者#" + l.getOperatorId()),
                l.getAction().name(),
                l.getChannel(),
                l.getDetail())).toList();
    }
}
```

- [ ] **Step 8: 跑測試確認通過**

Run: `BE_UNIT "*FormLogServiceTest"`
Expected: `BUILD SUCCESSFUL`，2 tests passed。

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/resources/db/migration/V35__forecast_reopen_window_and_form_log.sql \
  backend/src/main/java/com/sinker/app/config/ClockConfig.java \
  backend/src/main/java/com/sinker/app/entity/FormLogAction.java \
  backend/src/main/java/com/sinker/app/entity/SalesForecastFormLog.java \
  backend/src/main/java/com/sinker/app/entity/SalesForecastReopenWindow.java \
  backend/src/main/java/com/sinker/app/repository/SalesForecastFormLogRepository.java \
  backend/src/main/java/com/sinker/app/repository/SalesForecastReopenWindowRepository.java \
  backend/src/main/java/com/sinker/app/dto/forecast/FormLogDTO.java \
  backend/src/main/java/com/sinker/app/service/FormLogService.java \
  backend/src/test/java/com/sinker/app/service/FormLogServiceTest.java
git commit -m "feat(F129): 新增補登期間表、表單異動紀錄與開放補登權限"
```

---

### Task 2: 截止改為日期＋時間，自動截止與手動結束寫異動紀錄（R4-1）

**Files:**
- Create: `backend/src/main/resources/db/migration/V36__forecast_config_auto_close_at.sql`
- Modify: `backend/src/main/java/com/sinker/app/entity/SalesForecastConfig.java`（第 4、18–19、39–40 行）
- Modify: `backend/src/main/java/com/sinker/app/repository/SalesForecastConfigRepository.java:17`
- Modify: `backend/src/main/java/com/sinker/app/dto/forecast/ConfigResponse.java`
- Modify: `backend/src/main/java/com/sinker/app/dto/forecast/CreateMonthsRequest.java`
- Modify: `backend/src/main/java/com/sinker/app/dto/forecast/UpdateConfigRequest.java`
- Modify (整檔取代): `backend/src/main/java/com/sinker/app/service/SalesForecastConfigService.java`
- Modify: `backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java`
- Modify (整檔取代): `backend/src/main/java/com/sinker/app/scheduler/AutoCloseScheduler.java`
- Test (整檔取代): `backend/src/test/java/com/sinker/app/service/SalesForecastConfigServiceTest.java`
- Test (整檔取代): `backend/src/test/java/com/sinker/app/scheduler/AutoCloseSchedulerTest.java`
- Test: `backend/src/test/java/com/sinker/app/controller/SalesForecastConfigControllerIntegrationTest.java`

**Interfaces:**
- Consumes: `FormLogService.record`、`SalesForecastReopenWindowRepository.countByMonthAndEndedAtIsNull`、`Clock`
- Produces:
  - `SalesForecastConfig.getAutoCloseAt()/setAutoCloseAt(LocalDateTime)`
  - `SalesForecastConfigService.createMonth(String month, LocalDateTime autoCloseAt)`
  - `SalesForecastConfigService.updateConfig(Integer id, UpdateConfigRequest request, Long userId)`
  - `SalesForecastConfigService.autoCloseDue() : int`
  - `ConfigResponse.getAutoCloseAt() : LocalDateTime`、`getActiveReopenCount() : long`
  - JSON 欄位：`autoCloseAt`（例 `"2026-12-10T00:00:00"`）、`activeReopenCount`
  - Scheduler 方法 `AutoCloseScheduler.runEveryMinute()`（Task 6 會再加補登到期）

- [ ] **Step 1: 寫 Flyway V36**

```sql
-- V36: 自動結束新增設定由日期改為日期＋時間（台灣時間），舊資料以當天 00:00 回填

ALTER TABLE sales_forecast_config
    ADD COLUMN auto_close_at DATETIME NOT NULL DEFAULT '2099-12-31 00:00:00' AFTER month;

UPDATE sales_forecast_config SET auto_close_at = TIMESTAMP(auto_close_date);

DROP INDEX idx_sales_forecast_config_auto_close_date ON sales_forecast_config;

ALTER TABLE sales_forecast_config DROP COLUMN auto_close_date;

CREATE INDEX idx_sales_forecast_config_auto_close_at ON sales_forecast_config (auto_close_at);
```

- [ ] **Step 2: 實體、Repository、DTO**

`SalesForecastConfig.java`：
- 第 4 行 `import java.time.LocalDate;` 刪除。
- 第 18–19 行改為：
```java
    @Column(name = "auto_close_at", nullable = false)
    private LocalDateTime autoCloseAt;
```
- 第 39–40 行改為：
```java
    public LocalDateTime getAutoCloseAt() { return autoCloseAt; }
    public void setAutoCloseAt(LocalDateTime autoCloseAt) { this.autoCloseAt = autoCloseAt; }
```

`SalesForecastConfigRepository.java:17` 改為：
```java
    List<SalesForecastConfig> findByIsClosedFalseAndAutoCloseAtLessThanEqual(java.time.LocalDateTime now);
```

`ConfigResponse.java`：把 `LocalDate autoCloseDate` 欄位、`setAutoCloseDate(entity.getAutoCloseDate())`、getter/setter 改成下列，並刪除 `import java.time.LocalDate;`：
```java
    private LocalDateTime autoCloseAt;
    private long activeReopenCount;
```
```java
        dto.setAutoCloseAt(entity.getAutoCloseAt());
```
```java
    public LocalDateTime getAutoCloseAt() { return autoCloseAt; }
    public void setAutoCloseAt(LocalDateTime autoCloseAt) { this.autoCloseAt = autoCloseAt; }
    public long getActiveReopenCount() { return activeReopenCount; }
    public void setActiveReopenCount(long activeReopenCount) { this.activeReopenCount = activeReopenCount; }
```

`CreateMonthsRequest.java`：`autoCloseDate` 欄位與 getter/setter 改成：
```java
    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$",
            message = "autoCloseAt format must be YYYY-MM-DDTHH:mm")
    private String autoCloseAt;
```
```java
    public String getAutoCloseAt() { return autoCloseAt; }
    public void setAutoCloseAt(String autoCloseAt) { this.autoCloseAt = autoCloseAt; }
```

`UpdateConfigRequest.java`：`autoCloseDate` 改名為 `autoCloseAt`（欄位、getter `getAutoCloseAt`、setter `setAutoCloseAt`，型別仍為 `String`）。

- [ ] **Step 3: 寫失敗測試（整檔取代 SalesForecastConfigServiceTest）**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesForecastConfigServiceTest {

    // 2026-12-10 08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastConfigRepository repository;
    @Mock private FormSummaryService formSummaryService;
    @Mock private FormLogService formLogService;
    @Mock private SalesForecastReopenWindowRepository windowRepository;

    private SalesForecastConfigService service;

    @BeforeEach
    void setUp() {
        service = new SalesForecastConfigService(repository, formSummaryService, formLogService, windowRepository, CLOCK);
    }

    private SalesForecastConfig config(Integer id, String month, LocalDateTime autoCloseAt, boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(id);
        c.setMonth(month);
        c.setAutoCloseAt(autoCloseAt);
        c.setIsClosed(closed);
        c.setClosedAt(closed ? LocalDateTime.of(2026, 12, 9, 10, 0) : null);
        return c;
    }

    @Test
    void createMonth_setsAutoCloseAt() {
        when(repository.existsByMonth("202612")).thenReturn(false);
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfigResponse r = service.createMonth("202612", LocalDateTime.of(2026, 12, 10, 18, 30));

        assertEquals("202612", r.getMonth());
        assertEquals(LocalDateTime.of(2026, 12, 10, 18, 30), r.getAutoCloseAt());
        assertFalse(r.getIsClosed());
    }

    @Test
    void createMonth_duplicate() {
        when(repository.existsByMonth("202612")).thenReturn(true);
        assertThrows(SalesForecastConfigService.DuplicateMonthException.class,
                () -> service.createMonth("202612", NOW));
    }

    @Test
    void createMonth_invalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> service.createMonth("20261", NOW));
    }

    @Test
    void listAll_includesActiveReopenCount() {
        when(repository.findAllByOrderByMonthDesc()).thenReturn(List.of(config(1, "202612", NOW, true)));
        when(windowRepository.countByMonthAndEndedAtIsNull("202612")).thenReturn(2L);

        List<ConfigResponse> list = service.listAll();

        assertEquals(1, list.size());
        assertEquals(2L, list.get(0).getActiveReopenCount());
    }

    @Test
    void updateConfig_manualClose_logsAndSnapshots() {
        SalesForecastConfig c = config(1, "202612", NOW, false);
        when(repository.findById(1)).thenReturn(Optional.of(c));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(true);

        ConfigResponse r = service.updateConfig(1, req, 7L);

        assertTrue(r.getIsClosed());
        assertNotNull(r.getClosedAt());
        verify(formSummaryService).createFormVersion1Snapshot(eq("202612"), any());
        verify(formLogService).record(eq("202612"), isNull(), eq(FormLogAction.MANUAL_CLOSE), eq(7L), anyString());
    }

    @Test
    void updateConfig_cannotReopen() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, true)));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(false);
        assertThrows(IllegalArgumentException.class, () -> service.updateConfig(1, req, 7L));
    }

    @Test
    void updateConfig_closedUnchanged_doesNotLog() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, true)));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setIsClosed(true);

        service.updateConfig(1, req, 7L);

        verify(formLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void updateConfig_validAutoCloseAt() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, false)));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setAutoCloseAt("2026-12-11T18:00");

        ConfigResponse r = service.updateConfig(1, req, 7L);

        assertEquals(LocalDateTime.of(2026, 12, 11, 18, 0), r.getAutoCloseAt());
    }

    @Test
    void updateConfig_invalidAutoCloseAt() {
        when(repository.findById(1)).thenReturn(Optional.of(config(1, "202612", NOW, false)));
        UpdateConfigRequest req = new UpdateConfigRequest();
        req.setAutoCloseAt("2026/12/11");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(1, req, 7L));
        assertTrue(ex.getMessage().contains("autoCloseAt"));
    }

    @Test
    void updateConfig_notFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateConfig(99, new UpdateConfigRequest(), 7L));
    }

    @Test
    void autoCloseDue_closesDueMonthsWithLog() {
        SalesForecastConfig a = config(1, "202612", NOW.minusMinutes(1), false);
        SalesForecastConfig b = config(2, "202701", NOW, false);
        when(repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(NOW)).thenReturn(List.of(a, b));
        when(repository.save(any(SalesForecastConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = service.autoCloseDue();

        assertEquals(2, count);
        assertTrue(a.getIsClosed());
        assertTrue(b.getIsClosed());
        verify(formSummaryService, times(2)).createFormVersion1Snapshot(anyString(), any());
        verify(formLogService).record(eq("202612"), isNull(), eq(FormLogAction.AUTO_CLOSE), isNull(), anyString());
        verify(formLogService).record(eq("202701"), isNull(), eq(FormLogAction.AUTO_CLOSE), isNull(), anyString());
    }

    @Test
    void autoCloseDue_none() {
        when(repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(NOW)).thenReturn(List.of());
        assertEquals(0, service.autoCloseDue());
        verify(repository, never()).save(any());
    }
}
```

- [ ] **Step 4: 跑測試確認失敗**

Run: `BE_UNIT "*SalesForecastConfigServiceTest"`
Expected: 編譯失敗（建構子參數、`autoCloseDue` 不存在）。

- [ ] **Step 5: 整檔取代 SalesForecastConfigService**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class SalesForecastConfigService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastConfigService.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final SalesForecastConfigRepository repository;
    private final FormSummaryService formSummaryService;
    private final FormLogService formLogService;
    private final SalesForecastReopenWindowRepository windowRepository;
    private final Clock clock;

    public SalesForecastConfigService(SalesForecastConfigRepository repository,
                                      FormSummaryService formSummaryService,
                                      FormLogService formLogService,
                                      SalesForecastReopenWindowRepository windowRepository,
                                      Clock clock) {
        this.repository = repository;
        this.formSummaryService = formSummaryService;
        this.formLogService = formLogService;
        this.windowRepository = windowRepository;
        this.clock = clock;
    }

    @Transactional
    public ConfigResponse createMonth(String monthStr, LocalDateTime autoCloseAt) {
        YearMonth ym = parseMonth(monthStr);
        String formattedMonth = ym.format(MONTH_FORMAT);

        if (repository.existsByMonth(formattedMonth)) {
            throw new DuplicateMonthException("Month already exists: " + formattedMonth);
        }

        SalesForecastConfig config = new SalesForecastConfig();
        config.setMonth(formattedMonth);
        config.setAutoCloseAt(autoCloseAt);
        config.setIsClosed(false);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        try {
            SalesForecastConfig saved = repository.save(config);
            log.info("Created forecast config for month {}", formattedMonth);
            return ConfigResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateMonthException("Month already exists: " + formattedMonth);
        }
    }

    @Transactional(readOnly = true)
    public List<ConfigResponse> listAll() {
        return repository.findAllByOrderByMonthDesc().stream()
                .map(c -> {
                    ConfigResponse r = ConfigResponse.fromEntity(c);
                    r.setActiveReopenCount(windowRepository.countByMonthAndEndedAtIsNull(c.getMonth()));
                    return r;
                })
                .toList();
    }

    @Transactional
    public ConfigResponse updateConfig(Integer id, UpdateConfigRequest request, Long userId) {
        SalesForecastConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Forecast config not found with id: " + id));

        if (request.getAutoCloseAt() != null) {
            config.setAutoCloseAt(parseDateTime(request.getAutoCloseAt()));
        }

        boolean newlyClosed = false;
        if (request.getIsClosed() != null) {
            boolean wasClosed = Boolean.TRUE.equals(config.getIsClosed());
            boolean nowClosed = request.getIsClosed();
            if (!wasClosed && nowClosed) {
                config.setIsClosed(true);
                config.setClosedAt(LocalDateTime.now());
                newlyClosed = true;
            } else if (wasClosed && !nowClosed) {
                throw new IllegalArgumentException("已結束新增的月份無法重新開放");
            }
        }

        config.setUpdatedAt(LocalDateTime.now());
        SalesForecastConfig saved = repository.save(config);

        if (newlyClosed) {
            formLogService.record(saved.getMonth(), null, FormLogAction.MANUAL_CLOSE, userId, "生管手動結束新增設定");
        }
        if (Boolean.TRUE.equals(saved.getIsClosed())) {
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }
        return ConfigResponse.fromEntity(saved);
    }

    /** 排程每分鐘呼叫：關閉截止時間（台灣時間）已到的月份 */
    @Transactional
    public int autoCloseDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<SalesForecastConfig> due = repository.findByIsClosedFalseAndAutoCloseAtLessThanEqual(now);
        int closedCount = 0;
        for (SalesForecastConfig config : due) {
            config.setIsClosed(true);
            // closed_at 沿用既有寫法（前端以 UTC 轉台灣時間顯示）
            config.setClosedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            SalesForecastConfig saved = repository.save(config);
            closedCount++;
            formLogService.record(saved.getMonth(), null, FormLogAction.AUTO_CLOSE, null,
                    "到達截止時間 " + saved.getAutoCloseAt().format(DISPLAY));
            log.info("Auto-closed month {} (auto_close_at={})", saved.getMonth(), saved.getAutoCloseAt());
            try {
                formSummaryService.createFormVersion1Snapshot(saved.getMonth(), saved);
            } catch (Exception ex) {
                log.warn("Failed to create form version 1 snapshot for month {}: {}", saved.getMonth(), ex.getMessage());
            }
        }
        return closedCount;
    }

    public static LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("autoCloseAt format must be YYYY-MM-DDTHH:mm");
        }
    }

    private YearMonth parseMonth(String monthStr) {
        try {
            int year = Integer.parseInt(monthStr.substring(0, 4));
            int month = Integer.parseInt(monthStr.substring(4, 6));
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month format: " + monthStr
                    + ". Expected YYYYMM");
        }
    }

    public static class DuplicateMonthException extends RuntimeException {
        public DuplicateMonthException(String message) {
            super(message);
        }
    }
}
```

- [ ] **Step 6: Controller 改用 autoCloseAt 並傳入操作者**

`SalesForecastConfigController.java`：
- import 增加：
```java
import com.sinker.app.security.JwtUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDateTime;
```
並刪除 `import java.time.LocalDate;`。
- `createMonth` 方法本體改為：
```java
        log.info("POST /api/sales-forecast/config - Creating month {}, autoCloseAt={}",
                request.getMonth(), request.getAutoCloseAt());
        LocalDateTime autoCloseAt = SalesForecastConfigService.parseDateTime(request.getAutoCloseAt());
        ConfigResponse response = service.createMonth(request.getMonth(), autoCloseAt);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
```
- `updateConfig` 方法改為：
```java
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales_forecast_config.edit')")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable Integer id,
            @RequestBody UpdateConfigRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("PUT /api/sales-forecast/config/{} - Updating config", id);
        ConfigResponse response = service.updateConfig(id, request, principal.getUserId());
        return ResponseEntity.ok(response);
    }
```

- [ ] **Step 7: 整檔取代 AutoCloseScheduler 與其測試**

```java
package com.sinker.app.scheduler;

import com.sinker.app.service.SalesForecastConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseScheduler.class);

    private final SalesForecastConfigService service;

    public AutoCloseScheduler(SalesForecastConfigService service) {
        this.service = service;
    }

    /** 每分鐘：自動截止到期的月份 */
    @Scheduled(cron = "0 * * * * ?")
    public void runEveryMinute() {
        try {
            int closed = service.autoCloseDue();
            if (closed > 0) {
                log.info("AutoCloseScheduler - {} month(s) auto-closed", closed);
            }
        } catch (Exception e) {
            log.error("AutoCloseScheduler - 自動截止失敗", e);
        }
    }
}
```

`AutoCloseSchedulerTest.java`：
```java
package com.sinker.app.scheduler;

import com.sinker.app.service.SalesForecastConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCloseSchedulerTest {

    @Mock
    private SalesForecastConfigService service;

    @InjectMocks
    private AutoCloseScheduler scheduler;

    @Test
    void runEveryMinute_invokesAutoClose() {
        when(service.autoCloseDue()).thenReturn(2);
        scheduler.runEveryMinute();
        verify(service).autoCloseDue();
    }

    @Test
    void runEveryMinute_swallowsException() {
        when(service.autoCloseDue()).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
    }
}
```

- [ ] **Step 8: 更新 SalesForecastConfigControllerIntegrationTest**

在該檔執行以下取代（WSL 內）：
```bash
F=backend/src/test/java/com/sinker/app/controller/SalesForecastConfigControllerIntegrationTest.java
sed -i 's/setAutoCloseDate("\([0-9-]*\)")/setAutoCloseAt("\1T00:00")/g' "$F"
sed -i 's/setAutoCloseDate("2099\/01\/25")/setAutoCloseAt("2099\/01\/25")/' "$F"
sed -i 's/\.autoCloseDate")\.value("\([0-9-]*\)")/.autoCloseAt").value("\1T00:00:00")/g' "$F"
sed -i 's/\\"autoCloseDate\\":\\"\([0-9-]*\)\\"/\\"autoCloseAt\\":\\"\1T00:00\\"/g' "$F"
sed -i 's/\\"autoCloseDate\\":\\"2099\/01\/10\\"/\\"autoCloseAt\\":\\"2099\/01\/10\\"/' "$F"
sed -i 's/auto_close_date/auto_close_at/g' "$F"
sed -i 's/containsString("autoCloseDate")/containsString("autoCloseAt")/' "$F"
grep -n "autoCloseDate\|AutoCloseDate\|autoCloseMatchingMonths" "$F"
```
Expected：最後 grep 只剩測試方法名稱（`createMonth_InvalidAutoCloseDateFormat` 等），不影響編譯。

再把 `schedulerIntegration_AutoClosesMatchingMonths` 整個方法換成：
```java
    @Test
    void schedulerIntegration_AutoClosesDueMonths() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_at, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2000-01-01 00:00:00', false, NOW(), NOW())");

        int closedCount = service.autoCloseDue();

        assertTrue(closedCount >= 1);
        Boolean isClosed = jdbc.queryForObject(
                "SELECT is_closed FROM sales_forecast_config WHERE month = '209901'",
                Boolean.class);
        assertTrue(isClosed);
        Integer logs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_form_log WHERE month = '209901' AND action = 'AUTO_CLOSE'",
                Integer.class);
        assertEquals(1, logs);
    }
```
並在 `setUp()` 的清理加一行：
```java
        jdbc.update("DELETE FROM sales_forecast_form_log WHERE month LIKE '2099%' OR month LIKE '2098%'");
```
最後刪除檔頭 `import java.time.LocalDate;`（若已無使用）。

- [ ] **Step 9: 跑單元測試**

Run: `BE_UNIT "*SalesForecastConfigServiceTest" --tests "*AutoCloseSchedulerTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 10: 跑整合測試**

Run: `BE_IT "*SalesForecastConfigControllerIntegrationTest"`
Expected: `BUILD SUCCESSFUL`。若 `createMonth_InvalidAutoCloseDateFormat` 斷言訊息含 `autoCloseDate`，把斷言字串改為 `autoCloseAt`。

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/resources/db/migration/V36__forecast_config_auto_close_at.sql \
  backend/src/main/java/com/sinker/app/entity/SalesForecastConfig.java \
  backend/src/main/java/com/sinker/app/repository/SalesForecastConfigRepository.java \
  backend/src/main/java/com/sinker/app/dto/forecast/ConfigResponse.java \
  backend/src/main/java/com/sinker/app/dto/forecast/CreateMonthsRequest.java \
  backend/src/main/java/com/sinker/app/dto/forecast/UpdateConfigRequest.java \
  backend/src/main/java/com/sinker/app/service/SalesForecastConfigService.java \
  backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java \
  backend/src/main/java/com/sinker/app/scheduler/AutoCloseScheduler.java \
  backend/src/test/java/com/sinker/app/service/SalesForecastConfigServiceTest.java \
  backend/src/test/java/com/sinker/app/scheduler/AutoCloseSchedulerTest.java \
  backend/src/test/java/com/sinker/app/controller/SalesForecastConfigControllerIntegrationTest.java
git commit -m "feat(F129): 自動結束新增設定改為日期加時間並記錄截止異動"
```

---

### Task 3: 生管直接修正：修改原因必填並記錄（R4-3 後端）

**Files:**
- Modify: `backend/src/main/java/com/sinker/app/dto/forecast/SaveFormSummaryVersionRequest.java`
- Modify: `backend/src/main/java/com/sinker/app/service/FormSummaryService.java`（建構子、`CHANNEL_ORDER`、`saveFormSummaryVersion`）
- Modify: `backend/src/main/java/com/sinker/app/controller/SalesForecastController.java:147`
- Test: `backend/src/test/java/com/sinker/app/service/FormSummaryServiceTest.java`

**Interfaces:**
- Consumes: `FormLogService.record`
- Produces:
  - `FormSummaryService.CHANNEL_ORDER` 改為 `public static final`
  - `FormSummaryService.saveFormSummaryVersion(String month, SaveFormSummaryVersionRequest request, Long userId) : int`

- [ ] **Step 1: 寫失敗測試**

`FormSummaryServiceTest.java`：
- 在既有 `@Mock` 欄位區加：
```java
    @Mock
    private FormLogService formLogService;
```
- 第 114、208、340 行的 `service.saveFormSummaryVersion(MONTH, req)` 全部改為 `service.saveFormSummaryVersion(MONTH, req, 1L)`。
- 第 208 行那個測試（成功儲存）在呼叫之後加：
```java
        verify(formLogService).record(eq(MONTH), isNull(),
                eq(com.sinker.app.entity.FormLogAction.CORRECTION), eq(1L), contains("form_v" + versionNo));
```
（若檔頭沒有 `eq`／`isNull`／`contains` 的 static import，加 `import static org.mockito.ArgumentMatchers.*;`）

- [ ] **Step 2: 跑測試確認失敗**

Run: `BE_UNIT "*FormSummaryServiceTest"`
Expected: 編譯失敗（三參數方法不存在）。

- [ ] **Step 3: 實作**

`SaveFormSummaryVersionRequest.java`：類別註解改為「儲存新版本請求：修改原因（必填）+ 各列各通路數量」，`changeReason` 欄位加註解：
```java
    @jakarta.validation.constraints.NotBlank(message = "change_reason is required")
    @JsonProperty("change_reason")
    private String changeReason;
```

`FormSummaryService.java`：
- `private static final List<String> CHANNEL_ORDER` 改 `public static final List<String> CHANNEL_ORDER`。
- 加欄位 `private final FormLogService formLogService;`，建構子最後加參數 `FormLogService formLogService` 並賦值。
- `public int saveFormSummaryVersion(String month, SaveFormSummaryVersionRequest request)` 簽名改為 `(String month, SaveFormSummaryVersionRequest request, Long userId)`；在 `log.info("Saved form summary version {} ...` 之後、`return nextNo;` 之前加：
```java
        formLogService.record(month, null, FormLogAction.CORRECTION, userId,
                "產生版本 " + versionLabel + "；原因：" + request.getChangeReason());
```
並加 `import com.sinker.app.entity.FormLogAction;`。

`SalesForecastController.java:147` 改為：
```java
        int versionNo = formSummaryService.saveFormSummaryVersion(month, request, principal.getUserId());
```

- [ ] **Step 4: 跑測試確認通過**

Run: `BE_UNIT "*FormSummaryServiceTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/sinker/app/dto/forecast/SaveFormSummaryVersionRequest.java \
  backend/src/main/java/com/sinker/app/service/FormSummaryService.java \
  backend/src/main/java/com/sinker/app/controller/SalesForecastController.java \
  backend/src/test/java/com/sinker/app/service/FormSummaryServiceTest.java
git commit -m "feat(F129): 生管直接修正改為修改原因必填並寫入異動紀錄"
```

---

### Task 4: 業務上傳／編輯的統一檢查，上傳不再刪除生管版本

**Files:**
- Create: `backend/src/main/java/com/sinker/app/service/ForecastEditGuard.java`
- Test: `backend/src/test/java/com/sinker/app/service/ForecastEditGuardTest.java`
- Modify: `backend/src/main/java/com/sinker/app/repository/SalesForecastRepository.java`
- Modify: `backend/src/main/java/com/sinker/app/service/SalesForecastUploadService.java`（約第 79、126 行）
- Modify: `backend/src/main/java/com/sinker/app/service/GiftSalesForecastUploadService.java`（約第 74 行）
- Modify: `backend/src/main/java/com/sinker/app/service/SalesForecastService.java`（第 74、130、157、174–181 行）
- Modify: `backend/src/main/java/com/sinker/app/service/GiftSalesForecastService.java`（第 65、112、128、139–145 行）
- Test: `SalesForecastUploadServiceTest.java`、`GiftSalesForecastUploadServiceTest.java`、`SalesForecastServiceTest.java`、`GiftSalesForecastServiceTest.java`

**Interfaces:**
- Consumes: `SalesForecastReopenWindowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter`、`Clock`
- Produces:
  - `ForecastEditGuard.activeWindow(String month, String channel) : Optional<SalesForecastReopenWindow>`
  - `ForecastEditGuard.status(SalesForecastConfig config, String channel) : ForecastEditGuard.Status`（`OPEN`／`CLOSED`／`REOPENED`）
  - `ForecastEditGuard.assertBusinessEditable(SalesForecastConfig config, String channel)`（不可編輯丟 `AccessDeniedException`）
  - `SalesForecastRepository.deleteBusinessRowsByMonthAndChannel(String month, String channel)`
  - `SalesForecastRepository.findDistinctBusinessVersionsByMonthAndChannel(String month, String channel) : List<String>`

- [ ] **Step 1: 寫失敗測試 ForecastEditGuardTest**

```java
package com.sinker.app.service;

import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastEditGuardTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastReopenWindowRepository windowRepository;

    private ForecastEditGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ForecastEditGuard(windowRepository, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setMonth("202612");
        c.setIsClosed(closed);
        return c;
    }

    @Test
    void open_isEditable() {
        assertDoesNotThrow(() -> guard.assertBusinessEditable(config(false), "家樂福"));
        assertEquals(ForecastEditGuard.Status.OPEN, guard.status(config(false), "家樂福"));
        verifyNoInteractions(windowRepository);
    }

    @Test
    void closedWithoutWindow_throws() {
        when(windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter("202612", "家樂福", NOW))
                .thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> guard.assertBusinessEditable(config(true), "家樂福"));
        assertEquals(ForecastEditGuard.Status.CLOSED, guard.status(config(true), "家樂福"));
    }

    @Test
    void closedWithActiveWindow_isEditable() {
        SalesForecastReopenWindow w = new SalesForecastReopenWindow();
        w.setExpiresAt(NOW.plusHours(1));
        when(windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter("202612", "家樂福", NOW))
                .thenReturn(Optional.of(w));
        assertDoesNotThrow(() -> guard.assertBusinessEditable(config(true), "家樂福"));
        assertEquals(ForecastEditGuard.Status.REOPENED, guard.status(config(true), "家樂福"));
    }
}
```

（「補登已到期」與「他通路補登中」由 repository 查詢條件 `ExpiresAtAfter(now)` 與 `channel` 參數保證；Task 8 整合測試會實際驗證。）

- [ ] **Step 2: 跑測試確認失敗**

Run: `BE_UNIT "*ForecastEditGuardTest"`
Expected: 編譯失敗。

- [ ] **Step 3: 實作 ForecastEditGuard**

```java
package com.sinker.app.service;

import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/** 業務上傳／編輯前的統一檢查：月份開放中，或該通路有未到期的補登 */
@Service
public class ForecastEditGuard {

    public enum Status { OPEN, CLOSED, REOPENED }

    private final SalesForecastReopenWindowRepository windowRepository;
    private final Clock clock;

    public ForecastEditGuard(SalesForecastReopenWindowRepository windowRepository, Clock clock) {
        this.windowRepository = windowRepository;
        this.clock = clock;
    }

    public Optional<SalesForecastReopenWindow> activeWindow(String month, String channel) {
        return windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter(
                month, channel, LocalDateTime.now(clock));
    }

    public Status status(SalesForecastConfig config, String channel) {
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            return Status.OPEN;
        }
        return activeWindow(config.getMonth(), channel).isPresent() ? Status.REOPENED : Status.CLOSED;
    }

    public void assertBusinessEditable(SalesForecastConfig config, String channel) {
        if (status(config, channel) == Status.CLOSED) {
            throw new AccessDeniedException("表單已截止（" + config.getMonth() + "）");
        }
    }
}
```

- [ ] **Step 4: Repository 新增方法**

`SalesForecastRepository.java`，在 `deleteByMonthAndChannelAndVersion` 之後加：
```java
    /** 只刪業務上傳資料，保留生管表單版本（form_v*） */
    @Modifying
    @Query("DELETE FROM SalesForecast sf WHERE sf.month = :month AND sf.channel = :channel " +
           "AND sf.version NOT LIKE 'form_v%'")
    void deleteBusinessRowsByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);

    /** 業務版本（排除 form_v*），新到舊 */
    @Query("SELECT DISTINCT sf.version FROM SalesForecast sf " +
           "WHERE sf.month = :month AND sf.channel = :channel AND sf.version NOT LIKE 'form_v%' " +
           "ORDER BY sf.version DESC")
    List<String> findDistinctBusinessVersionsByMonthAndChannel(@Param("month") String month, @Param("channel") String channel);
```

- [ ] **Step 5: 套用到 4 個服務**

`SalesForecastUploadService.java`：
- 欄位與建構子最後加 `ForecastEditGuard editGuard`（`private final ForecastEditGuard editGuard;`，建構子賦值）。
- 把
```java
        if (Boolean.TRUE.equals(config.getIsClosed())) {
            throw new AccessDeniedException("Month " + month + " is closed");
        }
```
改為
```java
        editGuard.assertBusinessEditable(config, channel);
```
- `forecastRepository.deleteByMonthAndChannel(month, channel);` 改為 `forecastRepository.deleteBusinessRowsByMonthAndChannel(month, channel);`

`GiftSalesForecastUploadService.java`：同樣加 `editGuard` 建構子參數（最後一個），並把同一段 `isClosed` 檢查改為 `editGuard.assertBusinessEditable(config, channel);`（禮品表沒有 form_v 資料，刪除不用改）。

`SalesForecastService.java` 與 `GiftSalesForecastService.java`（兩檔做相同修改）：
- 建構子最後加參數 `ForecastEditGuard editGuard` 與欄位。
- `validateMonthOpen(String month)` 改為：
```java
    private void validateMonthOpen(String month, String channel) {
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));
        editGuard.assertBusinessEditable(config, channel);
    }
```
- 呼叫處：`validateMonthOpen(request.getMonth())` → `validateMonthOpen(request.getMonth(), request.getChannel())`；`validateMonthOpen(forecast.getMonth())` → `validateMonthOpen(forecast.getMonth(), forecast.getChannel())`（各檔 3 處）。
- 若 `AccessDeniedException` import 已不再使用則刪除。

- [ ] **Step 6: 更新既有測試**

`SalesForecastUploadServiceTest.java` 與 `GiftSalesForecastUploadServiceTest.java`：
- 加 `@Mock private ForecastEditGuard editGuard;`，`new ...UploadService(...)` 最後加 `editGuard`。
- 用 `closedConfig()`（或 `setIsClosed(true)`）且預期 `AccessDeniedException` 的測試，在呼叫 `upload` 前加：
```java
        doThrow(new org.springframework.security.access.AccessDeniedException("表單已截止"))
                .when(editGuard).assertBusinessEditable(any(), anyString());
```
- `SalesForecastUploadServiceTest` 第 113、237 行 `deleteByMonthAndChannel(MONTH, CHANNEL)` 改為 `deleteBusinessRowsByMonthAndChannel(MONTH, CHANNEL)`。

`SalesForecastServiceTest.java`（`@InjectMocks`）與 `GiftSalesForecastServiceTest.java`（`@InjectMocks`）：
- 加 `@Mock private ForecastEditGuard editGuard;`
- `createForecast_monthClosed`（Sales）與 `deleteForecast_closedMonth_throws`（Gift），以及其他使用已結束 config 並預期 `AccessDeniedException` 的測試（`grep -n "AccessDeniedException" <檔>` 找出），在 act 前加同上的 `doThrow(...)` stub。

- [ ] **Step 7: 跑測試**

Run: `BE_UNIT "*ForecastEditGuardTest" --tests "*SalesForecastUploadServiceTest" --tests "*GiftSalesForecastUploadServiceTest" --tests "*SalesForecastServiceTest" --tests "*GiftSalesForecastServiceTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/sinker/app/service/ForecastEditGuard.java \
  backend/src/main/java/com/sinker/app/repository/SalesForecastRepository.java \
  backend/src/main/java/com/sinker/app/service/SalesForecastUploadService.java \
  backend/src/main/java/com/sinker/app/service/GiftSalesForecastUploadService.java \
  backend/src/main/java/com/sinker/app/service/SalesForecastService.java \
  backend/src/main/java/com/sinker/app/service/GiftSalesForecastService.java \
  backend/src/test/java/com/sinker/app/service/ForecastEditGuardTest.java \
  backend/src/test/java/com/sinker/app/service/SalesForecastUploadServiceTest.java \
  backend/src/test/java/com/sinker/app/service/GiftSalesForecastUploadServiceTest.java \
  backend/src/test/java/com/sinker/app/service/SalesForecastServiceTest.java \
  backend/src/test/java/com/sinker/app/service/GiftSalesForecastServiceTest.java
git commit -m "feat(F129): 業務上傳與編輯改走統一截止檢查（含補登），上傳不刪生管版本"
```

---

### Task 5: 開放補登（R4-4：建立補登期間）

**Files:**
- Create: `backend/src/main/java/com/sinker/app/util/WorkingDays.java`
- Create: `backend/src/main/java/com/sinker/app/dto/forecast/ReopenRequest.java`
- Create: `backend/src/main/java/com/sinker/app/service/ReopenService.java`
- Modify: `backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java`
- Test: `backend/src/test/java/com/sinker/app/util/WorkingDaysTest.java`
- Test: `backend/src/test/java/com/sinker/app/service/ReopenServiceTest.java`

**Interfaces:**
- Consumes: `SalesForecastConfigRepository.findById`、`SalesForecastReopenWindowRepository.existsByMonthAndChannelAndEndedAtIsNull`、`FormLogService.record`、`FormSummaryService.CHANNEL_ORDER`、`SalesForecastConfigService.parseDateTime`
- Produces:
  - `WorkingDays.addWorkingDays(LocalDateTime start, int days) : LocalDateTime`（跳過週六日，保留時分）
  - `ReopenService.MAX_WORKING_DAYS = 3`
  - `ReopenService.open(Integer configId, ReopenRequest request, Long userId)`
  - 建構子 `ReopenService(SalesForecastConfigRepository, SalesForecastReopenWindowRepository, FormLogService, FormSummaryService, Clock)`
  - `POST /api/sales-forecast/config/{id}/reopen`（權限 `sales_forecast_config.reopen`，201）
  - JSON：`{ "channels": ["家樂福"], "expiresAt": "2026-12-12T18:00", "reason": "..." }`

- [ ] **Step 1: 寫失敗測試 WorkingDaysTest**

```java
package com.sinker.app.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkingDaysTest {

    @Test
    void monday_plus3_isThursday() {
        assertEquals(LocalDateTime.of(2026, 12, 10, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 7, 10, 0), 3));
    }

    @Test
    void friday_plus3_skipsWeekend() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 11, 10, 0), 3));
    }

    @Test
    void saturday_plus3_isWednesday() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 12, 10, 0), 3));
    }

    @Test
    void sunday_plus3_isWednesday() {
        assertEquals(LocalDateTime.of(2026, 12, 16, 10, 0),
                WorkingDays.addWorkingDays(LocalDateTime.of(2026, 12, 13, 10, 0), 3));
    }
}
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `BE_UNIT "*WorkingDaysTest"`
Expected: 編譯失敗。

- [ ] **Step 3: 實作 WorkingDays**

```java
package com.sinker.app.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class WorkingDays {

    private WorkingDays() {
    }

    /**
     * 從 start 起算第 days 個工作天的同一時刻（只跳過週六日）。
     * ponytail: 不含國定假日，需要時再加假日表
     */
    public static LocalDateTime addWorkingDays(LocalDateTime start, int days) {
        LocalDateTime t = start;
        int added = 0;
        while (added < days) {
            t = t.plusDays(1);
            DayOfWeek d = t.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return t;
    }
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `BE_UNIT "*WorkingDaysTest"`
Expected: 4 passed。

- [ ] **Step 5: ReopenRequest DTO**

```java
package com.sinker.app.dto.forecast;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 開放補登：可複選通路，共用一個補登截止時間（台灣時間）與原因 */
public class ReopenRequest {

    @NotEmpty(message = "channels is required")
    private List<String> channels;

    @NotBlank(message = "expiresAt is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$",
            message = "expiresAt format must be YYYY-MM-DDTHH:mm")
    private String expiresAt;

    @NotBlank(message = "reason is required")
    @Size(max = 200, message = "reason must be at most 200 characters")
    private String reason;

    public List<String> getChannels() { return channels; }
    public void setChannels(List<String> channels) { this.channels = channels; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

- [ ] **Step 6: 寫失敗測試 ReopenServiceTest（開放部分）**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.ReopenRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReopenServiceTest {

    // 2026-12-10（週四）08:00 台灣時間；3 個工作天上限 = 2026-12-15（週二）08:00
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastConfigRepository configRepository;
    @Mock private SalesForecastReopenWindowRepository windowRepository;
    @Mock private FormLogService formLogService;
    @Mock private FormSummaryService formSummaryService;

    private ReopenService service;

    @BeforeEach
    void setUp() {
        service = new ReopenService(configRepository, windowRepository, formLogService, formSummaryService, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(1);
        c.setMonth("202612");
        c.setIsClosed(closed);
        return c;
    }

    private static ReopenRequest request(String expiresAt, String... channels) {
        ReopenRequest r = new ReopenRequest();
        r.setChannels(List.of(channels));
        r.setExpiresAt(expiresAt);
        r.setReason("全聯新品提案追加");
        return r;
    }

    @Test
    void open_notClosed_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(false)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "家樂福"), 7L));
        assertTrue(ex.getMessage().contains("尚未結束新增"));
    }

    @Test
    void open_expiresNotInFuture_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-10T08:00", "家樂福"), 7L));
    }

    @Test
    void open_beyondThreeWorkingDays_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-15T08:01", "家樂福"), 7L));
        assertTrue(ex.getMessage().contains("2026/12/15 08:00"));
    }

    @Test
    void open_invalidChannel_throws() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "不存在通路"), 7L));
    }

    @Test
    void open_channelAlreadyReopened_throwsAndSavesNothing() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        when(windowRepository.existsByMonthAndChannelAndEndedAtIsNull("202612", "家樂福")).thenReturn(false);
        when(windowRepository.existsByMonthAndChannelAndEndedAtIsNull("202612", "愛買")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.open(1, request("2026-12-12T18:00", "家樂福", "愛買"), 7L));

        assertTrue(ex.getMessage().contains("愛買"));
        verify(windowRepository, never()).save(any());
        verifyNoInteractions(formLogService);
    }

    @Test
    void open_success_createsWindowPerChannelAndLogs() {
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));

        service.open(1, request("2026-12-15T08:00", "家樂福", "愛買"), 7L);

        ArgumentCaptor<SalesForecastReopenWindow> captor = ArgumentCaptor.forClass(SalesForecastReopenWindow.class);
        verify(windowRepository, times(2)).save(captor.capture());
        SalesForecastReopenWindow first = captor.getAllValues().get(0);
        assertEquals("202612", first.getMonth());
        assertEquals("家樂福", first.getChannel());
        assertEquals(LocalDateTime.of(2026, 12, 15, 8, 0), first.getExpiresAt());
        assertEquals(7L, first.getOpenedBy());
        assertEquals(NOW, first.getOpenedAt());
        assertNull(first.getEndedAt());
        verify(formLogService).record(eq("202612"), eq("家樂福"), eq(FormLogAction.REOPEN), eq(7L), contains("全聯新品提案追加"));
        verify(formLogService).record(eq("202612"), eq("愛買"), eq(FormLogAction.REOPEN), eq(7L), anyString());
    }
}
```

- [ ] **Step 7: 跑測試確認失敗**

Run: `BE_UNIT "*ReopenServiceTest"`
Expected: 編譯失敗。

- [ ] **Step 8: 實作 ReopenService（開放部分）**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.ReopenRequest;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import com.sinker.app.util.WorkingDays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;

/** 開放補登與補登結束 */
@Service
public class ReopenService {

    private static final Logger log = LoggerFactory.getLogger(ReopenService.class);
    public static final int MAX_WORKING_DAYS = 3;
    static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastReopenWindowRepository windowRepository;
    private final FormLogService formLogService;
    private final FormSummaryService formSummaryService;
    private final Clock clock;

    public ReopenService(SalesForecastConfigRepository configRepository,
                         SalesForecastReopenWindowRepository windowRepository,
                         FormLogService formLogService,
                         FormSummaryService formSummaryService,
                         Clock clock) {
        this.configRepository = configRepository;
        this.windowRepository = windowRepository;
        this.formLogService = formLogService;
        this.formSummaryService = formSummaryService;
        this.clock = clock;
    }

    /** 開放補登：任一通路不符規則則整批不建立 */
    @Transactional
    public void open(Integer configId, ReopenRequest request, Long userId) {
        SalesForecastConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + configId));
        if (!Boolean.TRUE.equals(config.getIsClosed())) {
            throw new IllegalArgumentException("表單尚未結束新增，不需開放補登");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = SalesForecastConfigService.parseDateTime(request.getExpiresAt());
        LocalDateTime max = WorkingDays.addWorkingDays(now, MAX_WORKING_DAYS);
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("補登截止時間必須晚於現在");
        }
        if (expiresAt.isAfter(max)) {
            throw new IllegalArgumentException("補登期限不可超過 " + max.format(DISPLAY));
        }

        List<String> channels = List.copyOf(new LinkedHashSet<>(request.getChannels()));
        List<String> invalid = channels.stream().filter(ch -> !FormSummaryService.CHANNEL_ORDER.contains(ch)).toList();
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("無效的通路：" + String.join("、", invalid));
        }
        String month = config.getMonth();
        List<String> busy = channels.stream()
                .filter(ch -> windowRepository.existsByMonthAndChannelAndEndedAtIsNull(month, ch)).toList();
        if (!busy.isEmpty()) {
            throw new IllegalArgumentException(String.join("、", busy) + " 已在補登中，請先結束原本的補登");
        }

        String reason = request.getReason().trim();
        for (String channel : channels) {
            SalesForecastReopenWindow w = new SalesForecastReopenWindow();
            w.setMonth(month);
            w.setChannel(channel);
            w.setExpiresAt(expiresAt);
            w.setReason(reason);
            w.setOpenedBy(userId);
            w.setOpenedAt(now);
            windowRepository.save(w);
            formLogService.record(month, channel, FormLogAction.REOPEN, userId,
                    "期限 " + expiresAt.format(DISPLAY) + "；原因：" + reason);
        }
        log.info("Reopened month {} channels {} until {}", month, channels, expiresAt);
    }
}
```

- [ ] **Step 9: Controller 端點**

`SalesForecastConfigController.java`：
- 欄位與建構子加 `ReopenService reopenService`（建構子第二個參數）。
- import `com.sinker.app.dto.forecast.ReopenRequest`、`com.sinker.app.service.ReopenService`。
- 在 `updateConfig` 之後加：
```java
    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('sales_forecast_config.reopen')")
    public ResponseEntity<Void> reopen(
            @PathVariable Integer id,
            @Valid @RequestBody ReopenRequest request,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("POST /api/sales-forecast/config/{}/reopen - channels={}", id, request.getChannels());
        reopenService.open(id, request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
```

- [ ] **Step 10: 跑測試確認通過**

Run: `BE_UNIT "*ReopenServiceTest" --tests "*WorkingDaysTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/sinker/app/util/WorkingDays.java \
  backend/src/main/java/com/sinker/app/dto/forecast/ReopenRequest.java \
  backend/src/main/java/com/sinker/app/service/ReopenService.java \
  backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java \
  backend/src/test/java/com/sinker/app/util/WorkingDaysTest.java \
  backend/src/test/java/com/sinker/app/service/ReopenServiceTest.java
git commit -m "feat(F129): 開放補登（可複選通路、上限 3 個工作天）"
```

---

### Task 6: 補登結束：合併成新的生管版本、提前結束、到期自動鎖回

**Files:**
- Modify: `backend/src/main/java/com/sinker/app/service/FormSummaryService.java`（新增 `mergeReopenedChannels`、`collectBusinessLatest`）
- Modify: `backend/src/main/java/com/sinker/app/service/ReopenService.java`（新增 `end`、`processExpired`、`finishWindows`）
- Modify: `backend/src/main/java/com/sinker/app/scheduler/AutoCloseScheduler.java`
- Modify: `backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java`
- Test: `FormSummaryServiceTest.java`、`ReopenServiceTest.java`、`AutoCloseSchedulerTest.java`

**Interfaces:**
- Consumes: `SalesForecastRepository.findDistinctBusinessVersionsByMonthAndChannel`（Task 4）
- Produces:
  - `FormSummaryService.mergeReopenedChannels(String month, Set<String> channels, String changeReason) : int`（回傳新版本號）
  - `ReopenService.end(Integer configId, Integer windowId, Long userId)`
  - `ReopenService.processExpired() : int`（處理的補登筆數）
  - `POST /api/sales-forecast/config/{id}/reopen/{windowId}/end`（權限 35，200）

- [ ] **Step 1: 寫失敗測試（FormSummaryServiceTest 加一個測試）**

```java
    @Test
    void mergeReopenedChannels_replacesOnlyReopenedChannelsInNewVersion() {
        SalesForecastConfig cfg = new SalesForecastConfig();
        cfg.setMonth(MONTH);
        cfg.setIsClosed(true);
        when(configRepository.findByMonth(MONTH)).thenReturn(Optional.of(cfg));
        when(formVersionRepository.countByMonth(MONTH)).thenReturn(2L);
        SalesForecastFormVersion v2 = new SalesForecastFormVersion();
        v2.setVersionNo(2);
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(v2));

        SalesForecast plannerRow = new SalesForecast();
        plannerRow.setProductCode("P1");
        plannerRow.setProductName("品1");
        plannerRow.setQuantity(new BigDecimal("5"));
        plannerRow.setRemark("");
        // 其他通路會以不同參數呼叫同一方法（回傳空清單），strict stubs 下需 lenient
        lenient().when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "愛買", "form_v2"))
                .thenReturn(List.of(plannerRow));

        String bizVersion = "2026/12/11 10:00:00(家樂福)";
        lenient().when(forecastRepository.findDistinctBusinessVersionsByMonthAndChannel(MONTH, "家樂福")).thenReturn(List.of(bizVersion));
        SalesForecast bizRow = new SalesForecast();
        bizRow.setProductCode("P1");
        bizRow.setProductName("品1");
        bizRow.setQuantity(new BigDecimal("99"));
        bizRow.setRemark("補登加量");
        lenient().when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "家樂福", bizVersion))
                .thenReturn(List.of(bizRow));

        int versionNo = service.mergeReopenedChannels(MONTH, Set.of("家樂福"), "補登：家樂福／原因");

        assertEquals(3, versionNo);
        ArgumentCaptor<SalesForecastFormVersion> verCaptor = ArgumentCaptor.forClass(SalesForecastFormVersion.class);
        verify(formVersionRepository).save(verCaptor.capture());
        assertEquals(3, verCaptor.getValue().getVersionNo());
        assertEquals("補登：家樂福／原因", verCaptor.getValue().getChangeReason());

        ArgumentCaptor<SalesForecast> rowCaptor = ArgumentCaptor.forClass(SalesForecast.class);
        verify(forecastRepository, times(FormSummaryService.CHANNEL_ORDER.size())).save(rowCaptor.capture());
        Map<String, SalesForecast> byChannel = rowCaptor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(SalesForecast::getChannel, r -> r));
        assertEquals(0, new BigDecimal("99").compareTo(byChannel.get("家樂福").getQuantity()));
        assertEquals("補登加量", byChannel.get("家樂福").getRemark());
        assertEquals(0, new BigDecimal("5").compareTo(byChannel.get("愛買").getQuantity()));
        assertEquals(0, BigDecimal.ZERO.compareTo(byChannel.get("7-11").getQuantity()));
        assertEquals("form_v3", byChannel.get("家樂福").getVersion());
        verify(forecastRepository, never()).deleteByMonthAndChannel(anyString(), anyString());
    }
```
（需要的 import：`SalesForecastFormVersion`、`SalesForecastConfig`、`SalesForecast`、`BigDecimal`、`Set`、`Map`、`Optional`、`ArgumentCaptor`，檔頭缺什麼補什麼。`MONTH` 為該測試類既有常數。）

- [ ] **Step 2: 跑測試確認失敗**

Run: `BE_UNIT "*FormSummaryServiceTest"`
Expected: 編譯失敗（`mergeReopenedChannels` 不存在）。

- [ ] **Step 3: 實作 mergeReopenedChannels**

在 `FormSummaryService.saveFormSummaryVersion` 方法之後加入（需要 `import java.util.Set;`）。不改既有 `createFormVersion1Snapshot`，避免影響既有測試：
```java
    /**
     * 補登結束：以最新生管版本為底，指定通路改用業務最新上傳（銷售＋禮品），存成新版本。
     * 同一次結束的多個通路合併成一個版本。
     */
    @Transactional
    public int mergeReopenedChannels(String month, Set<String> channels, String changeReason) {
        validateMonth(month);
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new IllegalArgumentException("Month config not found: " + month));
        ensureFormVersion1Exists(month, config);
        int latestNo = formVersionRepository.findByMonthOrderByVersionNoDesc(month).get(0).getVersionNo();
        String latestLabel = "form_v" + latestNo;
        int nextNo = latestNo + 1;

        Map<String, FormSummaryRowDTO> keyToRow = new TreeMap<>();
        Map<String, Map<String, String>> remarksByKey = new LinkedHashMap<>();
        List<Map<String, BigDecimal>> qtyByChannel = new ArrayList<>();
        for (String channel : CHANNEL_ORDER) {
            Map<String, BigDecimal> qty = new LinkedHashMap<>();
            if (channels.contains(channel)) {
                collectBusinessLatest(month, channel, keyToRow, remarksByKey, qty);
            } else {
                for (SalesForecast r : forecastRepository
                        .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, latestLabel)) {
                    String pk = productKey(r);
                    keyToRow.putIfAbsent(pk, rowFrom(r));
                    collectRemark(remarksByKey, pk, channel, r.getRemark());
                    qty.put(pk, r.getQuantity());
                }
            }
            qtyByChannel.add(qty);
        }

        LocalDateTime now = LocalDateTime.now();
        SalesForecastFormVersion ver = new SalesForecastFormVersion();
        ver.setMonth(month);
        ver.setVersionNo(nextNo);
        ver.setCreatedAt(now);
        ver.setChangeReason(changeReason);
        formVersionRepository.save(ver);

        String versionLabel = "form_v" + nextNo;
        for (int chIdx = 0; chIdx < CHANNEL_ORDER.size(); chIdx++) {
            String channel = CHANNEL_ORDER.get(chIdx);
            for (Map.Entry<String, FormSummaryRowDTO> e : keyToRow.entrySet()) {
                FormSummaryRowDTO row = e.getValue();
                SalesForecast sf = new SalesForecast();
                sf.setMonth(month);
                sf.setChannel(channel);
                sf.setVersion(versionLabel);
                sf.setFormVersionNo(nextNo);
                sf.setWarehouseLocation(row.getWarehouseLocation());
                sf.setCategory(row.getCategory());
                sf.setSpec(row.getSpec());
                sf.setProductName(row.getProductName());
                sf.setProductCode(row.getProductCode() != null ? row.getProductCode() : "");
                sf.setQuantity(qtyByChannel.get(chIdx).getOrDefault(e.getKey(), BigDecimal.ZERO));
                sf.setRemark(remarksByKey.getOrDefault(e.getKey(), Map.of()).getOrDefault(channel, ""));
                sf.setIsModified(true);
                sf.setCreatedAt(now);
                sf.setUpdatedAt(now);
                forecastRepository.save(sf);
            }
        }
        log.info("Merged reopened channels {} into form version {} for month {}", channels, nextNo, month);
        return nextNo;
    }

    /** 某通路業務最新上傳（排除 form_v*）＋禮品最新版，累加到同一品項 */
    private void collectBusinessLatest(String month, String channel, Map<String, FormSummaryRowDTO> keyToRow,
                                       Map<String, Map<String, String>> remarksByKey, Map<String, BigDecimal> qty) {
        List<String> versions = forecastRepository.findDistinctBusinessVersionsByMonthAndChannel(month, channel);
        if (!versions.isEmpty()) {
            for (SalesForecast r : forecastRepository
                    .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, versions.get(0))) {
                String pk = productKey(r);
                keyToRow.putIfAbsent(pk, rowFrom(r));
                collectRemark(remarksByKey, pk, channel, r.getRemark());
                qty.put(pk, r.getQuantity());
            }
        }
        for (GiftSalesForecast g : giftForecastRepository.findLatestByMonthAndChannel(month, channel)) {
            String pk = productKeyGift(g);
            keyToRow.putIfAbsent(pk, rowFromGift(g));
            collectRemark(remarksByKey, pk, channel, g.getRemark());
            qty.put(pk, qty.getOrDefault(pk, BigDecimal.ZERO).add(g.getQuantity()));
        }
    }
```

- [ ] **Step 4: 跑測試確認通過**

Run: `BE_UNIT "*FormSummaryServiceTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: 寫失敗測試（ReopenServiceTest 加三個測試）**

```java
    private static SalesForecastReopenWindow window(int id, String month, String channel) {
        SalesForecastReopenWindow w = new SalesForecastReopenWindow();
        w.setId(id);
        w.setMonth(month);
        w.setChannel(channel);
        w.setReason("新品追加");
        w.setExpiresAt(NOW.minusMinutes(1));
        return w;
    }

    @Test
    void end_mergesChannelAndMarksWindow() {
        SalesForecastReopenWindow w = window(3, "202612", "家樂福");
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        when(windowRepository.findById(3)).thenReturn(Optional.of(w));
        when(formSummaryService.mergeReopenedChannels(eq("202612"), eq(java.util.Set.of("家樂福")), contains("家樂福")))
                .thenReturn(3);

        service.end(1, 3, 7L);

        assertEquals(NOW, w.getEndedAt());
        assertEquals(7L, w.getEndedBy());
        assertEquals("form_v3", w.getMergedVersion());
        verify(windowRepository).save(w);
        verify(formLogService).record(eq("202612"), eq("家樂福"), eq(FormLogAction.REOPEN_END), eq(7L), contains("form_v3"));
    }

    @Test
    void end_alreadyEnded_throws() {
        SalesForecastReopenWindow w = window(3, "202612", "家樂福");
        w.setEndedAt(NOW.minusHours(1));
        when(configRepository.findById(1)).thenReturn(Optional.of(config(true)));
        when(windowRepository.findById(3)).thenReturn(Optional.of(w));
        assertThrows(IllegalArgumentException.class, () -> service.end(1, 3, 7L));
        verifyNoInteractions(formSummaryService);
    }

    @Test
    void processExpired_groupsSameMonthIntoOneVersion() {
        SalesForecastReopenWindow a = window(1, "202612", "家樂福");
        SalesForecastReopenWindow b = window(2, "202612", "愛買");
        SalesForecastReopenWindow c = window(3, "202701", "全家");
        when(windowRepository.findByEndedAtIsNullAndExpiresAtLessThanEqual(NOW)).thenReturn(List.of(a, b, c));
        when(formSummaryService.mergeReopenedChannels(eq("202612"), eq(java.util.Set.of("家樂福", "愛買")), anyString()))
                .thenReturn(4);
        when(formSummaryService.mergeReopenedChannels(eq("202701"), eq(java.util.Set.of("全家")), anyString()))
                .thenReturn(2);

        int count = service.processExpired();

        assertEquals(3, count);
        assertEquals("form_v4", a.getMergedVersion());
        assertEquals("form_v4", b.getMergedVersion());
        assertEquals("form_v2", c.getMergedVersion());
        assertNull(a.getEndedBy());
        verify(formLogService, times(3)).record(anyString(), anyString(), eq(FormLogAction.REOPEN_END), isNull(), contains("到期自動鎖回"));
    }
```

- [ ] **Step 6: 跑測試確認失敗**

Run: `BE_UNIT "*ReopenServiceTest"`
Expected: 編譯失敗（`end`、`processExpired` 不存在）。

- [ ] **Step 7: 實作 end／processExpired**

`ReopenService.java` 加 import `java.util.ArrayList`、`java.util.LinkedHashMap`、`java.util.Map`、`java.util.Set`、`java.util.stream.Collectors`，並在 `open` 之後加：
```java
    /** 生管提前結束單一通路的補登 */
    @Transactional
    public void end(Integer configId, Integer windowId, Long userId) {
        SalesForecastConfig config = configRepository.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + configId));
        SalesForecastReopenWindow w = windowRepository.findById(windowId)
                .filter(x -> x.getMonth().equals(config.getMonth()))
                .orElseThrow(() -> new ResourceNotFoundException("Reopen window not found with id: " + windowId));
        if (w.getEndedAt() != null) {
            throw new IllegalArgumentException("此補登已結束");
        }
        finishWindows(config.getMonth(), List.of(w), userId);
    }

    /**
     * 排程每分鐘呼叫：結束已到期的補登，同月份合併成一個新版本。
     * ponytail: 全部在同一交易，任一月份合併失敗則整批回滾、下一分鐘重試
     */
    @Transactional
    public int processExpired() {
        List<SalesForecastReopenWindow> expired =
                windowRepository.findByEndedAtIsNullAndExpiresAtLessThanEqual(LocalDateTime.now(clock));
        Map<String, List<SalesForecastReopenWindow>> byMonth = new LinkedHashMap<>();
        for (SalesForecastReopenWindow w : expired) {
            byMonth.computeIfAbsent(w.getMonth(), k -> new ArrayList<>()).add(w);
        }
        byMonth.forEach((month, windows) -> finishWindows(month, windows, null));
        return expired.size();
    }

    private void finishWindows(String month, List<SalesForecastReopenWindow> windows, Long userId) {
        Set<String> channels = windows.stream().map(SalesForecastReopenWindow::getChannel)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        String reasons = windows.stream().map(SalesForecastReopenWindow::getReason).distinct()
                .collect(Collectors.joining("；"));
        int versionNo = formSummaryService.mergeReopenedChannels(month, channels,
                "補登：" + String.join("、", channels) + "／" + reasons);
        String label = "form_v" + versionNo;
        LocalDateTime now = LocalDateTime.now(clock);
        for (SalesForecastReopenWindow w : windows) {
            w.setEndedAt(now);
            w.setEndedBy(userId);
            w.setMergedVersion(label);
            windowRepository.save(w);
            formLogService.record(month, w.getChannel(), FormLogAction.REOPEN_END, userId,
                    (userId == null ? "到期自動鎖回" : "生管提前結束") + "，產生版本 " + label);
        }
    }
```

- [ ] **Step 8: Scheduler 加補登到期，更新測試**

`AutoCloseScheduler.java`：建構子加 `ReopenService reopenService`（欄位 `private final ReopenService reopenService;`），`runEveryMinute()` 在既有 try 區塊之後加：
```java
        try {
            int ended = reopenService.processExpired();
            if (ended > 0) {
                log.info("AutoCloseScheduler - {} reopen window(s) ended", ended);
            }
        } catch (Exception e) {
            log.error("AutoCloseScheduler - 補登到期處理失敗", e);
        }
```
方法註解改為「每分鐘：自動截止到期的月份、結束到期的補登」。

`AutoCloseSchedulerTest.java`：加 `@Mock private ReopenService reopenService;`（import `com.sinker.app.service.ReopenService`），並把兩個測試改為：
```java
    @Test
    void runEveryMinute_invokesAutoCloseAndReopenExpiry() {
        when(service.autoCloseDue()).thenReturn(2);
        when(reopenService.processExpired()).thenReturn(1);
        scheduler.runEveryMinute();
        verify(service).autoCloseDue();
        verify(reopenService).processExpired();
    }

    @Test
    void runEveryMinute_autoCloseFailureDoesNotBlockReopenExpiry() {
        when(service.autoCloseDue()).thenThrow(new RuntimeException("db down"));
        assertDoesNotThrow(() -> scheduler.runEveryMinute());
        verify(reopenService).processExpired();
    }
```

- [ ] **Step 9: Controller 提前結束端點**

```java
    @PostMapping("/{id}/reopen/{windowId}/end")
    @PreAuthorize("hasAuthority('sales_forecast_config.reopen')")
    public ResponseEntity<Void> endReopen(
            @PathVariable Integer id,
            @PathVariable Integer windowId,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        log.info("POST /api/sales-forecast/config/{}/reopen/{}/end", id, windowId);
        reopenService.end(id, windowId, principal.getUserId());
        return ResponseEntity.ok().build();
    }
```

- [ ] **Step 10: 跑測試**

Run: `BE_UNIT "*ReopenServiceTest" --tests "*AutoCloseSchedulerTest" --tests "*FormSummaryServiceTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/sinker/app/service/FormSummaryService.java \
  backend/src/main/java/com/sinker/app/service/ReopenService.java \
  backend/src/main/java/com/sinker/app/scheduler/AutoCloseScheduler.java \
  backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java \
  backend/src/test/java/com/sinker/app/service/FormSummaryServiceTest.java \
  backend/src/test/java/com/sinker/app/service/ReopenServiceTest.java \
  backend/src/test/java/com/sinker/app/scheduler/AutoCloseSchedulerTest.java
git commit -m "feat(F129): 補登結束合併為新生管版本，支援提前結束與到期自動鎖回"
```

---

### Task 7: 查詢 API：表單明細、異動紀錄、業務端編輯狀態（R4-5、R4-2 後端）

**Files:**
- Create: `backend/src/main/java/com/sinker/app/dto/forecast/FormDetailResponse.java`
- Create: `backend/src/main/java/com/sinker/app/dto/forecast/EditStatusResponse.java`
- Create: `backend/src/main/java/com/sinker/app/service/ForecastFormDetailService.java`
- Modify: `backend/src/main/java/com/sinker/app/repository/SalesForecastRepository.java`
- Modify: `backend/src/main/java/com/sinker/app/repository/GiftSalesForecastRepository.java`
- Modify: `backend/src/main/java/com/sinker/app/service/FormSummaryService.java`（新增 `countCorrections`、`latestPlannerQuantities`）
- Modify: `backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java`
- Test: `backend/src/test/java/com/sinker/app/service/ForecastFormDetailServiceTest.java`
- Test: `FormSummaryServiceTest.java`

**Interfaces:**
- Produces（JSON 欄位名與 record 元件名相同）：
  - `GET /api/sales-forecast/config/{id}/detail` → `{ config, closeType("AUTO"|"MANUAL"|null), reopenMaxExpiresAt, channels[{ channel, salesLastUploadAt, giftLastUploadAt, activeWindow{ id, expiresAt, reason }|null, correctionCount }] }`
  - `GET /api/sales-forecast/config/{id}/logs` → `FormLogDTO[]`
  - `GET /api/sales-forecast/config/edit-status?month=&channel=` → `{ status("OPEN"|"CLOSED"|"REOPENED"), autoCloseAt, reopenExpiresAt, reopenReason, plannerQuantities{ 品號: 數量 }|null }`
  - 三者權限皆為 `sales_forecast_config.view`（業務角色已有，見 V19）
  - `FormSummaryService.countCorrections(String month, String channel) : int`
  - `FormSummaryService.latestPlannerQuantities(String month, String channel) : Map<String, BigDecimal>`（無生管版本時回 null）

- [ ] **Step 1: DTO**

`FormDetailResponse.java`:
```java
package com.sinker.app.dto.forecast;

import java.time.LocalDateTime;
import java.util.List;

/** 表單明細：月份狀態、補登上限、各通路上傳／補登／生管修正狀態 */
public record FormDetailResponse(ConfigResponse config, String closeType, LocalDateTime reopenMaxExpiresAt,
                                 List<ChannelStatus> channels) {

    public record ChannelStatus(String channel, LocalDateTime salesLastUploadAt, LocalDateTime giftLastUploadAt,
                                ActiveWindow activeWindow, int correctionCount) {
    }

    public record ActiveWindow(Integer id, LocalDateTime expiresAt, String reason) {
    }
}
```

`EditStatusResponse.java`:
```java
package com.sinker.app.dto.forecast;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/** 業務上傳頁的編輯狀態；plannerQuantities 為最新生管版本中該通路各品號數量（含禮品），未結束時為 null */
public record EditStatusResponse(String status, LocalDateTime autoCloseAt, LocalDateTime reopenExpiresAt,
                                 String reopenReason, Map<String, BigDecimal> plannerQuantities) {
}
```

- [ ] **Step 2: Repository 查詢**

`SalesForecastRepository.java` 加：
```java
    /** 各通路業務最後上傳時間（排除生管 form_v* 版本） */
    @Query("SELECT sf.channel, MAX(sf.createdAt) FROM SalesForecast sf " +
           "WHERE sf.month = :month AND sf.version NOT LIKE 'form_v%' GROUP BY sf.channel")
    List<Object[]> findLastUploadAtByChannel(@Param("month") String month);
```
`GiftSalesForecastRepository.java` 加：
```java
    @Query("SELECT g.channel, MAX(g.createdAt) FROM GiftSalesForecast g WHERE g.month = :month GROUP BY g.channel")
    List<Object[]> findLastUploadAtByChannel(@Param("month") String month);
```

- [ ] **Step 3: 寫失敗測試（FormSummaryServiceTest 加兩個測試）**

```java
    @Test
    void countCorrections_comparesLatestWithFirstVersion() {
        SalesForecastFormVersion v3 = new SalesForecastFormVersion();
        v3.setVersionNo(3);
        SalesForecastFormVersion v1 = new SalesForecastFormVersion();
        v1.setVersionNo(1);
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(v3, v1));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "家樂福", "form_v3"))
                .thenReturn(List.of(qtyRow("P1", "10"), qtyRow("P2", "7")));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "家樂福", "form_v1"))
                .thenReturn(List.of(qtyRow("P1", "10"), qtyRow("P2", "5"), qtyRow("P3", "1")));

        assertEquals(2, service.countCorrections(MONTH, "家樂福"));
    }

    @Test
    void latestPlannerQuantities_byProductCode_nullWhenNoVersion() {
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of());
        assertNull(service.latestPlannerQuantities(MONTH, "家樂福"));

        SalesForecastFormVersion v2 = new SalesForecastFormVersion();
        v2.setVersionNo(2);
        when(formVersionRepository.findByMonthOrderByVersionNoDesc(MONTH)).thenReturn(List.of(v2));
        when(forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(MONTH, "家樂福", "form_v2"))
                .thenReturn(List.of(qtyRow("P1", "150")));

        Map<String, BigDecimal> q = service.latestPlannerQuantities(MONTH, "家樂福");
        assertEquals(0, new BigDecimal("150").compareTo(q.get("P1")));
    }

    private static SalesForecast qtyRow(String code, String qty) {
        SalesForecast r = new SalesForecast();
        r.setProductCode(code);
        r.setQuantity(new BigDecimal(qty));
        return r;
    }
```

- [ ] **Step 4: 實作 countCorrections／latestPlannerQuantities**

在 `FormSummaryService` 加（需要 `import java.util.HashMap;`、`import java.util.HashSet;`）：
```java
    /**
     * 該通路最新生管版本與第一版相比，數量不同的品項數（明細頁與開放補登的覆蓋提醒用）。
     * ponytail: 含先前補登併入的差異，不區分來源
     */
    @Transactional(readOnly = true)
    public int countCorrections(String month, String channel) {
        List<SalesForecastFormVersion> versions = formVersionRepository.findByMonthOrderByVersionNoDesc(month);
        if (versions.size() < 2) return 0;
        Map<String, BigDecimal> latest = qtyByProduct(month, channel, "form_v" + versions.get(0).getVersionNo());
        Map<String, BigDecimal> first = qtyByProduct(month, channel, "form_v1");
        Set<String> keys = new HashSet<>(latest.keySet());
        keys.addAll(first.keySet());
        int count = 0;
        for (String k : keys) {
            if (latest.getOrDefault(k, BigDecimal.ZERO).compareTo(first.getOrDefault(k, BigDecimal.ZERO)) != 0) {
                count++;
            }
        }
        return count;
    }

    /** 最新生管版本中該通路各品號數量；尚無任何生管版本時回 null */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> latestPlannerQuantities(String month, String channel) {
        List<SalesForecastFormVersion> versions = formVersionRepository.findByMonthOrderByVersionNoDesc(month);
        if (versions.isEmpty()) return null;
        return qtyByProduct(month, channel, "form_v" + versions.get(0).getVersionNo());
    }

    private Map<String, BigDecimal> qtyByProduct(String month, String channel, String version) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (SalesForecast r : forecastRepository
                .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, version)) {
            String code = r.getProductCode() != null ? r.getProductCode() : "";
            map.merge(code, r.getQuantity() != null ? r.getQuantity() : BigDecimal.ZERO, BigDecimal::add);
        }
        return map;
    }
```

- [ ] **Step 5: 寫失敗測試 ForecastFormDetailServiceTest**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.EditStatusResponse;
import com.sinker.app.dto.forecast.FormDetailResponse;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.SalesForecastRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForecastFormDetailServiceTest {

    // 2026-12-10（週四）08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));

    @Mock private SalesForecastConfigRepository configRepository;
    @Mock private SalesForecastRepository forecastRepository;
    @Mock private GiftSalesForecastRepository giftForecastRepository;
    @Mock private SalesForecastFormLogRepository formLogRepository;
    @Mock private FormLogService formLogService;
    @Mock private FormSummaryService formSummaryService;
    @Mock private ForecastEditGuard editGuard;

    private ForecastFormDetailService service;

    @BeforeEach
    void setUp() {
        service = new ForecastFormDetailService(configRepository, forecastRepository, giftForecastRepository,
                formLogRepository, formLogService, formSummaryService, editGuard, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setId(7);
        c.setMonth("202612");
        c.setAutoCloseAt(LocalDateTime.of(2026, 12, 10, 0, 0));
        c.setIsClosed(closed);
        return c;
    }

    private static SalesForecastReopenWindow window() {
        SalesForecastReopenWindow w = new SalesForecastReopenWindow();
        w.setId(3);
        w.setExpiresAt(LocalDateTime.of(2026, 12, 12, 18, 0));
        w.setReason("全聯新品提案追加");
        return w;
    }

    @Test
    void getFormDetail_listsChannelStatus() {
        when(configRepository.findById(7)).thenReturn(Optional.of(config(true)));
        LocalDateTime t1 = LocalDateTime.of(2026, 12, 8, 9, 40);
        LocalDateTime t2 = LocalDateTime.of(2026, 12, 9, 3, 2);
        when(forecastRepository.findLastUploadAtByChannel("202612")).thenReturn(List.<Object[]>of(new Object[]{"家樂福", t1}));
        when(giftForecastRepository.findLastUploadAtByChannel("202612")).thenReturn(List.<Object[]>of(new Object[]{"愛買", t2}));
        when(editGuard.activeWindow(anyString(), anyString())).thenReturn(Optional.empty());
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.of(window()));
        when(formSummaryService.countCorrections(anyString(), anyString())).thenReturn(0);
        when(formSummaryService.countCorrections("202612", "家樂福")).thenReturn(2);
        SalesForecastFormLog closeLog = new SalesForecastFormLog();
        closeLog.setAction(FormLogAction.MANUAL_CLOSE);
        when(formLogRepository.findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(eq("202612"), anyCollection()))
                .thenReturn(Optional.of(closeLog));

        FormDetailResponse d = service.getFormDetail(7);

        assertEquals("MANUAL", d.closeType());
        assertEquals(LocalDateTime.of(2026, 12, 15, 8, 0), d.reopenMaxExpiresAt());
        assertEquals(FormSummaryService.CHANNEL_ORDER.size(), d.channels().size());
        FormDetailResponse.ChannelStatus carrefour = d.channels().get(FormSummaryService.CHANNEL_ORDER.indexOf("家樂福"));
        assertEquals(t1, carrefour.salesLastUploadAt());
        assertNull(carrefour.giftLastUploadAt());
        assertEquals(3, carrefour.activeWindow().id());
        assertEquals(2, carrefour.correctionCount());
        FormDetailResponse.ChannelStatus aimai = d.channels().get(FormSummaryService.CHANNEL_ORDER.indexOf("愛買"));
        assertEquals(t2, aimai.giftLastUploadAt());
        assertNull(aimai.activeWindow());
    }

    @Test
    void getFormDetail_notFound() {
        when(configRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getFormDetail(99));
    }

    @Test
    void getEditStatus_closed_returnsPlannerQuantities() {
        SalesForecastConfig c = config(true);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.CLOSED);
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.empty());
        when(formSummaryService.latestPlannerQuantities("202612", "家樂福")).thenReturn(Map.of("P1", new BigDecimal("150")));

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("CLOSED", r.status());
        assertEquals(c.getAutoCloseAt(), r.autoCloseAt());
        assertNull(r.reopenExpiresAt());
        assertEquals(0, new BigDecimal("150").compareTo(r.plannerQuantities().get("P1")));
    }

    @Test
    void getEditStatus_reopened_includesWindow() {
        SalesForecastConfig c = config(true);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.REOPENED);
        when(editGuard.activeWindow("202612", "家樂福")).thenReturn(Optional.of(window()));

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("REOPENED", r.status());
        assertEquals(LocalDateTime.of(2026, 12, 12, 18, 0), r.reopenExpiresAt());
        assertEquals("全聯新品提案追加", r.reopenReason());
    }

    @Test
    void getEditStatus_open_noPlannerQuantities() {
        SalesForecastConfig c = config(false);
        when(configRepository.findByMonth("202612")).thenReturn(Optional.of(c));
        when(editGuard.status(c, "家樂福")).thenReturn(ForecastEditGuard.Status.OPEN);

        EditStatusResponse r = service.getEditStatus("202612", "家樂福");

        assertEquals("OPEN", r.status());
        assertNull(r.plannerQuantities());
        verifyNoInteractions(formSummaryService);
    }
}
```

- [ ] **Step 6: 跑測試確認失敗**

Run: `BE_UNIT "*ForecastFormDetailServiceTest" --tests "*FormSummaryServiceTest"`
Expected: 編譯失敗。

- [ ] **Step 7: 實作 ForecastFormDetailService**

```java
package com.sinker.app.service;

import com.sinker.app.dto.forecast.ConfigResponse;
import com.sinker.app.dto.forecast.EditStatusResponse;
import com.sinker.app.dto.forecast.FormDetailResponse;
import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.util.WorkingDays;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 表單明細頁、異動紀錄、業務端編輯狀態的唯讀查詢 */
@Service
public class ForecastFormDetailService {

    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastRepository forecastRepository;
    private final GiftSalesForecastRepository giftForecastRepository;
    private final SalesForecastFormLogRepository formLogRepository;
    private final FormLogService formLogService;
    private final FormSummaryService formSummaryService;
    private final ForecastEditGuard editGuard;
    private final Clock clock;

    public ForecastFormDetailService(SalesForecastConfigRepository configRepository,
                                     SalesForecastRepository forecastRepository,
                                     GiftSalesForecastRepository giftForecastRepository,
                                     SalesForecastFormLogRepository formLogRepository,
                                     FormLogService formLogService,
                                     FormSummaryService formSummaryService,
                                     ForecastEditGuard editGuard,
                                     Clock clock) {
        this.configRepository = configRepository;
        this.forecastRepository = forecastRepository;
        this.giftForecastRepository = giftForecastRepository;
        this.formLogRepository = formLogRepository;
        this.formLogService = formLogService;
        this.formSummaryService = formSummaryService;
        this.editGuard = editGuard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FormDetailResponse getFormDetail(Integer id) {
        SalesForecastConfig config = findConfig(id);
        String month = config.getMonth();
        boolean closed = Boolean.TRUE.equals(config.getIsClosed());
        Map<String, LocalDateTime> salesAt = toChannelMap(forecastRepository.findLastUploadAtByChannel(month));
        Map<String, LocalDateTime> giftAt = toChannelMap(giftForecastRepository.findLastUploadAtByChannel(month));

        // ponytail: 通路清單沿用 CHANNEL_ORDER，R3 通路主檔上線後改讀主檔
        List<FormDetailResponse.ChannelStatus> channels = FormSummaryService.CHANNEL_ORDER.stream()
                .map(ch -> new FormDetailResponse.ChannelStatus(
                        ch,
                        salesAt.get(ch),
                        giftAt.get(ch),
                        editGuard.activeWindow(month, ch)
                                .map(w -> new FormDetailResponse.ActiveWindow(w.getId(), w.getExpiresAt(), w.getReason()))
                                .orElse(null),
                        closed ? formSummaryService.countCorrections(month, ch) : 0))
                .toList();

        String closeType = formLogRepository
                .findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(month,
                        List.of(FormLogAction.AUTO_CLOSE, FormLogAction.MANUAL_CLOSE))
                .map(l -> l.getAction() == FormLogAction.MANUAL_CLOSE ? "MANUAL" : "AUTO")
                .orElse(null);

        return new FormDetailResponse(ConfigResponse.fromEntity(config), closeType,
                WorkingDays.addWorkingDays(LocalDateTime.now(clock), ReopenService.MAX_WORKING_DAYS), channels);
    }

    @Transactional(readOnly = true)
    public List<FormLogDTO> getLogs(Integer id) {
        return formLogService.list(findConfig(id).getMonth());
    }

    @Transactional(readOnly = true)
    public EditStatusResponse getEditStatus(String month, String channel) {
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));
        ForecastEditGuard.Status status = editGuard.status(config, channel);
        if (status == ForecastEditGuard.Status.OPEN) {
            return new EditStatusResponse(status.name(), config.getAutoCloseAt(), null, null, null);
        }
        Optional<SalesForecastReopenWindow> window = editGuard.activeWindow(month, channel);
        Map<String, BigDecimal> planner = formSummaryService.latestPlannerQuantities(month, channel);
        return new EditStatusResponse(status.name(), config.getAutoCloseAt(),
                window.map(SalesForecastReopenWindow::getExpiresAt).orElse(null),
                window.map(SalesForecastReopenWindow::getReason).orElse(null),
                planner);
    }

    private SalesForecastConfig findConfig(Integer id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast config not found with id: " + id));
    }

    private static Map<String, LocalDateTime> toChannelMap(List<Object[]> rows) {
        Map<String, LocalDateTime> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put((String) r[0], (LocalDateTime) r[1]);
        }
        return map;
    }
}
```

- [ ] **Step 8: Controller 端點**

`SalesForecastConfigController.java`：建構子加第三個參數 `ForecastFormDetailService detailService`（欄位同名），import `ForecastFormDetailService`、`FormDetailResponse`、`FormLogDTO`、`EditStatusResponse`，以及 `org.springframework.web.bind.annotation.RequestParam`（若類別用 `annotation.*` 則免）。加：
```java
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<FormDetailResponse> getFormDetail(@PathVariable Integer id) {
        log.info("GET /api/sales-forecast/config/{}/detail", id);
        return ResponseEntity.ok(detailService.getFormDetail(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<List<FormLogDTO>> getFormLogs(@PathVariable Integer id) {
        log.info("GET /api/sales-forecast/config/{}/logs", id);
        return ResponseEntity.ok(detailService.getLogs(id));
    }

    @GetMapping("/edit-status")
    @PreAuthorize("hasAuthority('sales_forecast_config.view')")
    public ResponseEntity<EditStatusResponse> getEditStatus(@RequestParam String month, @RequestParam String channel) {
        log.info("GET /api/sales-forecast/config/edit-status - month={}, channel={}", month, channel);
        return ResponseEntity.ok(detailService.getEditStatus(month, channel));
    }
```

- [ ] **Step 9: 跑測試確認通過**

Run: `BE_UNIT "*ForecastFormDetailServiceTest" --tests "*FormSummaryServiceTest"`
Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/sinker/app/dto/forecast/FormDetailResponse.java \
  backend/src/main/java/com/sinker/app/dto/forecast/EditStatusResponse.java \
  backend/src/main/java/com/sinker/app/service/ForecastFormDetailService.java \
  backend/src/main/java/com/sinker/app/repository/SalesForecastRepository.java \
  backend/src/main/java/com/sinker/app/repository/GiftSalesForecastRepository.java \
  backend/src/main/java/com/sinker/app/service/FormSummaryService.java \
  backend/src/main/java/com/sinker/app/controller/SalesForecastConfigController.java \
  backend/src/test/java/com/sinker/app/service/ForecastFormDetailServiceTest.java \
  backend/src/test/java/com/sinker/app/service/FormSummaryServiceTest.java
git commit -m "feat(F129): 表單明細、異動紀錄與業務端編輯狀態查詢 API"
```

---

### Task 8: 後端整合測試：截止 → 開放補登 → 補登 → 提前結束

**Files:**
- Create: `backend/src/test/java/com/sinker/app/controller/ReopenFlowIntegrationTest.java`

- [ ] **Step 1: 寫整合測試**

```java
package com.sinker.app.controller;

import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.ForecastEditGuard;
import com.sinker.app.service.FormSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReopenFlowIntegrationTest {

    private static final String MONTH = "209811";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SalesForecastConfigRepository configRepository;
    @Autowired private SalesForecastRepository forecastRepository;
    @Autowired private FormSummaryService formSummaryService;
    @Autowired private ForecastEditGuard editGuard;

    private String adminToken;
    private Integer configId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_form_version WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_reopen_window WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_form_log WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);

        Long adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");

        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_at, is_closed, closed_at, created_at, updated_at) " +
                "VALUES (?, '2098-11-01 00:00:00', true, NOW(), NOW(), NOW())", MONTH);
        configId = jdbc.queryForObject("SELECT id FROM sales_forecast_config WHERE month = ?", Integer.class, MONTH);

        SalesForecast biz = new SalesForecast();
        biz.setMonth(MONTH);
        biz.setChannel("家樂福");
        biz.setVersion("2098/10/30 10:00:00(家樂福)");
        biz.setCategory("果凍");
        biz.setSpec("1000g");
        biz.setProductName("蒟蒻果凍");
        biz.setProductCode("P1");
        biz.setWarehouseLocation("C425");
        biz.setQuantity(new BigDecimal("10"));
        biz.setRemark("");
        biz.setIsModified(false);
        biz.setCreatedAt(LocalDateTime.now());
        biz.setUpdatedAt(LocalDateTime.now());
        forecastRepository.save(biz);

        SalesForecastConfig config = configRepository.findByMonth(MONTH).orElseThrow();
        formSummaryService.createFormVersion1Snapshot(MONTH, config);
    }

    @Test
    void reopen_upload_end_createsNewFormVersionAndLogs() throws Exception {
        String expiresAt = LocalDateTime.now(ZoneId.of("Asia/Taipei")).plusHours(2)
                .withSecond(0).withNano(0).toString();

        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"" + expiresAt + "\",\"reason\":\"新品追加\"}"))
                .andExpect(status().isCreated());

        // 重複開放同一通路 → 400
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"" + expiresAt + "\",\"reason\":\"再一次\"}"))
                .andExpect(status().isBadRequest());

        SalesForecastConfig config = configRepository.findByMonth(MONTH).orElseThrow();
        assertDoesNotThrow(() -> editGuard.assertBusinessEditable(config, "家樂福"));
        assertThrows(AccessDeniedException.class, () -> editGuard.assertBusinessEditable(config, "愛買"));

        mockMvc.perform(get("/api/sales-forecast/config/" + configId + "/detail")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[?(@.channel=='家樂福')].activeWindow.reason").value("新品追加"));

        // 模擬業務補登：業務層數量改為 99
        jdbc.update("UPDATE sales_forecast SET quantity = 99 WHERE month = ? AND channel = '家樂福' " +
                "AND version NOT LIKE 'form_v%'", MONTH);

        Integer windowId = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_reopen_window WHERE month = ? AND channel = '家樂福'", Integer.class, MONTH);
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen/" + windowId + "/end")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        BigDecimal merged = jdbc.queryForObject(
                "SELECT quantity FROM sales_forecast WHERE month = ? AND channel = '家樂福' " +
                        "AND version = 'form_v2' AND product_code = 'P1'", BigDecimal.class, MONTH);
        assertEquals(0, new BigDecimal("99").compareTo(merged));
        assertEquals("form_v2", jdbc.queryForObject(
                "SELECT merged_version FROM sales_forecast_reopen_window WHERE id = ?", String.class, windowId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_form_log WHERE month = ? AND action = 'REOPEN'", Integer.class, MONTH));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_form_log WHERE month = ? AND action = 'REOPEN_END'", Integer.class, MONTH));
        assertThrows(AccessDeniedException.class, () -> editGuard.assertBusinessEditable(config, "家樂福"));

        mockMvc.perform(get("/api/sales-forecast/config/" + configId + "/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REOPEN_END"));
    }

    @Test
    void reopen_withoutPermission_forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"2098-11-02T10:00\",\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveFormVersion_blankReason_badRequest() throws Exception {
        mockMvc.perform(post("/api/sales-forecast/form-summary/save-version")
                        .param("month", MONTH)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"change_reason\":\"  \",\"rows\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 跑整合測試**

Run: `BE_IT "*ReopenFlowIntegrationTest" --tests "*SalesForecastConfigControllerIntegrationTest"`
Expected: `BUILD SUCCESSFUL`。若 `SalesForecast` 有其他 `nullable = false` 欄位導致 save 失敗，依錯誤訊息補 setter；若 `validateMonth` 拒絕 `209811`，改用 `202812` 並同步替換本檔所有月份字串。

- [ ] **Step 3: 後端全部測試與基線比對**

Run: `BE_UNIT "*"`
Expected: 失敗清單是 Task 0 基線的子集。

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/sinker/app/controller/ReopenFlowIntegrationTest.java
git commit -m "test(F129): 開放補登到提前結束的整合測試"
```

---

### Task 9: 前端基礎：API、時間工具、截止改日期＋時間、列表頁入口、修 13 個過時測試

**Files:**
- Create: `frontend/src/utils/dateTime.js`
- Test: `frontend/src/utils/__tests__/dateTime.test.js`
- Modify (整檔取代): `frontend/src/api/forecastConfig.js`
- Test (整檔取代): `frontend/src/api/__tests__/forecastConfig.test.js`
- Modify: `frontend/src/pages/sales-forecast/CreateMonthsDialog.jsx`
- Modify: `frontend/src/pages/sales-forecast/EditConfigDialog.jsx`
- Test (整檔取代): `frontend/src/pages/sales-forecast/__tests__/CreateMonthsDialog.test.jsx`
- Test (整檔取代): `frontend/src/pages/sales-forecast/__tests__/EditConfigDialog.test.jsx`
- Modify: `frontend/src/pages/sales-forecast/ForecastConfigPage.jsx`
- Modify: `frontend/src/pages/sales-forecast/ForecastConfig.css`
- Test: `frontend/src/pages/sales-forecast/__tests__/ForecastConfigPage.test.jsx`
- Modify (整檔取代): `frontend/src/components/SalesForecastFillDeadlineBanner.jsx`

**Interfaces:**
- Produces:
  - `formatTaipeiDateTime(v)`：既有 UTC／無時區欄位 → 台灣時間 `YYYY/MM/DD HH:mm:ss`
  - `formatLocalDateTime(v)`：台灣時間欄位原樣 → `YYYY/MM/DD HH:mm`；空值回 `-`
  - `toDateTimeLocalValue(v)`：→ `YYYY-MM-DDTHH:mm`；空值回 `''`
  - `remainingText(expiresAt, now = new Date())`：`剩餘 1 天 6 小時`／`剩餘 3 小時 20 分`／`已到期`
  - API：`listConfigs()`、`createMonth(month, autoCloseAt)`、`updateConfig(id, data)`、`getFormDetail(id)`、`getFormLogs(id)`、`reopenChannels(id, { channels, expiresAt, reason })`、`endReopen(id, windowId)`、`getEditStatus(month, channel)`

- [ ] **Step 1: 寫失敗測試 dateTime.test.js**

```js
import { describe, it, expect } from 'vitest'
import { formatTaipeiDateTime, formatLocalDateTime, toDateTimeLocalValue, remainingText } from '../dateTime'

describe('dateTime utils', () => {
  it('formatTaipeiDateTime 把無時區時間當 UTC 轉台灣時間', () => {
    expect(formatTaipeiDateTime('2026-12-08T09:40:00')).toBe('2026/12/08 17:40:00')
    expect(formatTaipeiDateTime(null)).toBe('-')
  })

  it('formatLocalDateTime 原樣顯示台灣時間', () => {
    expect(formatLocalDateTime('2026-12-10T00:00:00')).toBe('2026/12/10 00:00')
    expect(formatLocalDateTime('2026-12-10T18:30')).toBe('2026/12/10 18:30')
    expect(formatLocalDateTime(null)).toBe('-')
  })

  it('toDateTimeLocalValue 截到分鐘', () => {
    expect(toDateTimeLocalValue('2026-12-10T18:30:00')).toBe('2026-12-10T18:30')
    expect(toDateTimeLocalValue(null)).toBe('')
  })

  it('remainingText 以台灣時間計算剩餘時間', () => {
    const now = new Date('2026-12-11T04:00:00Z') // 台灣 12/11 12:00
    expect(remainingText('2026-12-12T18:00:00', now)).toBe('剩餘 1 天 6 小時')
    expect(remainingText('2026-12-11T15:20:00', now)).toBe('剩餘 3 小時 20 分')
    expect(remainingText('2026-12-11T11:00:00', now)).toBe('已到期')
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `FE_TEST src/utils/__tests__/dateTime.test.js`
Expected: FAIL（找不到模組）。

- [ ] **Step 3: 實作 dateTime.js**

```js
/** 既有時間欄位（closed_at、上傳時間；後端為 UTC 或無時區）轉台灣時間顯示 */
export function formatTaipeiDateTime(dateStr) {
  if (!dateStr) return '-'
  const utcStr = typeof dateStr === 'string' && !/Z|[+-]\d{2}:?\d{2}$/.test(dateStr) ? dateStr + 'Z' : dateStr
  const d = new Date(utcStr)
  if (isNaN(d.getTime())) return String(dateStr)
  return d.toLocaleString('zh-TW', {
    timeZone: 'Asia/Taipei',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

/** 後端以台灣時間儲存的欄位（截止時間、補登期限、異動紀錄）原樣顯示 */
export function formatLocalDateTime(value) {
  if (!value) return '-'
  const m = String(value).match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/)
  return m ? `${m[1]}/${m[2]}/${m[3]} ${m[4]}:${m[5]}` : String(value)
}

/** 轉成 <input type="datetime-local"> 的值 */
export function toDateTimeLocalValue(value) {
  return value ? String(value).slice(0, 16) : ''
}

/** 台灣時間的到期時間距離 now 還剩多久 */
export function remainingText(expiresAt, now = new Date()) {
  if (!expiresAt) return ''
  const end = new Date(`${String(expiresAt).slice(0, 19)}+08:00`)
  const diff = end.getTime() - now.getTime()
  if (isNaN(diff)) return ''
  if (diff <= 0) return '已到期'
  const totalMin = Math.floor(diff / 60000)
  const d = Math.floor(totalMin / 1440)
  const h = Math.floor((totalMin % 1440) / 60)
  const m = totalMin % 60
  return d > 0 ? `剩餘 ${d} 天 ${h} 小時` : `剩餘 ${h} 小時 ${m} 分`
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `FE_TEST src/utils/__tests__/dateTime.test.js`
Expected: 4 passed。

- [ ] **Step 5: API 模組與測試（整檔取代）**

`frontend/src/api/forecastConfig.js`:
```js
import api from './axios'

export function listConfigs() {
  return api.get('/api/sales-forecast/config').then((r) => r.data)
}

export function createMonth(month, autoCloseAt) {
  return api
    .post('/api/sales-forecast/config', { month, autoCloseAt })
    .then((r) => r.data)
}

export function updateConfig(id, data) {
  return api.put(`/api/sales-forecast/config/${id}`, data).then((r) => r.data)
}

export function getFormDetail(id) {
  return api.get(`/api/sales-forecast/config/${id}/detail`).then((r) => r.data)
}

export function getFormLogs(id) {
  return api.get(`/api/sales-forecast/config/${id}/logs`).then((r) => r.data)
}

export function reopenChannels(id, payload) {
  return api.post(`/api/sales-forecast/config/${id}/reopen`, payload).then((r) => r.data)
}

export function endReopen(id, windowId) {
  return api.post(`/api/sales-forecast/config/${id}/reopen/${windowId}/end`).then((r) => r.data)
}

export function getEditStatus(month, channel) {
  return api
    .get('/api/sales-forecast/config/edit-status', { params: { month, channel } })
    .then((r) => r.data)
}
```

`frontend/src/api/__tests__/forecastConfig.test.js`:
```js
import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../axios', () => ({
  default: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
}))

import api from '../axios'
import {
  listConfigs,
  createMonth,
  updateConfig,
  getFormDetail,
  getFormLogs,
  reopenChannels,
  endReopen,
  getEditStatus,
} from '../forecastConfig'

describe('forecastConfig API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.get.mockResolvedValue({ data: 'ok' })
    api.post.mockResolvedValue({ data: 'ok' })
    api.put.mockResolvedValue({ data: 'ok' })
  })

  it('listConfigs', async () => {
    expect(await listConfigs()).toBe('ok')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config')
  })

  it('createMonth 送出月份與截止時間', async () => {
    await createMonth('202612', '2026-12-10T18:30')
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config', { month: '202612', autoCloseAt: '2026-12-10T18:30' })
  })

  it('updateConfig', async () => {
    await updateConfig(1, { autoCloseAt: '2026-12-11T18:00', isClosed: true })
    expect(api.put).toHaveBeenCalledWith('/api/sales-forecast/config/1', { autoCloseAt: '2026-12-11T18:00', isClosed: true })
  })

  it('getFormDetail／getFormLogs', async () => {
    await getFormDetail(7)
    await getFormLogs(7)
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/7/detail')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/7/logs')
  })

  it('reopenChannels／endReopen', async () => {
    const payload = { channels: ['家樂福'], expiresAt: '2026-12-12T18:00', reason: '新品追加' }
    await reopenChannels(7, payload)
    await endReopen(7, 3)
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config/7/reopen', payload)
    expect(api.post).toHaveBeenCalledWith('/api/sales-forecast/config/7/reopen/3/end')
  })

  it('getEditStatus 以 query 帶月份與通路', async () => {
    await getEditStatus('202612', '家樂福')
    expect(api.get).toHaveBeenCalledWith('/api/sales-forecast/config/edit-status', { params: { month: '202612', channel: '家樂福' } })
  })
})
```

- [ ] **Step 6: 兩個對話框改成日期＋時間**

```bash
cd ~/SinkerProject26/frontend/src/pages/sales-forecast
sed -i 's/autoCloseDate/autoCloseAt/g; s/AutoCloseDate/AutoCloseAt/g' CreateMonthsDialog.jsx EditConfigDialog.jsx
```

`CreateMonthsDialog.jsx` 再改：
- `<label htmlFor="autoCloseAt">系統自動結束新增設定日期</label>` → `<label htmlFor="autoCloseAt">系統自動結束新增設定時間</label>`
- 該 input 的 `type="date"` → `type="datetime-local"`
- `min={today}` → ``min={`${today}T00:00`}``

`EditConfigDialog.jsx` 再改：
- import 加 `import { toDateTimeLocalValue } from '../../utils/dateTime'`
- `setAutoCloseAt(config.autoCloseAt ?? '')` → `setAutoCloseAt(toDateTimeLocalValue(config.autoCloseAt))`
- `autoCloseAt !== (config.autoCloseAt ?? '')` → `autoCloseAt !== toDateTimeLocalValue(config.autoCloseAt)`
- `<label htmlFor="editAutoCloseAt">自動結束新增設定日期</label>` → `<label htmlFor="editAutoCloseAt">自動結束新增設定時間</label>`
- 該 input 的 `type="date"` → `type="datetime-local"`
- `已結束新增設定，無法再開放；仍可修改自動結束新增設定日期。` → `已結束新增設定，無法再開放；需要業務再填請到明細頁「開放補登」。`

- [ ] **Step 7: 對話框測試（整檔取代）**

`__tests__/CreateMonthsDialog.test.jsx`:
```jsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import CreateMonthsDialog from '../CreateMonthsDialog'

vi.mock('../../../api/forecastConfig', () => ({ createMonth: vi.fn() }))

import { createMonth } from '../../../api/forecastConfig'

function renderDialog(open = true) {
  const onClose = vi.fn()
  const onSuccess = vi.fn()
  render(
    <ToastProvider>
      <CreateMonthsDialog open={open} onClose={onClose} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { onClose, onSuccess }
}

function fillForm() {
  fireEvent.change(screen.getByLabelText('填寫月份'), { target: { value: '2026-12' } })
  fireEvent.change(screen.getByLabelText('系統自動結束新增設定時間'), { target: { value: '2026-12-10T18:30' } })
}

describe('CreateMonthsDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('open=false 時不渲染', () => {
    renderDialog(false)
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('截止欄位為日期加時間，兩欄都填才能建立', () => {
    renderDialog()
    expect(screen.getByLabelText('系統自動結束新增設定時間')).toHaveAttribute('type', 'datetime-local')
    expect(screen.getByRole('button', { name: '建立' })).toBeDisabled()
    fillForm()
    expect(screen.getByRole('button', { name: '建立' })).toBeEnabled()
  })

  it('送出 YYYYMM 與截止時間', async () => {
    createMonth.mockResolvedValue({})
    const { onSuccess } = renderDialog()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: '建立' }))
    await waitFor(() => expect(createMonth).toHaveBeenCalledWith('202612', '2026-12-10T18:30'))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('409 顯示後端訊息', async () => {
    createMonth.mockRejectedValue({ response: { status: 409, data: { message: '該月份已存在' } } })
    renderDialog()
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: '建立' }))
    expect(await screen.findByText('該月份已存在')).toBeInTheDocument()
  })

  it('取消呼叫 onClose', () => {
    const { onClose } = renderDialog()
    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(onClose).toHaveBeenCalled()
  })
})
```

`__tests__/EditConfigDialog.test.jsx`:
```jsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import EditConfigDialog from '../EditConfigDialog'

vi.mock('../../../api/forecastConfig', () => ({ updateConfig: vi.fn() }))

import { updateConfig } from '../../../api/forecastConfig'

const openConfig = { id: 1, month: '202612', autoCloseAt: '2026-12-10T00:00:00', isClosed: false, closedAt: null }
const closedConfig = { ...openConfig, isClosed: true, closedAt: '2026-12-09T16:00:00' }

function renderDialog(config) {
  const onSuccess = vi.fn()
  const utils = render(
    <ToastProvider>
      <EditConfigDialog open config={config} onClose={vi.fn()} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { ...utils, onSuccess }
}

describe('EditConfigDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('帶入既有截止時間（日期加時間）', () => {
    renderDialog(openConfig)
    const input = screen.getByLabelText('自動結束新增設定時間')
    expect(input).toHaveAttribute('type', 'datetime-local')
    expect(input).toHaveValue('2026-12-10T00:00')
  })

  it('未變更時不能儲存', () => {
    renderDialog(openConfig)
    expect(screen.getByRole('button', { name: '儲存' })).toBeDisabled()
  })

  it('修改截止時間後送出', async () => {
    updateConfig.mockResolvedValue({})
    const { onSuccess } = renderDialog(openConfig)
    fireEvent.change(screen.getByLabelText('自動結束新增設定時間'), { target: { value: '2026-12-11T18:00' } })
    fireEvent.click(screen.getByRole('button', { name: '儲存' }))
    await waitFor(() => expect(updateConfig).toHaveBeenCalledWith(1, { autoCloseAt: '2026-12-11T18:00', isClosed: false }))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('已結束的月份不能改回開放', () => {
    const { container } = renderDialog(closedConfig)
    expect(container.querySelector('#editIsClosed')).toBeDisabled()
    expect(screen.getByText(/已結束新增設定，無法再開放/)).toBeInTheDocument()
  })

  it('400 顯示後端訊息', async () => {
    updateConfig.mockRejectedValue({ response: { status: 400, data: { message: 'autoCloseAt format must be YYYY-MM-DDTHH:mm' } } })
    renderDialog(openConfig)
    fireEvent.change(screen.getByLabelText('自動結束新增設定時間'), { target: { value: '2026-12-11T18:00' } })
    fireEvent.click(screen.getByRole('button', { name: '儲存' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('autoCloseAt format')
  })
})
```

- [ ] **Step 8: 列表頁（ForecastConfigPage.jsx）**

- import 區加：
```js
import { Link } from 'react-router-dom'
import { formatLocalDateTime, formatTaipeiDateTime } from '../../utils/dateTime'
```
- 刪除整個 `function formatClosedAt(dateStr) { ... }`（含上方 `/** 將後端時間轉成台灣時間顯示（後端多為 UTC 或無時區） */` 註解，共 17 行）。
- `<th>自動結束新增設定日期</th>` → `<th>自動結束新增設定時間</th>`
- `<td>{c.month}</td>` → ``<td><Link to={`/sales-forecast/config/${c.id}`}>{c.month}</Link></td>``
- `<td>{c.autoCloseDate ?? '-'}</td>` → `<td>{formatLocalDateTime(c.autoCloseAt)}</td>`
- 狀態欄 `</span>` 之後、`</td>` 之前加：
```jsx
                    {c.isClosed && c.activeReopenCount > 0 && (
                      <span className="badge--reopen">補登中 {c.activeReopenCount} 通路</span>
                    )}
```
- `formatClosedAt(c.closedAt)` → `formatTaipeiDateTime(c.closedAt)`

`ForecastConfig.css` 檔尾加：
```css
.badge--reopen {
  margin-left: 6px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #fff3e0;
  color: #e65100;
  font-size: 12px;
}

.fc-reopen-warning {
  margin: 8px 0;
  padding: 8px 12px;
  border: 1px solid #ffcc80;
  border-radius: 4px;
  background: #fff8e1;
  color: #8d6e00;
  font-size: 13px;
}

.fc-channel-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
}

.fc-detail-section {
  margin-top: 20px;
}
```

- [ ] **Step 9: 更新 ForecastConfigPage.test.jsx**

- `vi.mock` 內 `createMonths: vi.fn(),` → `createMonth: vi.fn(),`
- `mockConfigs[0]` 的 `autoCloseDay: 10,` → `autoCloseAt: '2026-01-10T00:00:00', activeReopenCount: 0,`
- `mockConfigs[1]` 的 `autoCloseDay: 15,` → `autoCloseAt: '2025-12-15T00:00:00', activeReopenCount: 2,`
- `expect(screen.getByText('10')).toBeInTheDocument()` → `expect(screen.getByText('2026/01/10 00:00')).toBeInTheDocument()`
- `expect(screen.getByText('15')).toBeInTheDocument()` → `expect(screen.getByText('2025/12/15 00:00')).toBeInTheDocument()`
- 檔尾 `})` 之前加：
```jsx
  it('已結束且有補登中通路時顯示橘色標籤', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view'] })
    expect(await screen.findByText('補登中 2 通路')).toBeInTheDocument()
  })

  it('月份連結導向表單明細頁', async () => {
    listConfigs.mockResolvedValue(mockConfigs)
    renderPage({ permissions: ['sales_forecast_config.view'] })
    const link = await screen.findByRole('link', { name: '202601' })
    expect(link).toHaveAttribute('href', '/sales-forecast/config/1')
  })
```

- [ ] **Step 10: 截止提醒列改讀 autoCloseAt（整檔取代 SalesForecastFillDeadlineBanner.jsx）**

此元件掛在 `MainLayout`，原本讀已不存在的 `autoCloseDay`，目前永遠不顯示。
```jsx
import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { listConfigs } from '../api/forecastConfig'
import { formatLocalDateTime } from '../utils/dateTime'
import './SalesForecastFillDeadlineBanner.css'

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

function startOfLocalDay(d) {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate())
}

/** month: YYYYMM → YYYY-MM */
function formatYm(monthStr) {
  const s = String(monthStr ?? '')
  return /^\d{6}$/.test(s) ? `${s.slice(0, 4)}-${s.slice(4, 6)}` : s
}

/** 開放中且 7 天內截止的月份 */
function buildAlerts(configs) {
  const today = startOfLocalDay(new Date())
  const out = []
  for (const c of configs) {
    if (c.isClosed || !c.autoCloseAt) continue
    const deadline = new Date(`${String(c.autoCloseAt).slice(0, 19)}+08:00`)
    if (isNaN(deadline.getTime())) continue
    const diff = Math.round((startOfLocalDay(deadline).getTime() - today.getTime()) / 86400000)
    if (diff >= 0 && diff <= 7) {
      out.push({ monthLabel: formatYm(c.month), deadlineLabel: formatLocalDateTime(c.autoCloseAt) })
    }
  }
  return out
}

export default function SalesForecastFillDeadlineBanner() {
  const { user } = useAuth()
  const [alerts, setAlerts] = useState([])

  useEffect(() => {
    if (!user || !hasPermission(user, 'sales_forecast.upload')) {
      setAlerts([])
      return
    }
    let cancelled = false
    listConfigs()
      .then((data) => {
        if (!cancelled) setAlerts(buildAlerts(Array.isArray(data) ? data : []))
      })
      .catch(() => {
        if (!cancelled) setAlerts([])
      })
    return () => {
      cancelled = true
    }
  }, [user])

  if (!alerts.length) return null

  return (
    <div className="sales-fill-deadline-banner" role="status">
      {alerts.map((a) => (
        <p key={`${a.monthLabel}-${a.deadlineLabel}`} className="sales-fill-deadline-banner__line">
          開放填寫月份 <strong>{a.monthLabel}</strong> 表單填寫期限至 <strong>{a.deadlineLabel}</strong> 為止，截止後將無法上傳，若需延展請於到期前聯繫生管同仁。
        </p>
      ))}
    </div>
  )
}
```

- [ ] **Step 11: 跑測試與 lint**

Run: `FE_TEST src/utils src/api/__tests__/forecastConfig.test.js src/pages/sales-forecast/__tests__/CreateMonthsDialog.test.jsx src/pages/sales-forecast/__tests__/EditConfigDialog.test.jsx src/pages/sales-forecast/__tests__/ForecastConfigPage.test.jsx src/layouts`
Expected: 全部通過。
Run: `FE_LINT src/utils/dateTime.js src/api/forecastConfig.js src/pages/sales-forecast/CreateMonthsDialog.jsx src/pages/sales-forecast/EditConfigDialog.jsx src/pages/sales-forecast/ForecastConfigPage.jsx src/components/SalesForecastFillDeadlineBanner.jsx`
Expected: 無錯誤。

- [ ] **Step 12: Commit**

```bash
git add frontend/src/utils/dateTime.js frontend/src/utils/__tests__/dateTime.test.js \
  frontend/src/api/forecastConfig.js frontend/src/api/__tests__/forecastConfig.test.js \
  frontend/src/pages/sales-forecast/CreateMonthsDialog.jsx frontend/src/pages/sales-forecast/EditConfigDialog.jsx \
  frontend/src/pages/sales-forecast/__tests__/CreateMonthsDialog.test.jsx frontend/src/pages/sales-forecast/__tests__/EditConfigDialog.test.jsx \
  frontend/src/pages/sales-forecast/ForecastConfigPage.jsx frontend/src/pages/sales-forecast/ForecastConfig.css \
  frontend/src/pages/sales-forecast/__tests__/ForecastConfigPage.test.jsx \
  frontend/src/components/SalesForecastFillDeadlineBanner.jsx
git commit -m "feat(F129): 前端截止改日期加時間、列表頁進入明細與補登標籤，更新過時測試"
```

---

### Task 10: 表單明細頁與開放補登視窗（R4-4、R4-5 前端）

**Files:**
- Create: `frontend/src/pages/sales-forecast/ForecastConfigDetailPage.jsx`
- Create: `frontend/src/pages/sales-forecast/ReopenDialog.jsx`
- Modify: `frontend/src/router.jsx`
- Test: `frontend/src/pages/sales-forecast/__tests__/ForecastConfigDetailPage.test.jsx`
- Test: `frontend/src/pages/sales-forecast/__tests__/ReopenDialog.test.jsx`

**Interfaces:**
- Consumes: `getFormDetail`、`getFormLogs`、`reopenChannels`、`endReopen`（Task 9）、`formatLocalDateTime`、`formatTaipeiDateTime`、`toDateTimeLocalValue`、`remainingText`
- Produces: 路由 `/sales-forecast/config/:id`；`<ReopenDialog open configId month channels maxExpiresAt onClose onSuccess />`

- [ ] **Step 1: 寫失敗測試 ReopenDialog.test.jsx**

```jsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ToastProvider } from '../../../components/Toast'
import ReopenDialog from '../ReopenDialog'

vi.mock('../../../api/forecastConfig', () => ({ reopenChannels: vi.fn() }))

import { reopenChannels } from '../../../api/forecastConfig'

const channels = [
  { channel: '家樂福', activeWindow: null, correctionCount: 2 },
  { channel: '愛買', activeWindow: null, correctionCount: 0 },
  { channel: '7-11', activeWindow: { id: 9, expiresAt: '2026-12-11T18:00:00', reason: 'x' }, correctionCount: 0 },
]

function renderDialog() {
  const onSuccess = vi.fn()
  render(
    <ToastProvider>
      <ReopenDialog open configId="7" month="202612" channels={channels}
        maxExpiresAt="2026-12-15T08:00:00" onClose={vi.fn()} onSuccess={onSuccess} />
    </ToastProvider>
  )
  return { onSuccess }
}

describe('ReopenDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('補登中的通路不能選，期限上限顯示在提示', () => {
    renderDialog()
    expect(screen.getByLabelText('7-11（補登中）')).toBeDisabled()
    expect(screen.getByText(/上限：2026\/12\/15 08:00/)).toBeInTheDocument()
    expect(screen.getByLabelText('補登截止時間')).toHaveAttribute('max', '2026-12-15T08:00')
  })

  it('通路、期限、原因都填才能送出', () => {
    renderDialog()
    const submit = screen.getByRole('button', { name: '確認開放' })
    expect(submit).toBeDisabled()
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-12T18:00' } })
    expect(submit).toBeDisabled()
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: '新品追加' } })
    expect(submit).toBeEnabled()
  })

  it('選到已有生管修正的通路時顯示覆蓋提醒', () => {
    renderDialog()
    expect(screen.queryByText(/將覆蓋此通路數量/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('家樂福'))
    expect(screen.getByText(/家樂福（2 筆）/)).toBeInTheDocument()
    expect(screen.getByText(/將覆蓋此通路數量/)).toBeInTheDocument()
  })

  it('送出可複選通路並呼叫 onSuccess', async () => {
    reopenChannels.mockResolvedValue()
    const { onSuccess } = renderDialog()
    fireEvent.click(screen.getByLabelText('家樂福'))
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-12T18:00' } })
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: ' 新品追加 ' } })
    fireEvent.click(screen.getByRole('button', { name: '確認開放' }))
    await waitFor(() => expect(reopenChannels).toHaveBeenCalledWith('7', {
      channels: ['家樂福', '愛買'], expiresAt: '2026-12-12T18:00', reason: '新品追加',
    }))
    expect(onSuccess).toHaveBeenCalled()
  })

  it('後端拒絕時顯示訊息', async () => {
    reopenChannels.mockRejectedValue({ response: { status: 400, data: { message: '補登期限不可超過 2026/12/15 08:00' } } })
    renderDialog()
    fireEvent.click(screen.getByLabelText('愛買'))
    fireEvent.change(screen.getByLabelText('補登截止時間'), { target: { value: '2026-12-16T18:00' } })
    fireEvent.change(screen.getByLabelText('開放原因'), { target: { value: 'x' } })
    fireEvent.click(screen.getByRole('button', { name: '確認開放' }))
    expect(await screen.findByText('補登期限不可超過 2026/12/15 08:00')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ReopenDialog.test.jsx`
Expected: FAIL（找不到模組）。

- [ ] **Step 3: 實作 ReopenDialog.jsx**

```jsx
import { useState } from 'react'
import { reopenChannels } from '../../api/forecastConfig'
import { useToast } from '../../components/Toast'
import { formatLocalDateTime, toDateTimeLocalValue } from '../../utils/dateTime'
import './ForecastConfig.css'

/** 開放補登：通路可複選、共用一個補登截止時間與原因；上限由後端給（開放後 3 個工作天） */
export default function ReopenDialog({ open, configId, month, channels, maxExpiresAt, onClose, onSuccess }) {
  const toast = useToast()
  const [selected, setSelected] = useState([])
  const [expiresAt, setExpiresAt] = useState('')
  const [reason, setReason] = useState('')
  const [loading, setLoading] = useState(false)

  if (!open) return null

  const overwrite = channels.filter((c) => selected.includes(c.channel) && c.correctionCount > 0)
  const canSubmit = !loading && selected.length > 0 && expiresAt !== '' && reason.trim() !== ''

  function reset() {
    setSelected([])
    setExpiresAt('')
    setReason('')
  }

  function handleToggle(channel) {
    setSelected((prev) => (prev.includes(channel) ? prev.filter((c) => c !== channel) : [...prev, channel]))
  }

  function handleClose() {
    reset()
    onClose()
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!canSubmit) return
    setLoading(true)
    try {
      // 依畫面通路順序送出
      const ordered = channels.map((c) => c.channel).filter((c) => selected.includes(c))
      await reopenChannels(configId, { channels: ordered, expiresAt, reason: reason.trim() })
      toast.success('已開放補登')
      reset()
      onSuccess()
    } catch (err) {
      toast.error(err.response?.data?.message || '開放補登失敗')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="dialog-overlay" onClick={handleClose}>
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reopen-dialog-title"
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: '560px' }}
      >
        <h3 id="reopen-dialog-title" className="dialog-title">開放補登 – {month}</h3>
        <form onSubmit={handleSubmit}>
          <fieldset className="fc-dialog-field">
            <legend>開放通路（可複選）</legend>
            <div className="fc-channel-checks">
              {channels.map((c) => {
                const busy = Boolean(c.activeWindow)
                const id = `reopen-ch-${c.channel}`
                return (
                  <label key={c.channel} htmlFor={id}>
                    <input
                      id={id}
                      type="checkbox"
                      checked={selected.includes(c.channel)}
                      disabled={busy || loading}
                      onChange={() => handleToggle(c.channel)}
                    />
                    {busy ? `${c.channel}（補登中）` : c.channel}
                  </label>
                )
              })}
            </div>
          </fieldset>
          <div className="fc-dialog-field">
            <label htmlFor="reopenExpiresAt">補登截止時間</label>
            <input
              id="reopenExpiresAt"
              className="form-input"
              type="datetime-local"
              value={expiresAt}
              max={toDateTimeLocalValue(maxExpiresAt)}
              onChange={(e) => setExpiresAt(e.target.value)}
              disabled={loading}
            />
            <div className="form-hint">上限：{formatLocalDateTime(maxExpiresAt)}（開放後 3 個工作天）</div>
          </div>
          <div className="fc-dialog-field">
            <label htmlFor="reopenReason">開放原因</label>
            <textarea
              id="reopenReason"
              className="form-input"
              rows={2}
              maxLength={200}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              disabled={loading}
            />
          </div>
          {overwrite.length > 0 && (
            <div className="fc-reopen-warning" role="alert">
              {overwrite.map((c) => `${c.channel}（${c.correctionCount} 筆）`).join('、')}
              截止後已有生管修正。業務補登後將覆蓋此通路數量，補登結束時系統自動產生新版本並保留差異。
            </div>
          )}
          <div className="dialog-actions">
            <button type="button" className="btn btn--secondary" onClick={handleClose} disabled={loading}>
              取消
            </button>
            <button type="submit" className="btn btn--primary" disabled={!canSubmit}>
              {loading ? '開放中...' : '確認開放'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ReopenDialog.test.jsx`
Expected: 5 passed。若 `getByLabelText('家樂福')` 因 `<label>` 內含 input 而比對到兩個，改用 `getByRole('checkbox', { name: '家樂福' })`。

- [ ] **Step 5: 寫失敗測試 ForecastConfigDetailPage.test.jsx**

```jsx
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import AuthContext from '../../../contexts/AuthContext'
import { ToastProvider } from '../../../components/Toast'
import ForecastConfigDetailPage from '../ForecastConfigDetailPage'

vi.mock('../../../api/forecastConfig', () => ({
  getFormDetail: vi.fn(),
  getFormLogs: vi.fn(),
  endReopen: vi.fn(),
  reopenChannels: vi.fn(),
}))

import { getFormDetail, getFormLogs, endReopen } from '../../../api/forecastConfig'

const detail = {
  config: { id: 7, month: '202612', autoCloseAt: '2026-12-10T00:00:00', isClosed: true, closedAt: '2026-12-09T16:00:00' },
  closeType: 'AUTO',
  reopenMaxExpiresAt: '2026-12-15T08:00:00',
  channels: [
    { channel: '家樂福', salesLastUploadAt: '2026-12-08T09:40:00', giftLastUploadAt: null,
      activeWindow: { id: 3, expiresAt: '2026-12-12T18:00:00', reason: '新品追加' }, correctionCount: 2 },
    { channel: '7-11', salesLastUploadAt: null, giftLastUploadAt: null, activeWindow: null, correctionCount: 0 },
  ],
}

const logs = [
  { id: 2, createdAt: '2026-12-11T09:15:00', operatorName: '陳美玲', action: 'REOPEN', channel: '家樂福', detail: '期限 2026/12/12 18:00；原因：新品追加' },
  { id: 1, createdAt: '2026-12-10T00:00:00', operatorName: '系統', action: 'AUTO_CLOSE', channel: null, detail: '到達截止時間 2026/12/10 00:00' },
]

function renderPage(permissions = ['sales_forecast_config.view', 'sales_forecast_config.reopen']) {
  const authValue = {
    token: 't',
    user: { username: 'pm', fullName: '陳美玲', roleCode: 'production_planner', permissions },
    isAuthenticated: true,
    login: vi.fn(),
    logout: vi.fn(),
  }
  return render(
    <MemoryRouter initialEntries={['/sales-forecast/config/7']}>
      <AuthContext.Provider value={authValue}>
        <ToastProvider>
          <Routes>
            <Route path="/sales-forecast/config/:id" element={<ForecastConfigDetailPage />} />
          </Routes>
        </ToastProvider>
      </AuthContext.Provider>
    </MemoryRouter>
  )
}

describe('ForecastConfigDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getFormDetail.mockResolvedValue(detail)
    getFormLogs.mockResolvedValue(logs)
  })

  it('顯示狀態、各通路上傳與補登、異動紀錄', async () => {
    renderPage()
    expect(await screen.findByText(/> 202612/)).toBeInTheDocument()
    expect(getFormDetail).toHaveBeenCalledWith('7')
    expect(screen.getByText('已結束新增設定')).toBeInTheDocument()
    expect(screen.getByText(/系統自動截止/)).toBeInTheDocument()
    expect(screen.getByText('2026/12/08 17:40:00')).toBeInTheDocument()
    expect(screen.getAllByText('未上傳')).toHaveLength(3)
    expect(screen.getByText(/補登中，至 2026\/12\/12 18:00/)).toBeInTheDocument()
    expect(screen.getByText('2 筆')).toBeInTheDocument()
    const logTable = screen.getByRole('table', { name: '異動紀錄' })
    expect(within(logTable).getByText('開放補登')).toBeInTheDocument()
    expect(within(logTable).getByText('自動截止')).toBeInTheDocument()
    expect(within(logTable).getByText('全部')).toBeInTheDocument()
  })

  it('有權限 35 才顯示開放補登與提前結束', async () => {
    renderPage(['sales_forecast_config.view'])
    await screen.findByText(/> 202612/)
    expect(screen.queryByRole('button', { name: '開放補登' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '提前結束' })).not.toBeInTheDocument()
  })

  it('開放補登按鈕開啟視窗', async () => {
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '開放補登' }))
    expect(screen.getByRole('dialog', { name: '開放補登 – 202612' })).toBeInTheDocument()
  })

  it('提前結束後重新載入', async () => {
    endReopen.mockResolvedValue()
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: '提前結束' }))
    await waitFor(() => expect(endReopen).toHaveBeenCalledWith('7', 3))
    await waitFor(() => expect(getFormDetail).toHaveBeenCalledTimes(2))
  })

  it('403 顯示無權限', async () => {
    getFormDetail.mockRejectedValue({ response: { status: 403 } })
    renderPage()
    expect(await screen.findByText('您沒有權限檢視此頁面')).toBeInTheDocument()
  })
})
```

- [ ] **Step 6: 跑測試確認失敗**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ForecastConfigDetailPage.test.jsx`
Expected: FAIL（找不到模組）。

- [ ] **Step 7: 實作 ForecastConfigDetailPage.jsx**

```jsx
import { useState, useEffect, useCallback } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getFormDetail, getFormLogs, endReopen } from '../../api/forecastConfig'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../components/Toast'
import { formatTaipeiDateTime, formatLocalDateTime, remainingText } from '../../utils/dateTime'
import ReopenDialog from './ReopenDialog'
import './ForecastConfig.css'

const ACTION_LABELS = {
  AUTO_CLOSE: '自動截止',
  MANUAL_CLOSE: '手動結束新增',
  REOPEN: '開放補登',
  REOPEN_END: '補登結束',
  CORRECTION: '生管直接修正',
}

function hasPermission(user, perm) {
  return Boolean(user?.permissions && Array.isArray(user.permissions) && user.permissions.includes(perm))
}

/** 表單明細：月份狀態、各通路上傳／補登狀態、開放補登、異動紀錄 */
export default function ForecastConfigDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const [detail, setDetail] = useState(null)
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(true)
  const [accessDenied, setAccessDenied] = useState(false)
  const [reopenOpen, setReopenOpen] = useState(false)
  const [endingId, setEndingId] = useState(null)

  const canReopen = hasPermission(user, 'sales_forecast_config.reopen')

  const fetchAll = useCallback(async () => {
    setLoading(true)
    try {
      const [d, l] = await Promise.all([getFormDetail(id), getFormLogs(id)])
      setDetail(d)
      setLogs(Array.isArray(l) ? l : [])
    } catch (err) {
      if (err.response?.status === 403) setAccessDenied(true)
      else toast.error(err.response?.data?.message || '無法載入表單明細')
    } finally {
      setLoading(false)
    }
  }, [id, toast])

  useEffect(() => {
    fetchAll()
  }, [fetchAll])

  async function handleEnd(windowId) {
    setEndingId(windowId)
    try {
      await endReopen(id, windowId)
      toast.success('已結束補登並產生新版本')
      await fetchAll()
    } catch (err) {
      toast.error(err.response?.data?.message || '結束補登失敗')
    } finally {
      setEndingId(null)
    }
  }

  const config = detail?.config

  return (
    <div className="forecast-page">
      <div className="forecast-page-header">
        <h1>
          <Link to="/sales-forecast/config">銷售預估量-表單設定</Link>
          {config && ` > ${config.month}`}
        </h1>
        {config?.isClosed && canReopen && (
          <button type="button" className="btn btn--primary" onClick={() => setReopenOpen(true)}>
            開放補登
          </button>
        )}
      </div>

      {accessDenied ? (
        <div className="forecast-access-denied" role="alert">您沒有權限檢視此頁面</div>
      ) : loading && !detail ? (
        <div className="forecast-loading" role="status">載入中...</div>
      ) : !detail ? (
        <div className="forecast-empty">找不到此表單</div>
      ) : (
        <>
          <p>
            <span className={config.isClosed ? 'badge--closed' : 'badge--open'}>
              {config.isClosed ? '已結束新增設定' : '開放中'}
            </span>{' '}
            截止時間 {formatLocalDateTime(config.autoCloseAt)}
            {config.isClosed &&
              `（${detail.closeType === 'MANUAL' ? '生管手動結束' : '系統自動截止'}，結束於 ${formatTaipeiDateTime(config.closedAt)}）`}
          </p>

          <section className="fc-detail-section">
            <h2>各通路狀態</h2>
            <div className="forecast-table-wrap">
              <table className="forecast-table" aria-label="各通路狀態">
                <thead>
                  <tr>
                    <th>通路</th>
                    <th>業務最後上傳（銷售）</th>
                    <th>業務最後上傳（禮品）</th>
                    <th>補登</th>
                    <th>生管修正</th>
                    {canReopen && <th className="actions-cell">操作</th>}
                  </tr>
                </thead>
                <tbody>
                  {detail.channels.map((c) => (
                    <tr key={c.channel}>
                      <td>{c.channel}</td>
                      <UploadCell value={c.salesLastUploadAt} />
                      <UploadCell value={c.giftLastUploadAt} />
                      <td>
                        {c.activeWindow
                          ? `補登中，至 ${formatLocalDateTime(c.activeWindow.expiresAt)}（${remainingText(c.activeWindow.expiresAt)}）`
                          : '—'}
                      </td>
                      <td>{c.correctionCount > 0 ? `${c.correctionCount} 筆` : '—'}</td>
                      {canReopen && (
                        <td className="actions-cell">
                          {c.activeWindow && (
                            <button
                              type="button"
                              className="btn btn--small btn--outline"
                              disabled={endingId === c.activeWindow.id}
                              onClick={() => handleEnd(c.activeWindow.id)}
                            >
                              提前結束
                            </button>
                          )}
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="fc-detail-section">
            <h2>異動紀錄</h2>
            {logs.length === 0 ? (
              <p className="forecast-empty">尚無異動紀錄</p>
            ) : (
              <div className="forecast-table-wrap">
                <table className="forecast-table" aria-label="異動紀錄">
                  <thead>
                    <tr>
                      <th>時間</th>
                      <th>操作者</th>
                      <th>動作</th>
                      <th>通路</th>
                      <th>內容</th>
                    </tr>
                  </thead>
                  <tbody>
                    {logs.map((l) => (
                      <tr key={l.id}>
                        <td>{formatLocalDateTime(l.createdAt)}</td>
                        <td>{l.operatorName}</td>
                        <td>{ACTION_LABELS[l.action] ?? l.action}</td>
                        <td>{l.channel ?? '全部'}</td>
                        <td>{l.detail ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <ReopenDialog
            open={reopenOpen}
            configId={id}
            month={config.month}
            channels={detail.channels}
            maxExpiresAt={detail.reopenMaxExpiresAt}
            onClose={() => setReopenOpen(false)}
            onSuccess={() => {
              setReopenOpen(false)
              fetchAll()
            }}
          />
        </>
      )}
    </div>
  )
}

function UploadCell({ value }) {
  return value
    ? <td>{formatTaipeiDateTime(value)}</td>
    : <td style={{ color: '#c62828', fontWeight: 600 }}>未上傳</td>
}
```

- [ ] **Step 8: 路由**

`frontend/src/router.jsx`：
- `import ForecastConfigPage from './pages/sales-forecast/ForecastConfigPage'` 下一行加 `import ForecastConfigDetailPage from './pages/sales-forecast/ForecastConfigDetailPage'`
- `<Route path="sales-forecast/config" element={<ForecastConfigPage />} />` 下一行加 `<Route path="sales-forecast/config/:id" element={<ForecastConfigDetailPage />} />`（縮排與上一行相同）

- [ ] **Step 9: 跑測試與 lint**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ForecastConfigDetailPage.test.jsx src/pages/sales-forecast/__tests__/ReopenDialog.test.jsx`
Expected: 全部通過。
Run: `FE_LINT src/pages/sales-forecast/ForecastConfigDetailPage.jsx src/pages/sales-forecast/ReopenDialog.jsx src/router.jsx`
Expected: 無錯誤。

- [ ] **Step 10: Commit**

```bash
git add frontend/src/pages/sales-forecast/ForecastConfigDetailPage.jsx frontend/src/pages/sales-forecast/ReopenDialog.jsx \
  frontend/src/router.jsx \
  frontend/src/pages/sales-forecast/__tests__/ForecastConfigDetailPage.test.jsx \
  frontend/src/pages/sales-forecast/__tests__/ReopenDialog.test.jsx
git commit -m "feat(F129): 表單明細頁（通路狀態、開放補登、提前結束、異動紀錄）"
```

---

### Task 11: 業務上傳頁：紅／橘提示列與「生管最新版數量」（R4-2 前端）

**Files:**
- Create: `frontend/src/components/forecast/ForecastEditStatusBanner.jsx`
- Create: `frontend/src/components/forecast/ForecastEditStatusBanner.css`
- Test: `frontend/src/components/forecast/__tests__/ForecastEditStatusBanner.test.jsx`
- Modify: `frontend/src/pages/sales-forecast/ForecastUploadPage.jsx`
- Modify: `frontend/src/pages/sales-forecast/GiftForecastUploadPage.jsx`
- Test: `frontend/src/pages/sales-forecast/__tests__/ForecastUploadPage.test.jsx`

**Interfaces:**
- Consumes: `getEditStatus(month, channel)` → `{ status, autoCloseAt, reopenExpiresAt, reopenReason, plannerQuantities }`
- Produces: `<ForecastEditStatusBanner status month />`（default export）、`<PlannerQtyCell item planner />`（named export）

- [ ] **Step 1: 寫失敗測試 ForecastEditStatusBanner.test.jsx**

```jsx
import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import ForecastEditStatusBanner, { PlannerQtyCell } from '../ForecastEditStatusBanner'

describe('ForecastEditStatusBanner', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('開放中不顯示', () => {
    const { container } = render(<ForecastEditStatusBanner status={{ status: 'OPEN' }} month="202612" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('已截止顯示紅色提示', () => {
    render(<ForecastEditStatusBanner status={{ status: 'CLOSED', autoCloseAt: '2026-12-10T00:00:00' }} month="202612" />)
    const alert = screen.getByRole('alert')
    expect(alert).toHaveClass('forecast-edit-status--closed')
    expect(alert).toHaveTextContent('已結束新增')
    expect(alert).toHaveTextContent('2026/12/10 00:00')
  })

  it('補登中顯示橘色提示、期限與剩餘時間', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-12-11T04:00:00Z'))
    render(<ForecastEditStatusBanner
      status={{ status: 'REOPENED', reopenExpiresAt: '2026-12-12T18:00:00', reopenReason: '全聯新品提案追加' }}
      month="202612" />)
    const box = screen.getByRole('status')
    expect(box).toHaveClass('forecast-edit-status--reopen')
    expect(box).toHaveTextContent('補登中')
    expect(box).toHaveTextContent('2026/12/12 18:00')
    expect(box).toHaveTextContent('剩餘 1 天 6 小時')
    expect(box).toHaveTextContent('全聯新品提案追加')
  })

  it('PlannerQtyCell 與業務數量不同時紅字', () => {
    render(
      <table><tbody><tr>
        <PlannerQtyCell item={{ productCode: 'P1', quantity: 120 }} planner={{ P1: 150 }} />
        <PlannerQtyCell item={{ product_code: 'P2', quantity: 5 }} planner={{ P2: 5 }} />
        <PlannerQtyCell item={{ productCode: 'P3', quantity: 5 }} planner={{}} />
      </tr></tbody></table>
    )
    expect(screen.getByText('150')).toHaveClass('planner-qty--changed')
    expect(screen.getByText('5')).not.toHaveClass('planner-qty--changed')
    expect(screen.getByText('-')).toBeInTheDocument()
  })
})
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `FE_TEST src/components/forecast/__tests__/ForecastEditStatusBanner.test.jsx`
Expected: FAIL。

- [ ] **Step 3: 實作元件與樣式**

`ForecastEditStatusBanner.jsx`:
```jsx
import { formatLocalDateTime, remainingText } from '../../utils/dateTime'
import './ForecastEditStatusBanner.css'

/** 業務上傳頁的截止／補登提示（PDF R4 第 19 頁） */
export default function ForecastEditStatusBanner({ status, month }) {
  if (!status || status.status === 'OPEN') return null

  if (status.status === 'REOPENED') {
    return (
      <div className="forecast-edit-status forecast-edit-status--reopen" role="status">
        <strong>補登中</strong>
        生管已開放此通路補登至 {formatLocalDateTime(status.reopenExpiresAt)}，{remainingText(status.reopenExpiresAt)}。
        原因：{status.reopenReason}
      </div>
    )
  }

  return (
    <div className="forecast-edit-status forecast-edit-status--closed" role="alert">
      <strong>已結束新增</strong>
      {month} 表單截止時間 {formatLocalDateTime(status.autoCloseAt)}，上傳與編輯已關閉。如需異動請洽生管（生管可直接修正，或開放補登）。
    </div>
  )
}

/** 「生管最新版數量」欄：與業務數量不同時紅字 */
export function PlannerQtyCell({ item, planner }) {
  const code = item.product_code ?? item.productCode
  const qty = planner?.[code]
  if (qty == null) return <td className="align-right">-</td>
  const changed = Number(qty) !== Number(item.quantity)
  return <td className={`align-right${changed ? ' planner-qty--changed' : ''}`}>{qty}</td>
}
```

`ForecastEditStatusBanner.css`:
```css
.forecast-edit-status {
  margin: 8px 0;
  padding: 10px 14px;
  border-radius: 4px;
  font-size: 14px;
  line-height: 1.5;
}

.forecast-edit-status strong {
  margin-right: 12px;
}

.forecast-edit-status--closed {
  border: 1px solid #ef9a9a;
  background: #ffebee;
  color: #b71c1c;
}

.forecast-edit-status--reopen {
  border: 1px solid #ffcc80;
  background: #fff3e0;
  color: #e65100;
}

.planner-qty--changed {
  color: #c62828;
  font-weight: 600;
}
```

- [ ] **Step 4: 跑測試確認通過**

Run: `FE_TEST src/components/forecast/__tests__/ForecastEditStatusBanner.test.jsx`
Expected: 4 passed。

- [ ] **Step 5: 套用到兩個上傳頁（ForecastUploadPage.jsx 與 GiftForecastUploadPage.jsx 做相同修改）**

1. `import { listConfigs } from '../../api/forecastConfig'` → `import { listConfigs, getEditStatus } from '../../api/forecastConfig'`，下一行加 `import ForecastEditStatusBanner, { PlannerQtyCell } from '../../components/forecast/ForecastEditStatusBanner'`
2. `const isMonthOpen = selectedConfig && !selectedConfig.isClosed` 換成：
```js
  const [editStatus, setEditStatus] = useState(null)
  // 查詢後以後端判斷（含補登）為準；查詢前沿用月份設定
  const isMonthOpen = editStatus ? editStatus.status !== 'CLOSED' : Boolean(selectedConfig && !selectedConfig.isClosed)
  const showPlanner = Boolean(editStatus?.plannerQuantities)

  useEffect(() => {
    setEditStatus(null)
  }, [selectedMonth, selectedChannel])
```
3. `runQuery` 內 `setLoadingQuery(true)` 下一行加：
```js
    getEditStatus(selectedMonth, selectedChannel)
      .then(setEditStatus)
      .catch(() => setEditStatus(null))
```
4. `{/* Block 3: 上傳結果 */}` 上一行加：
```jsx
          <ForecastEditStatusBanner status={editStatus} month={selectedMonth} />
```
5. `<th className="align-right">箱數小計</th>` 下一行加：
```jsx
                                {showPlanner && <th className="align-right">生管最新版數量</th>}
```
6. `<td>{item.remark || '-'}</td>` 換成：
```jsx
                                    {showPlanner && <PlannerQtyCell item={item} planner={editStatus.plannerQuantities} />}
                                    <td>{item.remark || '-'}</td>
```

- [ ] **Step 6: 更新 ForecastUploadPage.test.jsx**

- `vi.mock('../../../api/forecastConfig', () => ({ listConfigs: vi.fn(), }))` 改為 `vi.mock('../../../api/forecastConfig', () => ({ listConfigs: vi.fn(), getEditStatus: vi.fn() }))`
- `import { listConfigs } from '../../../api/forecastConfig'` → `import { listConfigs, getEditStatus } from '../../../api/forecastConfig'`
- `mockConfigs` 三筆的 `autoCloseDay: N,` 改為 `autoCloseAt: '2026-01-10T00:00:00',`（三筆同值即可）
- `beforeEach` 加一行 `getEditStatus.mockResolvedValue({ status: 'OPEN' })`
- 檔尾 `})` 之前加：
```jsx
  it('補登截止後顯示紅色提示與生管最新版數量', async () => {
    const user = userEvent.setup()
    listConfigs.mockResolvedValue(mockConfigs)
    getEditStatus.mockResolvedValue({
      status: 'CLOSED',
      autoCloseAt: '2026-01-10T00:00:00',
      plannerQuantities: { P1: 150 },
    })
    getForecastVersions.mockResolvedValue(['v1'])
    getForecastList.mockResolvedValue([
      { id: 1, category: 'c', spec: 's', productCode: 'P1', productName: 'N1', warehouseLocation: 'W', quantity: 120 },
    ])
    renderPage({ permissions: ['sales_forecast.upload'], channels: ['家樂福'] })

    await selectMonthChannelAndQuery(user)

    await waitFor(() => expect(getEditStatus).toHaveBeenCalledWith('202601', '家樂福'))
    expect(await screen.findByText('生管最新版數量')).toBeInTheDocument()
    expect(screen.getByText('150')).toHaveClass('planner-qty--changed')
    expect(screen.getByText(/上傳與編輯已關閉/)).toBeInTheDocument()
  })
```

- [ ] **Step 7: 跑測試與 lint**

Run: `FE_TEST src/components/forecast src/pages/sales-forecast/__tests__/ForecastUploadPage.test.jsx`
Expected: 全部通過。
Run: `FE_LINT src/components/forecast/ForecastEditStatusBanner.jsx src/pages/sales-forecast/ForecastUploadPage.jsx src/pages/sales-forecast/GiftForecastUploadPage.jsx`
Expected: 無錯誤。

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/forecast/ForecastEditStatusBanner.jsx frontend/src/components/forecast/ForecastEditStatusBanner.css \
  frontend/src/components/forecast/__tests__/ForecastEditStatusBanner.test.jsx \
  frontend/src/pages/sales-forecast/ForecastUploadPage.jsx frontend/src/pages/sales-forecast/GiftForecastUploadPage.jsx \
  frontend/src/pages/sales-forecast/__tests__/ForecastUploadPage.test.jsx
git commit -m "feat(F129): 業務上傳頁顯示截止／補登提示與生管最新版數量"
```

---

### Task 12: 生管表單：修改原因必填（R4-3 前端）

**Files:**
- Modify: `frontend/src/pages/sales-forecast/ForecastListPage.jsx`（約第 161、449–456、815–822 行）
- Test: `frontend/src/pages/sales-forecast/__tests__/ForecastListPage.test.jsx`

- [ ] **Step 1: 寫失敗測試**

`ForecastListPage.test.jsx`：
- 既有測試 `edits a single channel remark and sends per-channel remarks on save` 中，`await userEvent.click(screen.getByRole('button', { name: '儲存' }))` 之後、點 `'是'` 之前加：
```jsx
      await userEvent.type(screen.getByLabelText(/生管備註（修改原因）/), '家樂福加量')
```
並在 payload 斷言後加：
```jsx
      expect(payload.change_reason).toBe('家樂福加量')
```
- 同一個 `describe` 內新增：
```jsx
    it('修改原因空白時不能確認儲存', async () => {
      await openSummary()
      await userEvent.click(screen.getByRole('button', { name: '編輯' }))
      await userEvent.click(screen.getByRole('button', { name: '儲存' }))
      expect(screen.getByRole('button', { name: '是' })).toBeDisabled()
      await userEvent.type(screen.getByLabelText(/生管備註（修改原因）/), '   ')
      expect(screen.getByRole('button', { name: '是' })).toBeDisabled()
      expect(saveFormSummaryVersion).not.toHaveBeenCalled()
    })
```

- [ ] **Step 2: 跑測試確認失敗**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ForecastListPage.test.jsx`
Expected: FAIL（找不到「生管備註（修改原因）」欄位）。

- [ ] **Step 3: 實作**

`ForecastListPage.jsx`：
- `const [saveConfirmOpen, setSaveConfirmOpen] = useState(false)` 下一行加 `const [saveReason, setSaveReason] = useState('')`
- `handleSaveClick`、`handleSaveConfirm` 改為：
```jsx
  const handleSaveClick = () => {
    setSaveReason('')
    setSaveConfirmOpen(true)
  }

  const handleSaveConfirm = () => {
    if (!saveReason.trim()) return
    setSaveConfirmOpen(false)
    performSave(saveReason)
  }
```
- 儲存確認的 `<ConfirmDialog ... />`（`open={saveConfirmOpen}` 那個）改為：
```jsx
          <ConfirmDialog
            open={saveConfirmOpen}
            title="確認儲存"
            message="是否確認修改？請填寫生管備註（修改原因）。"
            onConfirm={handleSaveConfirm}
            onCancel={() => setSaveConfirmOpen(false)}
            confirmText="是"
            confirmButtonClass="btn--primary"
            confirmDisabled={!saveReason.trim()}
          >
            <label htmlFor="saveChangeReason">生管備註（修改原因）*</label>
            <textarea
              id="saveChangeReason"
              className="form-input"
              rows={3}
              maxLength={200}
              value={saveReason}
              onChange={(e) => setSaveReason(e.target.value)}
            />
          </ConfirmDialog>
```
（先 `sed -n 1,30p frontend/src/components/ConfirmDialog.jsx` 確認 `children` 會被渲染；若不會，在 message 下方加 `{children}`。）

- [ ] **Step 4: 跑測試與 lint**

Run: `FE_TEST src/pages/sales-forecast/__tests__/ForecastListPage.test.jsx`
Expected: 全部通過。
Run: `FE_LINT src/pages/sales-forecast/ForecastListPage.jsx`

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/sales-forecast/ForecastListPage.jsx frontend/src/pages/sales-forecast/__tests__/ForecastListPage.test.jsx
git commit -m "feat(F129): 生管儲存新版本時修改原因必填"
```

---

### Task 13: 全部驗證與開發環境實測

- [ ] **Step 1: 後端全部測試**

Run: `BE_UNIT "*"`；整合測試 `BE_IT "*IntegrationTest"`，跑完 `down -v`。
Expected: 失敗清單是 Task 0 基線的子集（新增測試全綠）。

- [ ] **Step 2: 前端全部測試**

Run: `FE_TEST`
Expected: 0 failed（原本 13 個過時測試已在 Task 9 修好）。

- [ ] **Step 3: 重建開發環境**

Run: `docker compose up -d --build`（開發環境，不加 `-p`），等後端 log 出現 `Started Application`，確認 Flyway 套用 V35、V36 無錯。

- [ ] **Step 4: 實測流程（瀏覽器 http://localhost:5173）**

1. 生管帳號：表單設定 → 新增月份，截止填「現在 + 2 分鐘」→ 等排程自動截止，列表狀態變「已結束新增設定」。
2. 業務帳號：上傳頁選該月份與自己的通路 → 查詢 → 紅色提示列、上傳按鈕反灰、有「生管最新版數量」欄。
3. 生管：點月份進明細頁 → 開放補登（選該通路、期限 1 小時後、原因）→ 通路列顯示補登中；異動紀錄有「自動截止」「開放補登」。
4. 業務：重新查詢 → 橘色提示列、可上傳；上傳一份改過數量的 Excel。
5. 生管：明細頁按「提前結束」→ 生管表單（銷售預估量表單）最新版本數量等於業務補登的數字；異動紀錄有「補登結束」。
6. 生管：生管表單編輯後按儲存 → 不填修改原因時「是」反灰；填了才能存，異動紀錄出現「生管直接修正」。
7. 業務：補登結束後再上傳 → 被擋（403，畫面提示）。

- [ ] **Step 5: 更新規格狀態**

建立 `spec/feat/F129-forecast-cutoff-reopen/`（`description.md` 摘要設計文件、`acceptance.md` 抄設計文件第 6 節驗收條件、`status.done`），commit：
```bash
git add spec/feat/F129-forecast-cutoff-reopen
git commit -m "docs(F129): 任務規格與驗收"
```
