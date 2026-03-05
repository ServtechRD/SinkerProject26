package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.auth.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetAdminState() {
        // Reset admin user state before each test
        jdbc.update("UPDATE users SET failed_login_count = 0, is_locked = FALSE WHERE username = 'admin'");
        jdbc.update("DELETE FROM login_logs");
    }

    // --- Login by username ---

    @Test
    void loginSuccessReturnsTokenAndUserInfo() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.email").value("admin@sinker.local"))
                .andExpect(jsonPath("$.user.fullName").value("System Administrator"))
                .andExpect(jsonPath("$.user.roleCode").value("admin"));
    }

    @Test
    void loginSuccessUpdatesLastLoginAt() throws Exception {
        jdbc.update("UPDATE users SET last_login_at = NULL WHERE username = 'admin'");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());

        String lastLogin = jdbc.queryForObject(
                "SELECT last_login_at FROM users WHERE username = 'admin'", String.class);
        assertNotNull(lastLogin, "last_login_at should be updated");
    }

    @Test
    void loginInvalidUsernameReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nonexistent", "password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginInvalidPasswordReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrongpassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginMissingUsernameAndEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginMissingPasswordReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginEmptyBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginTokenIsValidJwtFormat() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("token").asText();
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
    }

    // --- Login by email ---

    @Test
    void loginByEmailReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@sinker.local\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void loginByInvalidEmailReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@sinker.local\",\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    // --- Protected endpoint tests ---

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointWithValidTokenReturns200() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void loginEndpointDoesNotRequireJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());
    }

    @Test
    void optionsRequestNotBlockedByJwtFilter() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk());
    }

    // --- T010: Login Logging Integration Tests ---

    @Test
    void successfulLoginCreatesLogEntry() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "TestClient/1.0")
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'admin' AND login_type = 'success'",
                Integer.class);
        assertEquals(1, count);

        String ua = jdbc.queryForObject(
                "SELECT user_agent FROM login_logs WHERE username = 'admin' AND login_type = 'success' ORDER BY created_at DESC LIMIT 1",
                String.class);
        assertEquals("TestClient/1.0", ua);
    }

    @Test
    void failedLoginCreatesLogEntry() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'admin' AND login_type = 'failed'",
                Integer.class);
        assertEquals(1, count);

        String reason = jdbc.queryForObject(
                "SELECT failed_reason FROM login_logs WHERE username = 'admin' AND login_type = 'failed' ORDER BY created_at DESC LIMIT 1",
                String.class);
        assertEquals("Invalid username or password", reason);
    }

    @Test
    void nonExistentUserLoginCreatesLogWithNullUserId() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ghost_user", "password"))))
                .andExpect(status().isUnauthorized());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'ghost_user' AND login_type = 'failed'",
                Integer.class);
        assertEquals(1, count);

        Object userId = jdbc.queryForObject(
                "SELECT user_id FROM login_logs WHERE username = 'ghost_user' ORDER BY created_at DESC LIMIT 1",
                Object.class);
        assertNull(userId);
    }

    @Test
    void successfulLoginResetsFailedCount() throws Exception {
        jdbc.update("UPDATE users SET failed_login_count = 3 WHERE username = 'admin'");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());

        Integer failedCount = jdbc.queryForObject(
                "SELECT failed_login_count FROM users WHERE username = 'admin'", Integer.class);
        assertEquals(0, failedCount);
    }

    @Test
    void failedLoginIncrementsFailedCount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isUnauthorized());

        Integer failedCount = jdbc.queryForObject(
                "SELECT failed_login_count FROM users WHERE username = 'admin'", Integer.class);
        assertEquals(1, failedCount);
    }

    @Test
    void lockoutAfter5FailedAttempts() throws Exception {
        jdbc.update("UPDATE users SET failed_login_count = 4 WHERE username = 'admin'");

        // 5th failed attempt should lock the account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is locked"));

        Boolean isLocked = jdbc.queryForObject(
                "SELECT is_locked FROM users WHERE username = 'admin'", Boolean.class);
        assertTrue(isLocked);

        Integer failedCount = jdbc.queryForObject(
                "SELECT failed_login_count FROM users WHERE username = 'admin'", Integer.class);
        assertEquals(5, failedCount);
    }

    @Test
    void lockedAccountReturns403WithCorrectPassword() throws Exception {
        jdbc.update("UPDATE users SET is_locked = TRUE, failed_login_count = 5 WHERE username = 'admin'");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is locked"));

        // Verify log entry created
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM login_logs WHERE username = 'admin' AND failed_reason = 'Account is locked'",
                Integer.class);
        assertEquals(1, count);
    }

    @Test
    void ipAddressLoggedFromXForwardedFor() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.50")
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());

        String ip = jdbc.queryForObject(
                "SELECT ip_address FROM login_logs WHERE username = 'admin' ORDER BY created_at DESC LIMIT 1",
                String.class);
        assertEquals("203.0.113.50", ip);
    }

    @Test
    void userAgentLoggedCorrectly() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 Custom")
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk());

        String ua = jdbc.queryForObject(
                "SELECT user_agent FROM login_logs WHERE username = 'admin' ORDER BY created_at DESC LIMIT 1",
                String.class);
        assertEquals("Mozilla/5.0 Custom", ua);
    }
}
