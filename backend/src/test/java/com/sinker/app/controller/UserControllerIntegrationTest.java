package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.user.CreateUserRequest;
import com.sinker.app.dto.user.UpdateUserRequest;
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
class UserControllerIntegrationTest {

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

        // Clean up test users from previous runs
        jdbc.update("DELETE FROM users WHERE username LIKE 'testuser_%'");
    }

    // --- GET /api/users - List ---

    @Test
    void listUsersSuccess() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    void listUsersUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsersForbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsersWithSearch() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("keyword", "admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].username").value("admin"));
    }

    @Test
    void listUsersWithRoleFilter() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);
        mockMvc.perform(get("/api/users")
                        .param("roleId", adminRoleId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users[*].role.code", everyItem(is("admin"))));
    }

    @Test
    void listUsersWithPagination() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(1)));
    }

    @Test
    void listUsersDefaultPageSize() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    void listUsersDoNotIncludeHashedPassword() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].hashedPassword").doesNotExist());
    }

    // --- GET /api/users/:id ---

    @Test
    void getUserByIdSuccess() throws Exception {
        Long adminId = jdbc.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'", Long.class);

        mockMvc.perform(get("/api/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@sinker.local"))
                .andExpect(jsonPath("$.role.code").value("admin"))
                .andExpect(jsonPath("$.role.id").isNumber())
                .andExpect(jsonPath("$.role.name").value("Administrator"))
                .andExpect(jsonPath("$.hashedPassword").doesNotExist());
    }

    @Test
    void getUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    // --- POST /api/users ---

    @Test
    void createUserSuccess() throws Exception {
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser_create");
        request.setEmail("testuser_create@sinker.local");
        request.setPassword("password123");
        request.setFullName("Test Create User");
        request.setRoleId(adminRoleId);
        request.setDepartment("Engineering");
        request.setPhone("1234567890");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("testuser_create"))
                .andExpect(jsonPath("$.email").value("testuser_create@sinker.local"))
                .andExpect(jsonPath("$.fullName").value("Test Create User"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.hashedPassword").doesNotExist());
    }

    @Test
    void createUserDuplicateUsernameReturns409() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin");
        request.setEmail("unique@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Username already exists")));
    }

    @Test
    void createUserDuplicateEmailReturns409() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser_dupemail");
        request.setEmail("admin@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Email already exists")));
    }

    @Test
    void createUserValidationErrors() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUserSalesRoleWithoutChannelsReturns400() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser_salesnoch");
        request.setEmail("testuser_salesnoch@sinker.local");
        request.setPassword("password123");
        request.setRoleId(salesRoleId);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("channel")));
    }

    @Test
    void createUserSalesRoleWithChannelsSuccess() throws Exception {
        Long salesRoleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'sales'", Long.class);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser_salesch");
        request.setEmail("testuser_salesch@sinker.local");
        request.setPassword("password123");
        request.setRoleId(salesRoleId);
        request.setChannels(List.of("PX/大全聯", "家樂福"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("testuser_salesch"));
    }

    // --- PUT /api/users/:id ---

    @Test
    void updateUserSuccess() throws Exception {
        Long userId = createTestUserInDb("testuser_update");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated Name");
        request.setDepartment("New Department");

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.department").value("New Department"));
    }

    @Test
    void updateUserNotFound() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated");

        mockMvc.perform(put("/api/users/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserDuplicateUsername() throws Exception {
        Long userId = createTestUserInDb("testuser_updup");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("admin"); // already exists

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- DELETE /api/users/:id ---

    @Test
    void deleteUserSuccess() throws Exception {
        Long userId = createTestUserInDb("testuser_delete");

        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserNotFound() throws Exception {
        mockMvc.perform(delete("/api/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /api/users/:id/toggle ---

    @Test
    void toggleUserStatusDeactivates() throws Exception {
        Long userId = createTestUserInDb("testuser_toggle");

        mockMvc.perform(patch("/api/users/" + userId + "/toggle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void toggleUserStatusActivates() throws Exception {
        Long userId = createTestUserInDb("testuser_toggle2");
        jdbc.update("UPDATE users SET is_active = false WHERE id = ?", userId);

        mockMvc.perform(patch("/api/users/" + userId + "/toggle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void toggleUserStatusNotFound() throws Exception {
        mockMvc.perform(patch("/api/users/99999/toggle")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // --- Permission tests ---

    @Test
    void createUserForbiddenWithoutPermission() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("testuser_noperm");
        request.setEmail("noperm@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserForbiddenWithoutPermission() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        mockMvc.perform(put("/api/users/1")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUserForbiddenWithoutPermission() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        mockMvc.perform(delete("/api/users/1")
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    // --- Helper ---

    private Long createTestUserInDb(String username) {
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE code = 'admin'", Long.class);
        jdbc.update(
                "INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count, created_at, updated_at) " +
                "VALUES (?, ?, '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'Test User', ?, true, false, 0, NOW(), NOW())",
                username, username + "@sinker.local", roleId);
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE username = ?", Long.class, username);
    }
}
