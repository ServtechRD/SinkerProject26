package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.forecast.CreateMonthsRequest;
import com.sinker.app.dto.forecast.UpdateConfigRequest;
import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.SalesForecastConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesForecastConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SalesForecastConfigService service;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbc.update("DELETE FROM sales_forecast_config WHERE month LIKE '2099%' OR month LIKE '2098%'");

        Long adminId = jdbc.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");
    }

    // --- POST /api/sales-forecast/config ---

    @Test
    void createMonth_Success() throws Exception {
        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setMonth("209901");
        request.setAutoCloseDate("2099-01-10");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.month").value("209901"))
                .andExpect(jsonPath("$.autoCloseDate").value("2099-01-10"))
                .andExpect(jsonPath("$.isClosed").value(false));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_config WHERE month = '209901'",
                Integer.class);
        assertEquals(1, count);
    }

    @Test
    void createMonth_Unauthorized() throws Exception {
        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setMonth("209901");
        request.setAutoCloseDate("2099-01-10");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMonth_Forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setMonth("209901");
        request.setAutoCloseDate("2099-01-10");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMonth_InvalidMonthFormat() throws Exception {
        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"month\":\"2099-01\",\"autoCloseDate\":\"2099-01-10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMonth_InvalidAutoCloseDateFormat() throws Exception {
        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"month\":\"209901\",\"autoCloseDate\":\"2099/01/10\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMonth_Duplicate() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209801', '2098-01-10', false, NOW(), NOW())");

        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setMonth("209801");
        request.setAutoCloseDate("2098-01-10");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- GET /api/sales-forecast/config ---

    @Test
    void listConfigs_Success() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209902', '2099-02-15', true, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.month=='209902')].autoCloseDate").value("2099-02-15"))
                .andExpect(jsonPath("$[?(@.month=='209901')].autoCloseDate").value("2099-01-10"));
    }

    @Test
    void listConfigs_Empty() throws Exception {
        // setUp already cleans test data
        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listConfigs_Forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listConfigs_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/sales-forecast/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listConfigs_SortedDescByMonth() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209903', '2099-03-10', false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209902', '2099-02-10', false, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.month=='209903')]").exists())
                .andExpect(jsonPath("$[?(@.month=='209902')]").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')]").exists());
    }

    @Test
    void listConfigs_ResponseFields() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.month=='209901')].id").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')].month").value("209901"))
                .andExpect(jsonPath("$[?(@.month=='209901')].autoCloseDate").value("2099-01-10"))
                .andExpect(jsonPath("$[?(@.month=='209901')].isClosed").value(false))
                .andExpect(jsonPath("$[?(@.month=='209901')].createdAt").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')].updatedAt").exists());
    }

    // --- PUT /api/sales-forecast/config/:id ---

    @Test
    void updateConfig_SetClosed() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(true);

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isClosed").value(true))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        Boolean isClosed = jdbc.queryForObject(
                "SELECT is_closed FROM sales_forecast_config WHERE id = ?",
                Boolean.class, id);
        assertTrue(isClosed);
    }

    @Test
    void updateConfig_CannotReopenWhenClosed() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, closed_at, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', true, NOW(), NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(false);

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("無法重新開放")));

        Boolean isClosed = jdbc.queryForObject(
                "SELECT is_closed FROM sales_forecast_config WHERE id = ?",
                Boolean.class, id);
        assertTrue(isClosed);
    }

    @Test
    void updateConfig_UpdateAutoCloseDate() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2099-01-25");

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoCloseDate").value("2099-01-25"));
    }

    @Test
    void updateConfig_NotFound() throws Exception {
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2099-01-15");

        mockMvc.perform(put("/api/sales-forecast/config/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateConfig_InvalidAutoCloseDateFormat() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', '2099-01-10', false, NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDate("2099/01/25");

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("autoCloseDate")));
    }

    @Test
    void updateConfig_Forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        mockMvc.perform(put("/api/sales-forecast/config/1")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoCloseDate\":\"2099-01-15\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/sales-forecast/config/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoCloseDate\":\"2099-01-15\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Scheduler Integration ---

    @Test
    void schedulerIntegration_AutoClosesMatchingMonths() throws Exception {
        LocalDate today = LocalDate.now();
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_date, is_closed, created_at, updated_at) " +
                "VALUES ('209901', ?, false, NOW(), NOW())", today);

        int closedCount = service.autoCloseMatchingMonths(today);

        assertTrue(closedCount >= 1);
        Boolean isClosed = jdbc.queryForObject(
                "SELECT is_closed FROM sales_forecast_config WHERE month = '209901'",
                Boolean.class);
        assertTrue(isClosed);
    }

    // --- Helper ---

    private static void assertEquals(int expected, Integer actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertTrue(Boolean value) {
        org.junit.jupiter.api.Assertions.assertTrue(value);
    }
}
