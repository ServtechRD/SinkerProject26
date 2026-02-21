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
class MaterialPurchaseControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    private String viewToken;
    private Long viewUserId;
    private String noPermToken;
    private Long noPermUserId;

    private static final String WEEK_START = "2026-02-17";
    private static final String FACTORY = "F1";

    @BeforeEach
    void setUp() {
        // Ensure permission exists
        ensurePermissionExists("material_purchase.view", "View material purchase data");

        // Get admin role to ensure permission
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);

        // Create user with view permission
        jdbc.update("DELETE FROM users WHERE username = 'test_mp_view'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_mp_view', 'mp_view@test.com', '$2b$10$test', 'MP View', ?, TRUE, FALSE, 0)", adminRoleId);
        viewUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_mp_view'", Long.class);
        ensurePermission(adminRoleId, "material_purchase.view");
        viewToken = tokenProvider.generateToken(viewUserId, "test_mp_view", "admin");

        // Create user without permission
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("DELETE FROM users WHERE username = 'test_mp_noperm'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_mp_noperm', 'mp_noperm@test.com', '$2b$10$test', 'MP NoPerm', ?, TRUE, FALSE, 0)", salesRoleId);
        noPermUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_mp_noperm'", Long.class);
        noPermToken = tokenProvider.generateToken(noPermUserId, "test_mp_noperm", "sales");

        // Clean up test data
        jdbc.update("DELETE FROM material_purchase WHERE factory = ?", FACTORY);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM material_purchase WHERE factory = ?", FACTORY);
        jdbc.update("DELETE FROM users WHERE username IN ('test_mp_view', 'test_mp_noperm')");
    }

    private void ensurePermissionExists(String code, String description) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE code = ?", Integer.class, code);
        if (count == 0) {
            jdbc.update("INSERT INTO permissions (code, description) VALUES (?, ?)", code, description);
        }
    }

    private void ensurePermission(Long roleId, String permissionCode) {
        Long permId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = ?", Long.class, permissionCode);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permId);
    }

    @Test
    void testQueryMaterialPurchaseSuccess() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered, erp_order_no) " +
                "VALUES (?, ?, 'P001', '產品A', 1000.00, '半成品A', 'SP001', 5.50, 5500.00, 20.00, 275.00, FALSE, NULL)",
                WEEK_START, FACTORY);
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered, erp_order_no) " +
                "VALUES (?, ?, 'P002', '產品B', 500.00, '半成品B', 'SP002', 3.00, 1500.00, 15.00, 100.00, TRUE, 'ERP-2026-001')",
                WEEK_START, FACTORY);

        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].productCode", is("P001")))
                .andExpect(jsonPath("$[0].productName", is("產品A")))
                .andExpect(jsonPath("$[0].quantity", is(1000.00)))
                .andExpect(jsonPath("$[0].semiProductName", is("半成品A")))
                .andExpect(jsonPath("$[0].semiProductCode", is("SP001")))
                .andExpect(jsonPath("$[0].kgPerBox", is(5.50)))
                .andExpect(jsonPath("$[0].basketQuantity", is(5500.00)))
                .andExpect(jsonPath("$[0].boxesPerBarrel", is(20.00)))
                .andExpect(jsonPath("$[0].requiredBarrels", is(275.00)))
                .andExpect(jsonPath("$[0].isErpTriggered", is(false)))
                .andExpect(jsonPath("$[0].erpOrderNo").doesNotExist())
                .andExpect(jsonPath("$[1].productCode", is("P002")))
                .andExpect(jsonPath("$[1].productName", is("產品B")))
                .andExpect(jsonPath("$[1].isErpTriggered", is(true)))
                .andExpect(jsonPath("$[1].erpOrderNo", is("ERP-2026-001")));
    }

    @Test
    void testQueryMaterialPurchaseOrderedByProductCode() throws Exception {
        // Insert test data in reverse order
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered) " +
                "VALUES (?, ?, 'P003', '產品C', 300.00, '半成品C', 'SP003', 4.20, 1260.00, 25.00, 50.40, FALSE)",
                WEEK_START, FACTORY);
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered) " +
                "VALUES (?, ?, 'P001', '產品A', 100.00, '半成品A', 'SP001', 5.50, 550.00, 20.00, 27.50, FALSE)",
                WEEK_START, FACTORY);
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered) " +
                "VALUES (?, ?, 'P002', '產品B', 200.00, '半成品B', 'SP002', 3.00, 600.00, 15.00, 40.00, FALSE)",
                WEEK_START, FACTORY);

        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].productCode", is("P001")))
                .andExpect(jsonPath("$[1].productCode", is("P002")))
                .andExpect(jsonPath("$[2].productCode", is("P003")));
    }

    @Test
    void testQueryMaterialPurchaseEmptyResult() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", "2026-12-31")
                        .param("factory", "F999")
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testQueryMaterialPurchaseMissingWeekStart() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void testQueryMaterialPurchaseMissingFactory() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void testQueryMaterialPurchaseInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", "invalid")
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testQueryMaterialPurchaseWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testQueryMaterialPurchaseWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testQueryMaterialPurchaseDecimalPrecision() throws Exception {
        // Insert test data with specific decimal values
        jdbc.update("INSERT INTO material_purchase (week_start, factory, product_code, product_name, quantity, " +
                "semi_product_name, semi_product_code, kg_per_box, basket_quantity, boxes_per_barrel, required_barrels, " +
                "is_erp_triggered) " +
                "VALUES (?, ?, 'P001', '產品A', 123.45, '半成品A', 'SP001', 6.78, 837.01, 22.50, 37.20, FALSE)",
                WEEK_START, FACTORY);

        mockMvc.perform(get("/api/material-purchase")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].quantity", is(123.45)))
                .andExpect(jsonPath("$[0].kgPerBox", is(6.78)))
                .andExpect(jsonPath("$[0].basketQuantity", is(837.01)))
                .andExpect(jsonPath("$[0].boxesPerBarrel", is(22.50)))
                .andExpect(jsonPath("$[0].requiredBarrels", is(37.20)));
    }
}
