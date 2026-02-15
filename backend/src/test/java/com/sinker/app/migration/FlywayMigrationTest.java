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
        assertTrue(history.size() >= 2, "At least V1 and V2 migrations should exist");

        Map<String, Object> v1 = history.get(0);
        assertEquals("1", v1.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v1.get("success")) || Integer.valueOf(1).equals(v1.get("success")),
                "V1 migration should be successful");

        Map<String, Object> v2 = history.get(1);
        assertEquals("2", v2.get("version").toString());
        assertTrue(Boolean.TRUE.equals(v2.get("success")) || Integer.valueOf(1).equals(v2.get("success")),
                "V2 migration should be successful");
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
}
