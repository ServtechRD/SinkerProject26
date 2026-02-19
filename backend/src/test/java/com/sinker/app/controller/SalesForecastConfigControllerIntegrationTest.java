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
    void createMonths_Success() throws Exception {
        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setStartMonth("209901");
        request.setEndMonth("209903");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(3))
                .andExpect(jsonPath("$.months", hasSize(3)))
                .andExpect(jsonPath("$.months[0]").value("209901"))
                .andExpect(jsonPath("$.months[1]").value("209902"))
                .andExpect(jsonPath("$.months[2]").value("209903"));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_config WHERE month IN ('209901','209902','209903')",
                Integer.class);
        assertEquals(3, count);
    }

    @Test
    void createMonths_Unauthorized() throws Exception {
        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setStartMonth("209901");
        request.setEndMonth("209903");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMonths_Forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setStartMonth("209901");
        request.setEndMonth("209903");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createMonths_InvalidFormat() throws Exception {
        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startMonth\":\"2099-01\",\"endMonth\":\"209903\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMonths_StartAfterEnd() throws Exception {
        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setStartMonth("209903");
        request.setEndMonth("209901");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("start_month")));
    }

    @Test
    void createMonths_Duplicate() throws Exception {
        // First create
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209801', 10, false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209802', 10, false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209803', 10, false, NOW(), NOW())");

        CreateMonthsRequest request = new CreateMonthsRequest();
        request.setStartMonth("209801");
        request.setEndMonth("209803");

        mockMvc.perform(post("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- GET /api/sales-forecast/config ---

    @Test
    void listConfigs_Success() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209902', 15, true, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.month=='209902')].autoCloseDay").value(15))
                .andExpect(jsonPath("$[?(@.month=='209901')].autoCloseDay").value(10));
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
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209903', 10, false, NOW(), NOW())");
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209902', 10, false, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.month=='209903')]").exists())
                .andExpect(jsonPath("$[?(@.month=='209902')]").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')]").exists());
    }

    @Test
    void listConfigs_ResponseFields() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");

        mockMvc.perform(get("/api/sales-forecast/config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.month=='209901')].id").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')].month").value("209901"))
                .andExpect(jsonPath("$[?(@.month=='209901')].autoCloseDay").value(10))
                .andExpect(jsonPath("$[?(@.month=='209901')].isClosed").value(false))
                .andExpect(jsonPath("$[?(@.month=='209901')].createdAt").exists())
                .andExpect(jsonPath("$[?(@.month=='209901')].updatedAt").exists());
    }

    // --- PUT /api/sales-forecast/config/:id ---

    @Test
    void updateConfig_SetClosed() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");
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
    void updateConfig_SetOpen() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, closed_at, created_at, updated_at) " +
                "VALUES ('209901', 10, true, NOW(), NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setIsClosed(false);

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.closedAt").isEmpty());
    }

    @Test
    void updateConfig_UpdateAutoCloseDay() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(25);

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoCloseDay").value(25));
    }

    @Test
    void updateConfig_NotFound() throws Exception {
        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(15);

        mockMvc.perform(put("/api/sales-forecast/config/99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateConfig_InvalidAutoCloseDay() throws Exception {
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
                "VALUES ('209901', 10, false, NOW(), NOW())");
        Integer id = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_config WHERE month = '209901'", Integer.class);

        UpdateConfigRequest request = new UpdateConfigRequest();
        request.setAutoCloseDay(50);

        mockMvc.perform(put("/api/sales-forecast/config/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("auto_close_day")));
    }

    @Test
    void updateConfig_Forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");

        mockMvc.perform(put("/api/sales-forecast/config/1")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoCloseDay\":15}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/sales-forecast/config/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoCloseDay\":15}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Scheduler Integration ---

    @Test
    void schedulerIntegration_AutoClosesMatchingMonths() throws Exception {
        int today = java.time.LocalDate.now().getDayOfMonth();
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed, created_at, updated_at) " +
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
