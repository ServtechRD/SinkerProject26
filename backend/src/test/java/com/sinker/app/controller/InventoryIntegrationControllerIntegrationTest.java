package com.sinker.app.controller;

import com.sinker.app.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class InventoryIntegrationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    private static final String MONTH = "2026-01";

    private String tokenWithPermission;
    private String tokenWithoutPermission;
    private Long userWithPermId;
    private Long userWithoutPermId;

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbc.update("DELETE FROM inventory_sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);

        // Use admin user (has all permissions)
        userWithPermId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        tokenWithPermission = tokenProvider.generateToken(userWithPermId, "admin", "admin");

        // Create user without inventory.view permission
        jdbc.update("DELETE FROM users WHERE username = 'no_inv_user'");
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('no_inv_user', 'noinv@test.com', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'No Inventory User', ?, TRUE, FALSE, 0)",
                salesRoleId);
        userWithoutPermId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'no_inv_user'", Long.class);
        tokenWithoutPermission = tokenProvider.generateToken(userWithoutPermId, "no_inv_user", "sales");

        // Insert test forecast data with all 12 channels
        String version = "v20260101120000";
        String[] channels = {"PX/大全聯", "家樂福", "愛買", "711", "全家", "OK/萊爾富",
                "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷"};

        for (String channel : channels) {
            jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, " +
                            "warehouse_location, quantity, version, is_modified, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NOW(), NOW())",
                    MONTH, channel, "Category A", "Spec A", "PROD001", "Product 1", "WH-A", 50.00, version);
        }

        for (String channel : channels) {
            jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, " +
                            "warehouse_location, quantity, version, is_modified, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NOW(), NOW())",
                    MONTH, channel, "Category B", "Spec B", "PROD002", "Product 2", "WH-B", 30.00, version);
        }
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM inventory_sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM users WHERE id = ?", userWithoutPermId);
    }

    @Test
    void queryInventoryIntegration_realTime_success() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productCode", is("PROD001")))
                .andExpect(jsonPath("$[0].productName", is("Product 1")))
                .andExpect(jsonPath("$[0].category", is("Category A")))
                .andExpect(jsonPath("$[0].forecastQuantity", is(600.00))) // 50 * 12 channels
                .andExpect(jsonPath("$[0].inventoryBalance", is(250.00))) // ERP stub value for PROD001
                .andExpect(jsonPath("$[0].salesQuantity", is(100.00)))    // ERP stub value for PROD001
                .andExpect(jsonPath("$[0].productionSubtotal", is(250.00))) // 600 - 250 - 100
                .andExpect(jsonPath("$[0].version", notNullValue()))
                .andExpect(jsonPath("$[0].queryStartDate", is("2026-01-01")))
                .andExpect(jsonPath("$[0].queryEndDate", is("2026-01-31")))
                .andExpect(jsonPath("$[1].productCode", is("PROD002")))
                .andExpect(jsonPath("$[1].forecastQuantity", is(360.00))) // 30 * 12 channels
                .andExpect(jsonPath("$[1].inventoryBalance", is(150.00))) // ERP stub value for PROD002
                .andExpect(jsonPath("$[1].salesQuantity", is(75.00)))     // ERP stub value for PROD002
                .andExpect(jsonPath("$[1].productionSubtotal", is(135.00))); // 360 - 150 - 75
    }

    @Test
    void queryInventoryIntegration_withDateRange_success() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH)
                        .param("startDate", "2026-01-10")
                        .param("endDate", "2026-01-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].queryStartDate", is("2026-01-10")))
                .andExpect(jsonPath("$[0].queryEndDate", is("2026-01-20")));
    }

    @Test
    void queryInventoryIntegration_versionQuery_success() throws Exception {
        // First create a version
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk());

        // Get the version from database
        String version = jdbc.queryForObject(
                "SELECT version FROM inventory_sales_forecast WHERE month = ? LIMIT 1",
                String.class, MONTH);

        // Query by version
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH)
                        .param("version", version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].version", is(version)))
                .andExpect(jsonPath("$[1].version", is(version)));

        // Verify no new version was created
        int versionCount = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT version) FROM inventory_sales_forecast WHERE month = ?",
                Integer.class, MONTH);
        assert versionCount == 1;
    }

    @Test
    void queryInventoryIntegration_versionNotFound_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH)
                        .param("version", "v99999999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void queryInventoryIntegration_multipleVersions_success() throws Exception {
        // Create first version
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk());

        Thread.sleep(1000); // Ensure different timestamp

        // Create second version
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk());

        // Verify two versions exist
        int versionCount = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT version) FROM inventory_sales_forecast WHERE month = ?",
                Integer.class, MONTH);
        assert versionCount == 2;
    }

    @Test
    void queryInventoryIntegration_noForecastData_returnsEmpty() throws Exception {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);

        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void queryInventoryIntegration_missingMonth_returns400() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("month")));
    }

    @Test
    void queryInventoryIntegration_noAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .param("month", MONTH))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void queryInventoryIntegration_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithoutPermission)
                        .param("month", MONTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void queryInventoryIntegration_aggregation_success() throws Exception {
        // Insert duplicate product with same key but different channel
        String version = "v20260101120000";
        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, " +
                        "warehouse_location, quantity, version, is_modified, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NOW(), NOW())",
                MONTH, "PX/大全聯", "Category C", "Spec C", "PROD003", "Product 3", "WH-C", 100.00, version);

        jdbc.update("INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, " +
                        "warehouse_location, quantity, version, is_modified, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, NOW(), NOW())",
                MONTH, "家樂福", "Category C", "Spec C", "PROD003", "Product 3", "WH-C", 200.00, version);

        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].productCode", is("PROD003")))
                .andExpect(jsonPath("$[2].forecastQuantity", is(300.00))); // 100 + 200
    }

    @Test
    void queryInventoryIntegration_sortedByProductCode_success() throws Exception {
        mockMvc.perform(get("/api/inventory-integration")
                        .header("Authorization", "Bearer " + tokenWithPermission)
                        .param("month", MONTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode", is("PROD001")))
                .andExpect(jsonPath("$[1].productCode", is("PROD002")));
    }
}
