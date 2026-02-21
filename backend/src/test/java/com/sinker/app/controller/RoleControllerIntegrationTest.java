package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.role.UpdateRoleRequest;
import com.sinker.app.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbc;

    private String adminToken;

    @BeforeEach
    void setUp() {
        Long adminId = jdbc.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");
    }

    // --- GET /api/roles ---

    @Test
    void listRolesSuccess() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.roles", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.roles[0].id").isNumber())
                .andExpect(jsonPath("$.roles[0].code").isString())
                .andExpect(jsonPath("$.roles[0].name").isString())
                .andExpect(jsonPath("$.roles[0].isSystem").isBoolean())
                .andExpect(jsonPath("$.roles[0].isActive").isBoolean());
    }

    @Test
    void listRolesContainsSeedRoles() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[*].code",
                        hasItems("admin", "sales", "production_planner", "procurement")));
    }

    @Test
    void listRolesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listRolesForbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    // --- GET /api/roles/:id ---

    @Test
    void getRoleByIdSuccess() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);

        mockMvc.perform(get("/api/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminRoleId))
                .andExpect(jsonPath("$.code").value("admin"))
                .andExpect(jsonPath("$.name").value("Administrator"))
                .andExpect(jsonPath("$.isSystem").value(true))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions", hasSize(greaterThanOrEqualTo(29))))
                .andExpect(jsonPath("$.permissionsByModule").isMap());
    }

    @Test
    void getRoleByIdPermissionsGroupedByModule() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);

        mockMvc.perform(get("/api/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionsByModule.user").isArray())
                .andExpect(jsonPath("$.permissionsByModule.role").isArray())
                .andExpect(jsonPath("$.permissionsByModule.user", hasSize(4)))
                .andExpect(jsonPath("$.permissionsByModule.role", hasSize(4)));
    }

    @Test
    void getRoleByIdPermissionFields() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);

        mockMvc.perform(get("/api/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0].id").isNumber())
                .andExpect(jsonPath("$.permissions[0].code").isString())
                .andExpect(jsonPath("$.permissions[0].name").isString())
                .andExpect(jsonPath("$.permissions[0].module").isString());
    }

    @Test
    void getRoleByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/roles/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    // --- PUT /api/roles/:id ---

    @Test
    void updateRoleNameAndDescription() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);
        String originalName = jdbc.queryForObject(
                "SELECT name FROM roles WHERE id = ?", String.class, salesRoleId);

        try {
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setName("Sales Updated");
            request.setDescription("Updated sales description");

            mockMvc.perform(put("/api/roles/" + salesRoleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Sales Updated"))
                    .andExpect(jsonPath("$.description").value("Updated sales description"))
                    .andExpect(jsonPath("$.code").value("sales"));
        } finally {
            jdbc.update("UPDATE roles SET name = ?, description = 'Sales forecast management' WHERE id = ?",
                    originalName, salesRoleId);
        }
    }

    @Test
    void updateRolePermissions() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);

        // Save original permissions
        List<Long> originalPermIds = jdbc.queryForList(
                "SELECT permission_id FROM role_permissions WHERE role_id = ?",
                Long.class, salesRoleId);

        try {
            // Get first two permission IDs
            List<Long> permIds = jdbc.queryForList(
                    "SELECT id FROM permissions ORDER BY id LIMIT 2", Long.class);

            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setPermissionIds(permIds);

            mockMvc.perform(put("/api/roles/" + salesRoleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissions", hasSize(permIds.size())));

            // Verify DB
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM role_permissions WHERE role_id = ?",
                    Integer.class, salesRoleId);
            assertEquals(permIds.size(), count);
        } finally {
            // Restore original permissions
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", salesRoleId);
            for (Long permId : originalPermIds) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        salesRoleId, permId);
            }
        }
    }

    @Test
    void updateRoleRemoveAllPermissions() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);

        List<Long> originalPermIds = jdbc.queryForList(
                "SELECT permission_id FROM role_permissions WHERE role_id = ?",
                Long.class, salesRoleId);

        try {
            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setPermissionIds(List.of());

            mockMvc.perform(put("/api/roles/" + salesRoleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permissions", hasSize(0)));
        } finally {
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", salesRoleId);
            for (Long permId : originalPermIds) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        salesRoleId, permId);
            }
        }
    }

    @Test
    void updateRoleWithInvalidPermissionIds() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setPermissionIds(List.of(1L, 999L));

        mockMvc.perform(put("/api/roles/" + adminRoleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("999")));
    }

    @Test
    void updateRoleNotFound() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        mockMvc.perform(put("/api/roles/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRoleUnauthorized() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        mockMvc.perform(put("/api/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRoleForbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        mockMvc.perform(put("/api/roles/1")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRolePermissionsOnlyKeepsNameUnchanged() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);
        String originalName = jdbc.queryForObject(
                "SELECT name FROM roles WHERE id = ?", String.class, salesRoleId);

        List<Long> originalPermIds = jdbc.queryForList(
                "SELECT permission_id FROM role_permissions WHERE role_id = ?",
                Long.class, salesRoleId);

        try {
            List<Long> permIds = jdbc.queryForList(
                    "SELECT id FROM permissions ORDER BY id LIMIT 3", Long.class);

            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setPermissionIds(permIds);

            mockMvc.perform(put("/api/roles/" + salesRoleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value(originalName))
                    .andExpect(jsonPath("$.permissions", hasSize(permIds.size())));
        } finally {
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", salesRoleId);
            for (Long permId : originalPermIds) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        salesRoleId, permId);
            }
        }
    }

    @Test
    void updateSystemRoleCanUpdateNameAndPermissions() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);
        String originalName = jdbc.queryForObject(
                "SELECT name FROM roles WHERE id = ?", String.class, adminRoleId);

        List<Long> originalPermIds = jdbc.queryForList(
                "SELECT permission_id FROM role_permissions WHERE role_id = ?",
                Long.class, adminRoleId);

        try {
            List<Long> permIds = jdbc.queryForList(
                    "SELECT id FROM permissions ORDER BY id LIMIT 5", Long.class);

            UpdateRoleRequest request = new UpdateRoleRequest();
            request.setName("Admin Updated");
            request.setPermissionIds(permIds);

            mockMvc.perform(put("/api/roles/" + adminRoleId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Admin Updated"))
                    .andExpect(jsonPath("$.isSystem").value(true))
                    .andExpect(jsonPath("$.code").value("admin"))
                    .andExpect(jsonPath("$.permissions", hasSize(permIds.size())));
        } finally {
            jdbc.update("UPDATE roles SET name = ? WHERE id = ?", originalName, adminRoleId);
            jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", adminRoleId);
            for (Long permId : originalPermIds) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                        adminRoleId, permId);
            }
        }
    }

    private static void assertEquals(int expected, Integer actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
