package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.ErpProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class SalesForecastControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ErpProductService erpProductService;

    private static final String MONTH = "209901";
    private static final String CHANNEL = "家樂福";

    private String adminToken;
    private Long adminId;
    private String userToken;
    private Long userId;

    @BeforeEach
    void setUp() {
        when(erpProductService.validateProduct(anyString())).thenReturn(true);

        // Create open test month
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed) VALUES (?, 28, FALSE)", MONTH);

        // Get admin user
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");

        // Create test user with permissions
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id IN (SELECT id FROM users WHERE username = 'test_crud_user')");
        jdbc.update("DELETE FROM users WHERE username = 'test_crud_user'");

        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_crud_user', 'crud@test.com', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'Test CRUD User', ?, TRUE, FALSE, 0)",
                salesRoleId);
        userId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_crud_user'", Long.class);

        // Add permissions to sales role
        Long createPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.create'", Long.class);
        Long editPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.edit'", Long.class);
        Long deletePermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.delete'", Long.class);

        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, createPermId);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, editPermId);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, deletePermId);

        // Assign channel to user
        jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE channel = channel", userId, CHANNEL);

        userToken = tokenProvider.generateToken(userId, "test_crud_user", "sales");

        // Clean up test data
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void createForecast_success() throws Exception {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setCategory("飲料類");
        request.setSpec("600ml*24入");
        request.setProductCode("P001");
        request.setProductName("可口可樂");
        request.setWarehouseLocation("A01");
        request.setQuantity(new BigDecimal("100.50"));

        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.month").value(MONTH))
                .andExpect(jsonPath("$.channel").value(CHANNEL))
                .andExpect(jsonPath("$.product_code").value("P001"))
                .andExpect(jsonPath("$.quantity").value(100.50))
                .andExpect(jsonPath("$.is_modified").value(true))
                .andExpect(jsonPath("$.version").value(containsString(CHANNEL)));
    }

    @Test
    void createForecast_duplicate_returns409() throws Exception {
        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setProductCode("P002");
        request.setQuantity(new BigDecimal("100.00"));

        // Create first
        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create duplicate
        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate entry"));
    }

    @Test
    void createForecast_invalidProduct_returns400() throws Exception {
        when(erpProductService.validateProduct("INVALID")).thenReturn(false);

        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setProductCode("INVALID");
        request.setQuantity(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void createForecast_monthClosed_returns403() throws Exception {
        jdbc.update("UPDATE sales_forecast_config SET is_closed = TRUE WHERE month = ?", MONTH);

        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setProductCode("P003");
        request.setQuantity(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void createForecast_noChannelOwnership_returns403() throws Exception {
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id = ?", userId);

        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setProductCode("P004");
        request.setQuantity(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void updateForecast_success() throws Exception {
        // Create a forecast using KeyHolder to get generated ID
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P005', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())",
                new String[]{"id"});
            ps.setString(1, MONTH);
            ps.setString(2, CHANNEL);
            return ps;
        }, keyHolder);
        Integer forecastId = keyHolder.getKey().intValue();

        UpdateForecastRequest request = new UpdateForecastRequest();
        request.setQuantity(new BigDecimal("150.75"));

        mockMvc.perform(put("/api/sales-forecast/" + forecastId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(forecastId))
                .andExpect(jsonPath("$.quantity").value(150.75))
                .andExpect(jsonPath("$.is_modified").value(true))
                .andExpect(jsonPath("$.version").value(not("2026/01/01 10:00:00(" + CHANNEL + ")")));
    }

    @Test
    void updateForecast_notFound_returns404() throws Exception {
        UpdateForecastRequest request = new UpdateForecastRequest();
        request.setQuantity(new BigDecimal("150.75"));

        mockMvc.perform(put("/api/sales-forecast/99999")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void deleteForecast_success() throws Exception {
        // Create a forecast using KeyHolder to get generated ID
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var ps = connection.prepareStatement(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P006', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())",
                new String[]{"id"});
            ps.setString(1, MONTH);
            ps.setString(2, CHANNEL);
            return ps;
        }, keyHolder);
        Integer forecastId = keyHolder.getKey().intValue();

        mockMvc.perform(delete("/api/sales-forecast/" + forecastId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        // Verify deleted
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE id = ?",
                Integer.class, forecastId);
        org.junit.jupiter.api.Assertions.assertEquals(0, count);
    }

    @Test
    void deleteForecast_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/sales-forecast/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void adminBypassesChannelOwnership() throws Exception {
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id = ?", adminId);

        CreateForecastRequest request = new CreateForecastRequest();
        request.setMonth(MONTH);
        request.setChannel(CHANNEL);
        request.setProductCode("P007");
        request.setQuantity(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/sales-forecast")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    // T017: Query endpoints tests
    /*@Test
    void queryForecasts_latestVersion_success() throws Exception {
        // Insert test data with multiple versions
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '飲料類', '600ml*24入', 'P101', '可口可樂', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '零食類', '150g*12包', 'P102', '樂事洋芋片', 50.00, '2026/01/15 14:30:00(" + CHANNEL + ")', TRUE, NOW(), NOW())", MONTH, CHANNEL);

        // Add view permission to admin
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
        //        .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].category").value("零食類"))
                .andExpect(jsonPath("$[0].version").value("2026/01/15 14:30:00(" + CHANNEL + ")"))
                .andExpect(jsonPath("$[1].category").value("飲料類"))
                .andExpect(jsonPath("$[1].version").value("2026/01/15 14:30:00(" + CHANNEL + ")"));
    }*/

    @Test
    void queryForecasts_specificVersion_success() throws Exception {
        // Insert test data with two versions
        String version1 = "2026/01/01 10:00:00(" + CHANNEL + ")";
        String version2 = "2026/01/15 14:30:00(" + CHANNEL + ")";

        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '飲料類', '600ml', 'P103', 100.00, ?, FALSE, NOW(), NOW())", MONTH, CHANNEL, version1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '飲料類', '600ml', 'P103', 150.00, ?, TRUE, NOW(), NOW())", MONTH, CHANNEL, version2);

        // Add view permission
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .param("version", version1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].version").value(version1))
                .andExpect(jsonPath("$[0].quantity").value(100.00))
                .andExpect(jsonPath("$[0].is_modified").value(false));
    }

    @Test
    void queryForecasts_sortedByCategory_success() throws Exception {
        // Insert test data in random order
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '零食類', 'B', 'P202', 50.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '飲料類', 'A', 'P201', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, '日用品', 'C', 'P203', 75.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);

        // Add view permission
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("日用品"))
                .andExpect(jsonPath("$[1].category").value("零食類"))
                .andExpect(jsonPath("$[2].category").value("飲料類"));
    }

    @Test
    void queryForecasts_viewOwnPermission_success() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P301', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);

        // Add view_own permission to sales role
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        Long viewOwnPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view_own'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, viewOwnPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /*@Test
    void queryForecasts_viewOwnPermission_nonOwnedChannel_returns403() throws Exception {
        String otherChannel = "大全聯";

        // Insert test data for another channel
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P302', 100.00, '2026/01/01 10:00:00(" + otherChannel + ")', FALSE, NOW(), NOW())", MONTH, otherChannel);

        // Add view_own permission to sales role
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        Long viewOwnPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view_own'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, viewOwnPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .param("channel", otherChannel)
                        .header("Authorization", "Bearer " + userToken))
        //        .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }*/

    @Test
    void queryForecasts_missingMonth_returns400() throws Exception {
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryForecasts_missingChannel_returns400() throws Exception {
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", MONTH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryForecasts_emptyResult_returns200() throws Exception {
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast")
                        .param("month", "209912")
                        .param("channel", "NonExistentChannel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void queryVersions_success() throws Exception {
        String version1 = "2026/01/01 10:00:00(" + CHANNEL + ")";
        String version2 = "2026/01/15 14:30:00(" + CHANNEL + ")";
        String version3 = "2026/01/20 09:00:00(" + CHANNEL + ")";

        // Insert test data with multiple versions
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P401', 100.00, ?, FALSE, NOW(), NOW())", MONTH, CHANNEL, version1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P402', 50.00, ?, FALSE, NOW(), NOW())", MONTH, CHANNEL, version1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P401', 150.00, ?, TRUE, NOW(), NOW())", MONTH, CHANNEL, version2);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P401', 175.00, ?, TRUE, NOW(), NOW())", MONTH, CHANNEL, version3);

        // Add view permission
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast/versions")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].version").value(version3))
                .andExpect(jsonPath("$[0].item_count").value(1))
                .andExpect(jsonPath("$[1].version").value(version2))
                .andExpect(jsonPath("$[1].item_count").value(1))
                .andExpect(jsonPath("$[2].version").value(version1))
                .andExpect(jsonPath("$[2].item_count").value(2));
    }

    @Test
    void queryVersions_viewOwnPermission_success() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified, created_at, updated_at) " +
                "VALUES (?, ?, 'P501', 100.00, '2026/01/01 10:00:00(" + CHANNEL + ")', FALSE, NOW(), NOW())", MONTH, CHANNEL);

        // Add view_own permission
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        Long viewOwnPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view_own'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, viewOwnPermId);

        mockMvc.perform(get("/api/sales-forecast/versions")
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /*@Test
    void queryVersions_viewOwnPermission_nonOwnedChannel_returns403() throws Exception {
        String otherChannel = "大全聯";

        // Add view_own permission
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        Long viewOwnPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view_own'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, viewOwnPermId);

        mockMvc.perform(get("/api/sales-forecast/versions")
                        .param("month", MONTH)
                        .param("channel", otherChannel)
                        .header("Authorization", "Bearer " + userToken))
        //        .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }*/

    @Test
    void queryVersions_emptyResult_returns200() throws Exception {
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", adminRoleId, viewPermId);

        mockMvc.perform(get("/api/sales-forecast/versions")
                        .param("month", "209912")
                        .param("channel", "NonExistentChannel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
