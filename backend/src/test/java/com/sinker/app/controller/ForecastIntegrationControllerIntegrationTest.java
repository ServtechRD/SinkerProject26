package com.sinker.app.controller;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class ForecastIntegrationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ErpProductService erpProductService;

    private static final String TEST_MONTH = "209902";
    private static final String VERSION_1 = "2099/02/01 10:00:00";
    private static final String VERSION_2 = "2099/02/01 11:00:00";

    private String adminToken;
    private Long adminId;

    @BeforeEach
    void setUp() {
        when(erpProductService.validateProduct(anyString())).thenReturn(true);

        // Create open test month
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", TEST_MONTH);
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed) VALUES (?, 28, FALSE)", TEST_MONTH);

        // Get admin user
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");

        // Clean up test data
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", TEST_MONTH);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", TEST_MONTH);
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", TEST_MONTH);
    }

    @Test
    void queryIntegration_success_withData() throws Exception {
        // Insert test data for version 1
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 100.00, ?)", TEST_MONTH, VERSION_1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '愛買', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 50.00, ?)", TEST_MONTH, VERSION_1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '711', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 75.00, ?)", TEST_MONTH, VERSION_1);

        // Insert version 2 with updated quantities
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 120.00, ?)", TEST_MONTH, VERSION_2);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '愛買', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 60.00, ?)", TEST_MONTH, VERSION_2);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '711', 'P001', '可口可樂', '01飲料類', '600ml*24入', 'A01', 80.00, ?)", TEST_MONTH, VERSION_2);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .param("version", VERSION_2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productCode", is("P001")))
                .andExpect(jsonPath("$[0].productName", is("可口可樂")))
                .andExpect(jsonPath("$[0].category", is("01飲料類")))
                .andExpect(jsonPath("$[0].qtyCarrefour", is(120.0)))
                .andExpect(jsonPath("$[0].qtyAimall", is(60.0)))
                .andExpect(jsonPath("$[0].qty711", is(80.0)))
                .andExpect(jsonPath("$[0].originalSubtotal", is(260.0)))
                .andExpect(jsonPath("$[0].difference", is(35.0)))
                .andExpect(jsonPath("$[0].remarks", is("數量增加")));
    }

    @Test
    void queryIntegration_success_latestVersion() throws Exception {
        // Insert test data for latest version
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P002', '樂事洋芋片', '02零食類', '150g*12包', 'B02', 50.00, ?)", TEST_MONTH, VERSION_2);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productCode", is("P002")))
                .andExpect(jsonPath("$[0].originalSubtotal", is(50.0)));
    }

    @Test
    void queryIntegration_emptyResult() throws Exception {
        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void queryIntegration_missingMonth_badRequest() throws Exception {
        mockMvc.perform(get("/api/sales-forecast/integration")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryIntegration_multipleProducts_sortedByCategory() throws Exception {
        // Insert products with different categories
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P003', '產品3', '03日用品', 'spec3', 'C03', 10.00, ?)", TEST_MONTH, VERSION_1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '產品1', '01飲料類', 'spec1', 'A01', 20.00, ?)", TEST_MONTH, VERSION_1);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P002', '產品2', '02零食類', 'spec2', 'B02', 15.00, ?)", TEST_MONTH, VERSION_1);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .param("version", VERSION_1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].productCode", is("P001")))  // 01飲料類
                .andExpect(jsonPath("$[1].productCode", is("P002")))  // 02零食類
                .andExpect(jsonPath("$[2].productCode", is("P003"))); // 03日用品
    }

    @Test
    void queryIntegration_newProduct_differenceEqualsSubtotal() throws Exception {
        // Version 1: has P000 but not P001
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P000', '舊產品', '01飲料類', 'spec0', 'A01', 50.00, ?)", TEST_MONTH, VERSION_1);

        // Version 2: has both P000 and P001 (P001 is new)
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P000', '舊產品', '01飲料類', 'spec0', 'A01', 50.00, ?)", TEST_MONTH, VERSION_2);
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '新產品', '01飲料類', 'spec1', 'A01', 100.00, ?)", TEST_MONTH, VERSION_2);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .param("version", VERSION_2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].productCode", is("P001")))  // Second product after sorting
                .andExpect(jsonPath("$[1].originalSubtotal", is(100.0)))
                .andExpect(jsonPath("$[1].difference", is(100.0)))
                .andExpect(jsonPath("$[1].remarks", is("新增產品")));
    }

    @Test
    void queryIntegration_decreasedQuantity() throws Exception {
        // Version 1: 200
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '產品', '01飲料類', 'spec1', 'A01', 200.00, ?)", TEST_MONTH, VERSION_1);

        // Version 2: 150
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '產品', '01飲料類', 'spec1', 'A01', 150.00, ?)", TEST_MONTH, VERSION_2);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .param("version", VERSION_2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difference", is(-50.0)))
                .andExpect(jsonPath("$[0].remarks", is("數量減少")));
    }

    @Test
    void queryIntegration_noChange() throws Exception {
        // Version 1: 100
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '產品', '01飲料類', 'spec1', 'A01', 100.00, ?)", TEST_MONTH, VERSION_1);

        // Version 2: 100 (same)
        jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, product_name, category, spec, warehouse_location, quantity, version) " +
                "VALUES (?, '家樂福', 'P001', '產品', '01飲料類', 'spec1', 'A01', 100.00, ?)", TEST_MONTH, VERSION_2);

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .param("version", VERSION_2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difference", is(0.0)))
                .andExpect(jsonPath("$[0].remarks", is("無變化")));
    }

    @Test
    void queryIntegration_requiresPermission() throws Exception {
        // Create user without view permission
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id IN (SELECT id FROM users WHERE username = 'test_no_perm')");
        jdbc.update("DELETE FROM users WHERE username = 'test_no_perm'");

        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_no_perm', 'noperm@test.com', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'No Perm User', ?, TRUE, FALSE, 0)",
                salesRoleId);
        Long nopermId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_no_perm'", Long.class);

        // Remove view permission from sales role temporarily
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ? AND permission_id IN (SELECT id FROM permissions WHERE code IN ('sales_forecast.view', 'sales_forecast.view_own'))", salesRoleId);

        String nopermToken = tokenProvider.generateToken(nopermId, "test_no_perm", "sales");

        mockMvc.perform(get("/api/sales-forecast/integration")
                        .param("month", TEST_MONTH)
                        .header("Authorization", "Bearer " + nopermToken))
                .andExpect(status().isForbidden());

        // Cleanup
        jdbc.update("DELETE FROM users WHERE id = ?", nopermId);

        // Restore permissions
        Long viewPermId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = 'sales_forecast.view'", Long.class);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", salesRoleId, viewPermId);
    }
}
