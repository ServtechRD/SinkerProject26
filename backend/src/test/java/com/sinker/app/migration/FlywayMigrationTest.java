package com.sinker.app.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationsRunSuccessfully() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank");
        assertTrue(history.size() >= 10, "At least V1-V10 migrations should exist");

        Map<String, Object> v1 = history.get(0);
        assertEquals("1", v1.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v1.get("success")) || Integer.valueOf(1).equals(v1.get("success")),
                "V1 migration should be successful");

        Map<String, Object> v2 = history.get(1);
        assertEquals("2", v2.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v2.get("success")) || Integer.valueOf(1).equals(v2.get("success")),
                "V2 migration should be successful");

        Map<String, Object> v3 = history.get(2);
        assertEquals("3", v3.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v3.get("success")) || Integer.valueOf(1).equals(v3.get("success")),
                "V3 migration should be successful");

        Map<String, Object> v4 = history.get(3);
        assertEquals("4", v4.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v4.get("success")) || Integer.valueOf(1).equals(v4.get("success")),
                "V4 migration should be successful");

        Map<String, Object> v5 = history.get(4);
        assertEquals("5", v5.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v5.get("success")) || Integer.valueOf(1).equals(v5.get("success")),
                "V5 migration should be successful");

        // Verify V10 migration ran successfully
        boolean v10Found = false;
        for (Map<String, Object> migration : history) {
            if ("10".equals(migration.get("version").toString())) {
                v10Found = true;
                assertTrue(Boolean.TRUE.equals(migration.get("success")) || Integer.valueOf(1).equals(migration.get("success")),
                        "V10 migration should be successful");
                break;
            }
        }
        assertTrue(v10Found, "V10 migration should exist in schema history");
    }

    // --- Table structure tests ---

    @Test
    void usersTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(16, columns.size(), "users table should have 16 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "username", "email", "hashed_password", "full_name",
                "role_id", "department", "phone", "is_active", "is_locked",
                "failed_login_count", "last_login_at", "password_changed_at",
                "created_by", "created_at", "updated_at")));
    }

    @Test
    void rolesTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'roles' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(8, columns.size(), "roles table should have 8 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "code", "name", "description", "is_system", "is_active", "created_at", "updated_at")));
    }

    @Test
    void permissionsTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'permissions' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(8, columns.size(), "permissions table should have 8 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "code", "name", "module", "description", "is_active", "created_at", "updated_at")));
    }

    @Test
    void rolePermissionsTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_permissions' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(4, columns.size(), "role_permissions table should have 4 columns");
        assertTrue(columns.containsAll(List.of("id", "role_id", "permission_id", "created_at")));
    }

    // --- Seed data tests ---

    @Test
    void seedDataRoles() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class);
        assertEquals(4, count, "Should have 4 seeded roles");

        List<String> codes = jdbc.queryForList("SELECT code FROM roles ORDER BY id", String.class);
        assertEquals(List.of("admin", "sales", "production_planner", "procurement"), codes);

        Integer systemCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE is_system = TRUE AND is_active = TRUE", Integer.class);
        assertEquals(4, systemCount, "All seeded roles should be system and active");
    }

    @Test
    void seedDataPermissions() {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
        assertEquals(29, total, "Should have 29 seeded permissions");

        // Verify module counts
        List<Map<String, Object>> moduleCounts = jdbc.queryForList(
                "SELECT module, COUNT(*) as cnt FROM permissions GROUP BY module ORDER BY module");

        Map<String, Integer> expected = Map.of(
                "inventory", 2,
                "material_demand", 1,
                "material_purchase", 2,
                "production_plan", 2,
                "role", 4,
                "sales_forecast", 6,
                "sales_forecast_config", 2,
                "semi_product", 3,
                "user", 4
        );

        for (Map<String, Object> row : moduleCounts) {
            String module = (String) row.get("module");
            int cnt = ((Number) row.get("cnt")).intValue();
            if (module.equals("weekly_schedule")) {
                assertEquals(3, cnt, "Module weekly_schedule should have 3 permissions");
            } else {
                assertEquals(expected.get(module), cnt, "Module " + module + " should have correct count");
            }
        }
    }

    @Test
    void seedDataAdminUser() {
        List<Map<String, Object>> admins = jdbc.queryForList(
                "SELECT username, email, hashed_password, is_active, is_locked, failed_login_count FROM users WHERE username = 'admin'");
        assertEquals(1, admins.size(), "Should have exactly 1 admin user");

        Map<String, Object> admin = admins.get(0);
        assertEquals("admin", admin.get("username"));
        assertEquals("admin@sinker.local", admin.get("email"));
        assertTrue(admin.get("hashed_password").toString().startsWith("$2"),
                "Password should be bcrypt hashed");
        assertEquals(60, admin.get("hashed_password").toString().length(),
                "Bcrypt hash should be 60 characters");
        assertTrue((Boolean) admin.get("is_active"), "Admin should be active");
        assertFalse((Boolean) admin.get("is_locked"), "Admin should not be locked");
        assertEquals(0, ((Number) admin.get("failed_login_count")).intValue());
    }

    @Test
    void seedDataAdminRolePermissions() {
        Integer adminPermCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = (SELECT id FROM roles WHERE code = 'admin')",
                Integer.class);
        Integer totalPerms = jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class);
        assertEquals(totalPerms, adminPermCount, "Admin role should have ALL permissions");
    }

    // --- Constraint tests ---

    @Test
    void uniqueConstraintOnUsername() {
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                jdbc.update("INSERT INTO users (username, email, hashed_password, role_id) VALUES ('admin', 'other@test.com', 'hash', 1)"));
    }

    @Test
    void uniqueConstraintOnEmail() {
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                jdbc.update("INSERT INTO users (username, email, hashed_password, role_id) VALUES ('other', 'admin@sinker.local', 'hash', 1)"));
    }

    @Test
    void uniqueConstraintOnRoleCode() {
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                jdbc.update("INSERT INTO roles (code, name) VALUES ('admin', 'Duplicate Admin')"));
    }

    @Test
    void uniqueConstraintOnPermissionCode() {
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                jdbc.update("INSERT INTO permissions (code, name, module) VALUES ('user.view', 'Dup', 'user')"));
    }

    @Test
    void uniqueConstraintOnRolePermissionPair() {
        Integer roleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Integer.class);
        Integer permId = jdbc.queryForObject("SELECT MIN(id) FROM permissions", Integer.class);
        assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permId));
    }

    @Test
    void foreignKeyConstraintOnRolePermissionsInvalidRole() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (9999, 1)"));
    }

    @Test
    void foreignKeyConstraintOnRolePermissionsInvalidPermission() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 9999)"));
    }

    @Test
    void cascadeDeleteOnRoleDeletesRolePermissions() {
        // Create a test role
        jdbc.update("INSERT INTO roles (code, name, is_system) VALUES ('test_cascade', 'Test Cascade', FALSE)");
        Integer testRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'test_cascade'", Integer.class);

        // Create a role_permission mapping
        Integer permId = jdbc.queryForObject("SELECT MIN(id) FROM permissions", Integer.class);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", testRoleId, permId);

        // Verify it exists
        Integer countBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", Integer.class, testRoleId);
        assertEquals(1, countBefore);

        // Delete the role
        jdbc.update("DELETE FROM roles WHERE id = ?", testRoleId);

        // Verify cascade deleted the role_permission
        Integer countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?", Integer.class, testRoleId);
        assertEquals(0, countAfter, "Role permission should be cascade deleted");
    }

    // --- V3: sales_channels_users tests ---

    @Test
    void salesChannelsUsersTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_channels_users' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(4, columns.size(), "sales_channels_users table should have 4 columns");
        assertTrue(columns.containsAll(List.of("id", "user_id", "channel", "created_at")));
    }

    @Test
    void salesChannelsUsersChannelIsVarchar50() {
        String dataType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_channels_users' AND COLUMN_NAME = 'channel'",
                String.class);
        assertEquals("varchar(50)", dataType);
    }

    @Test
    void salesChannelsUsersForeignKeyReferencesUsers() {
        List<Map<String, Object>> fks = jdbc.queryForList(
                "SELECT REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_channels_users' AND REFERENCED_TABLE_NAME IS NOT NULL");
        assertEquals(1, fks.size());
        assertEquals("users", fks.get(0).get("REFERENCED_TABLE_NAME"));
        assertEquals("id", fks.get(0).get("REFERENCED_COLUMN_NAME"));
    }

    @Test
    void salesChannelsUsersForeignKeyCascadeDelete() {
        String deleteRule = jdbc.queryForObject(
                "SELECT DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS " +
                "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_channels_users'",
                String.class);
        assertEquals("CASCADE", deleteRule);
    }

    @Test
    void salesChannelsUsersUniqueConstraintOnUserIdChannel() {
        Integer adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Integer.class);
        jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, 'PX/大全聯')", adminId);
        try {
            assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                    jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, 'PX/大全聯')", adminId));

            // Different channel for same user should succeed
            jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, '家樂福')", adminId);
        } finally {
            jdbc.update("DELETE FROM sales_channels_users WHERE user_id = ?", adminId);
        }
    }

    @Test
    void salesChannelsUsersForeignKeyRejectsInvalidUserId() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (9999, 'PX/大全聯')"));
    }

    @Test
    void salesChannelsUsersCascadeDeleteOnUserDelete() {
        // Create test user
        jdbc.update("INSERT INTO users (username, email, hashed_password, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_cascade_ch', 'cascade_ch@test.com', 'hash', 1, TRUE, FALSE, 0)");
        Integer testUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_cascade_ch'", Integer.class);

        jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, 'PX/大全聯')", testUserId);
        Integer countBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ?", Integer.class, testUserId);
        assertEquals(1, countBefore);

        jdbc.update("DELETE FROM users WHERE id = ?", testUserId);

        Integer countAfter = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ?", Integer.class, testUserId);
        assertEquals(0, countAfter, "Channel assignments should be cascade deleted with user");
    }

    @Test
    void salesChannelsUsersIndexOnUserId() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_channels_users' AND COLUMN_NAME = 'user_id'");
        assertFalse(indexes.isEmpty(), "Index on user_id should exist");
    }

    // --- V3: login_logs tests ---

    @Test
    void loginLogsTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(8, columns.size(), "login_logs table should have 8 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "user_id", "username", "login_type", "ip_address", "user_agent", "failed_reason", "created_at")));
    }

    @Test
    void loginLogsIdIsBigint() {
        String dataType = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'id'",
                String.class);
        assertEquals("bigint", dataType);
    }

    @Test
    void loginLogsUsernameIsVarchar50() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'username'",
                String.class);
        assertEquals("varchar(50)", columnType);
    }

    @Test
    void loginLogsIpAddressIsVarchar45() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'ip_address'",
                String.class);
        assertEquals("varchar(45)", columnType);
    }

    @Test
    void loginLogsUserAgentIsText() {
        String dataType = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'user_agent'",
                String.class);
        assertEquals("text", dataType);
    }

    @Test
    void loginLogsFailedReasonIsVarchar255() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'failed_reason'",
                String.class);
        assertEquals("varchar(255)", columnType);
    }

    @Test
    void loginLogsLoginTypeIsEnum() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'login_type'",
                String.class);
        assertEquals("enum('success','failed')", columnType);
    }

    @Test
    void loginLogsForeignKeyReferencesUsers() {
        List<Map<String, Object>> fks = jdbc.queryForList(
                "SELECT REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND REFERENCED_TABLE_NAME IS NOT NULL");
        assertEquals(1, fks.size());
        assertEquals("users", fks.get(0).get("REFERENCED_TABLE_NAME"));
        assertEquals("id", fks.get(0).get("REFERENCED_COLUMN_NAME"));
    }

    @Test
    void loginLogsForeignKeySetNullOnDelete() {
        String deleteRule = jdbc.queryForObject(
                "SELECT DELETE_RULE FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS " +
                "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs'",
                String.class);
        assertEquals("SET NULL", deleteRule);
    }

    @Test
    void loginLogsEnumAcceptsSuccessAndFailed() {
        jdbc.update("INSERT INTO login_logs (username, login_type, ip_address) VALUES ('enum_test', 'success', '127.0.0.1')");
        jdbc.update("INSERT INTO login_logs (username, login_type, ip_address) VALUES ('enum_test', 'failed', '127.0.0.1')");
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'enum_test'", Integer.class);
        assertEquals(2, count);
        jdbc.update("DELETE FROM login_logs WHERE username = 'enum_test'");
    }

    @Test
    void loginLogsEnumRejectsInvalidValue() {
        assertThrows(org.springframework.dao.DataAccessException.class, () ->
                jdbc.update("INSERT INTO login_logs (username, login_type, ip_address) VALUES ('enum_test', 'invalid', '127.0.0.1')"));
    }

    @Test
    void loginLogsSetNullOnUserDelete() {
        // Create test user
        jdbc.update("INSERT INTO users (username, email, hashed_password, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_setnull', 'setnull@test.com', 'hash', 1, TRUE, FALSE, 0)");
        Integer testUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_setnull'", Integer.class);

        jdbc.update("INSERT INTO login_logs (user_id, username, login_type, ip_address) VALUES (?, 'test_setnull', 'success', '127.0.0.1')", testUserId);

        // Delete user
        jdbc.update("DELETE FROM users WHERE id = ?", testUserId);

        // Verify log remains but user_id is NULL
        List<Map<String, Object>> logs = jdbc.queryForList(
                "SELECT user_id, username FROM login_logs WHERE username = 'test_setnull'");
        assertEquals(1, logs.size(), "Login log should still exist after user deletion");
        assertNull(logs.get(0).get("user_id"), "user_id should be NULL after user deletion");
        assertEquals("test_setnull", logs.get(0).get("username"), "username should be preserved");

        jdbc.update("DELETE FROM login_logs WHERE username = 'test_setnull'");
    }

    @Test
    void loginLogsAllowsNullUserId() {
        jdbc.update("INSERT INTO login_logs (user_id, username, login_type, ip_address) VALUES (NULL, 'unknown_user', 'failed', '127.0.0.1')");
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'unknown_user'", Integer.class);
        assertEquals(1, count);
        jdbc.update("DELETE FROM login_logs WHERE username = 'unknown_user'");
    }

    @Test
    void loginLogsIpv6AddressStorage() {
        String ipv4 = "192.168.1.1";
        String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        jdbc.update("INSERT INTO login_logs (username, login_type, ip_address) VALUES ('ipv_test', 'success', ?)", ipv4);
        jdbc.update("INSERT INTO login_logs (username, login_type, ip_address) VALUES ('ipv_test', 'success', ?)", ipv6);

        List<String> addresses = jdbc.queryForList(
                "SELECT ip_address FROM login_logs WHERE username = 'ipv_test' ORDER BY id",
                String.class);
        assertEquals(2, addresses.size());
        assertEquals(ipv4, addresses.get(0));
        assertEquals(ipv6, addresses.get(1));

        jdbc.update("DELETE FROM login_logs WHERE username = 'ipv_test'");
    }

    @Test
    void loginLogsIndexOnUserId() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'user_id'");
        assertFalse(indexes.isEmpty(), "Index on login_logs.user_id should exist");
    }

    @Test
    void loginLogsIndexOnUsername() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'username' AND INDEX_NAME LIKE '%username%'");
        assertFalse(indexes.isEmpty(), "Index on login_logs.username should exist");
    }

    @Test
    void loginLogsIndexOnCreatedAt() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'created_at' AND INDEX_NAME LIKE '%created_at'");
        assertFalse(indexes.isEmpty(), "Index on login_logs.created_at should exist");
    }

    @Test
    void loginLogsIndexOnLoginType() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND COLUMN_NAME = 'login_type'");
        assertFalse(indexes.isEmpty(), "Index on login_logs.login_type should exist");
    }

    @Test
    void loginLogsCompositeIndexOnUserIdCreatedAt() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'login_logs' AND INDEX_NAME = 'idx_login_logs_user_id_created_at'");
        assertFalse(indexes.isEmpty(), "Composite index on (user_id, created_at) should exist");
    }

    // --- V4: sales_forecast_config tests ---

    @Test
    void salesForecastConfigTableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config'",
                Integer.class);
        assertEquals(1, count, "sales_forecast_config table should exist");
    }

    @Test
    void salesForecastConfigTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(7, columns.size(), "sales_forecast_config table should have 7 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "month", "auto_close_day", "is_closed", "closed_at", "created_at", "updated_at")));
    }

    @Test
    void salesForecastConfigMonthIsVarchar7() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config' AND COLUMN_NAME = 'month'",
                String.class);
        assertEquals("varchar(7)", columnType);
    }

    @Test
    void salesForecastConfigAutoCloseDayIsInt() {
        String dataType = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config' AND COLUMN_NAME = 'auto_close_day'",
                String.class);
        assertEquals("int", dataType);
    }

    @Test
    void salesForecastConfigUniqueConstraintOnMonth() {
        jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202501')");
        try {
            assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                    jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202501')"));
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month = '202501'");
        }
    }

    @Test
    void salesForecastConfigDefaultValues() {
        jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202502')");
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT auto_close_day, is_closed, closed_at, created_at, updated_at FROM sales_forecast_config WHERE month = '202502'");
            assertEquals(10, ((Number) row.get("auto_close_day")).intValue(), "auto_close_day should default to 10");
            assertFalse((Boolean) row.get("is_closed"), "is_closed should default to FALSE");
            assertNull(row.get("closed_at"), "closed_at should default to NULL");
            assertNotNull(row.get("created_at"), "created_at should be auto-populated");
            assertNotNull(row.get("updated_at"), "updated_at should be auto-populated");
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month = '202502'");
        }
    }

    @Test
    void salesForecastConfigClosedAtAcceptsNull() {
        jdbc.update("INSERT INTO sales_forecast_config (month, closed_at) VALUES ('202503', NULL)");
        try {
            Object closedAt = jdbc.queryForObject(
                    "SELECT closed_at FROM sales_forecast_config WHERE month = '202503'", Object.class);
            assertNull(closedAt);

            // Update to non-null
            jdbc.update("UPDATE sales_forecast_config SET closed_at = CURRENT_TIMESTAMP WHERE month = '202503'");
            Object updatedClosedAt = jdbc.queryForObject(
                    "SELECT closed_at FROM sales_forecast_config WHERE month = '202503'", Object.class);
            assertNotNull(updatedClosedAt);

            // Update back to null
            jdbc.update("UPDATE sales_forecast_config SET closed_at = NULL WHERE month = '202503'");
            Object reNulled = jdbc.queryForObject(
                    "SELECT closed_at FROM sales_forecast_config WHERE month = '202503'", Object.class);
            assertNull(reNulled);
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month = '202503'");
        }
    }

    @Test
    void salesForecastConfigAutoUpdateTimestamp() throws InterruptedException {
        jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202504')");
        try {
            Thread.sleep(1100); // Wait for timestamp granularity
            jdbc.update("UPDATE sales_forecast_config SET is_closed = TRUE WHERE month = '202504'");

            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT created_at, updated_at FROM sales_forecast_config WHERE month = '202504'");
            java.sql.Timestamp createdAt = (java.sql.Timestamp) row.get("created_at");
            java.sql.Timestamp updatedAt = (java.sql.Timestamp) row.get("updated_at");
            assertTrue(updatedAt.after(createdAt), "updated_at should be after created_at after update");
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month = '202504'");
        }
    }

    @Test
    void salesForecastConfigIndexOnIsClosed() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config' AND COLUMN_NAME = 'is_closed'");
        assertFalse(indexes.isEmpty(), "Index on is_closed should exist");
    }

    @Test
    void salesForecastConfigIndexOnAutoCloseDay() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast_config' AND COLUMN_NAME = 'auto_close_day'");
        assertFalse(indexes.isEmpty(), "Index on auto_close_day should exist");
    }

    @Test
    void salesForecastConfigCheckConstraintOnAutoCloseDay() {
        // Valid values
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day) VALUES ('202505', 1)");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day) VALUES ('202506', 31)");
        try {
            // Invalid values should fail
            assertThrows(org.springframework.dao.DataAccessException.class, () ->
                    jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day) VALUES ('202507', 0)"));
            assertThrows(org.springframework.dao.DataAccessException.class, () ->
                    jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day) VALUES ('202508', 32)"));
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month IN ('202505', '202506', '202507', '202508')");
        }
    }

    @Test
    void salesForecastConfigIdIsAutoIncrement() {
        jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202509')");
        jdbc.update("INSERT INTO sales_forecast_config (month) VALUES ('202510')");
        try {
            List<Integer> ids = jdbc.queryForList(
                    "SELECT id FROM sales_forecast_config WHERE month IN ('202509', '202510') ORDER BY id",
                    Integer.class);
            assertEquals(2, ids.size());
            assertTrue(ids.get(1) > ids.get(0), "IDs should auto-increment");
        } finally {
            jdbc.update("DELETE FROM sales_forecast_config WHERE month IN ('202509', '202510')");
        }
    }

    // --- V10: material_demand tests ---

    @Test
    void materialDemandTableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand'",
                Integer.class);
        assertEquals(1, count, "material_demand table should exist");
    }

    @Test
    void materialDemandTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(13, columns.size(), "material_demand table should have 13 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "week_start", "factory", "material_code", "material_name", "unit",
                "last_purchase_date", "demand_date", "expected_delivery", "demand_quantity",
                "estimated_inventory", "created_at", "updated_at")));
    }

    @Test
    void materialDemandIdIsAutoIncrement() {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date) " +
                "VALUES ('2026-02-17', 'F1', 'M001', '原料A', 'kg', '2026-02-20')");
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date) " +
                "VALUES ('2026-02-17', 'F2', 'M002', '原料B', 'pcs', '2026-02-21')");
        try {
            List<Integer> ids = jdbc.queryForList(
                    "SELECT id FROM material_demand WHERE material_code IN ('M001', 'M002') ORDER BY id",
                    Integer.class);
            assertEquals(2, ids.size());
            assertTrue(ids.get(1) > ids.get(0), "IDs should auto-increment");
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code IN ('M001', 'M002')");
        }
    }

    @Test
    void materialDemandCompositeIndexOnWeekStartFactory() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT DISTINCT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' " +
                "AND INDEX_NAME = 'idx_material_demand_week_factory'");
        assertFalse(indexes.isEmpty(), "Composite index on (week_start, factory) should exist");

        // Verify the index contains both columns in correct order
        List<String> indexColumns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' " +
                "AND INDEX_NAME = 'idx_material_demand_week_factory' ORDER BY SEQ_IN_INDEX",
                String.class);
        assertEquals(List.of("week_start", "factory"), indexColumns);
    }

    @Test
    void materialDemandIndexOnMaterialCode() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' " +
                "AND INDEX_NAME = 'idx_material_demand_code'");
        assertFalse(indexes.isEmpty(), "Index on material_code should exist");
    }

    @Test
    void materialDemandDecimalPrecision() {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date, " +
                "expected_delivery, demand_quantity, estimated_inventory) " +
                "VALUES ('2026-02-17', 'F1', 'M_DEC', '測試精度', 'kg', '2026-02-20', 123.45, 9999.99, 0.50)");
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT expected_delivery, demand_quantity, estimated_inventory FROM material_demand WHERE material_code = 'M_DEC'");
            assertEquals("123.45", row.get("expected_delivery").toString());
            assertEquals("9999.99", row.get("demand_quantity").toString());
            assertEquals("0.50", row.get("estimated_inventory").toString());
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code = 'M_DEC'");
        }
    }

    @Test
    void materialDemandDecimalColumnTypesAreDECIMAL10_2() {
        String expectedDeliveryType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'expected_delivery'",
                String.class);
        assertEquals("decimal(10,2)", expectedDeliveryType);

        String demandQuantityType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'demand_quantity'",
                String.class);
        assertEquals("decimal(10,2)", demandQuantityType);

        String estimatedInventoryType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'estimated_inventory'",
                String.class);
        assertEquals("decimal(10,2)", estimatedInventoryType);
    }

    @Test
    void materialDemandDefaultValues() {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date) " +
                "VALUES ('2026-02-17', 'F1', 'M_DEF', '測試預設值', 'kg', '2026-02-20')");
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT expected_delivery, demand_quantity, estimated_inventory FROM material_demand WHERE material_code = 'M_DEF'");
            assertEquals("0.00", row.get("expected_delivery").toString(), "expected_delivery should default to 0");
            assertEquals("0.00", row.get("demand_quantity").toString(), "demand_quantity should default to 0");
            assertEquals("0.00", row.get("estimated_inventory").toString(), "estimated_inventory should default to 0");
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code = 'M_DEF'");
        }
    }

    @Test
    void materialDemandLastPurchaseDateNullable() {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date, last_purchase_date) " +
                "VALUES ('2026-02-17', 'F1', 'M_NULL', '測試NULL', 'kg', '2026-02-20', NULL)");
        try {
            Object lastPurchaseDate = jdbc.queryForObject(
                    "SELECT last_purchase_date FROM material_demand WHERE material_code = 'M_NULL'", Object.class);
            assertNull(lastPurchaseDate, "last_purchase_date should accept NULL");
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code = 'M_NULL'");
        }
    }

    @Test
    void materialDemandNotNullConstraints() {
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO material_demand (factory, material_code, material_name, unit, demand_date) " +
                        "VALUES ('F1', 'M_ERR', 'Test', 'kg', '2026-02-20')"),
                "Should reject NULL week_start");

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO material_demand (week_start, material_code, material_name, unit, demand_date) " +
                        "VALUES ('2026-02-17', 'M_ERR', 'Test', 'kg', '2026-02-20')"),
                "Should reject NULL factory");

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO material_demand (week_start, factory, material_name, unit, demand_date) " +
                        "VALUES ('2026-02-17', 'F1', 'Test', 'kg', '2026-02-20')"),
                "Should reject NULL material_code");

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit) " +
                        "VALUES ('2026-02-17', 'F1', 'M_ERR', 'Test', 'kg')"),
                "Should reject NULL demand_date");
    }

    @Test
    void materialDemandTimestampDefaults() {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date) " +
                "VALUES ('2026-02-17', 'F1', 'M_TS', '測試時間戳', 'kg', '2026-02-20')");
        try {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT created_at, updated_at FROM material_demand WHERE material_code = 'M_TS'");
            assertNotNull(row.get("created_at"), "created_at should be auto-populated");
            assertNotNull(row.get("updated_at"), "updated_at should be auto-populated");
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code = 'M_TS'");
        }
    }

    @Test
    void materialDemandTimestampAutoUpdate() throws InterruptedException {
        jdbc.update("INSERT INTO material_demand (week_start, factory, material_code, material_name, unit, demand_date) " +
                "VALUES ('2026-02-17', 'F1', 'M_UPD', '測試更新', 'kg', '2026-02-20')");
        try {
            Thread.sleep(1100); // Wait for timestamp granularity
            jdbc.update("UPDATE material_demand SET demand_quantity = 100.00 WHERE material_code = 'M_UPD'");

            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT created_at, updated_at FROM material_demand WHERE material_code = 'M_UPD'");
            java.sql.Timestamp createdAt = (java.sql.Timestamp) row.get("created_at");
            java.sql.Timestamp updatedAt = (java.sql.Timestamp) row.get("updated_at");
            assertTrue(updatedAt.after(createdAt), "updated_at should be after created_at after update");
        } finally {
            jdbc.update("DELETE FROM material_demand WHERE material_code = 'M_UPD'");
        }
    }

    @Test
    void materialDemandVarcharColumnTypes() {
        String factoryType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'factory'",
                String.class);
        assertEquals("varchar(50)", factoryType);

        String materialCodeType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'material_code'",
                String.class);
        assertEquals("varchar(50)", materialCodeType);

        String materialNameType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'material_name'",
                String.class);
        assertEquals("varchar(200)", materialNameType);

        String unitType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'material_demand' AND COLUMN_NAME = 'unit'",
                String.class);
        assertEquals("varchar(20)", unitType);
    }
}
