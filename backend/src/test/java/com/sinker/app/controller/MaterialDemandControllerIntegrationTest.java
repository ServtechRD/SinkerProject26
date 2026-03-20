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
class MaterialDemandControllerIntegrationTest {

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
        // Get admin role to ensure permission
        Long adminRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'admin'", Long.class);

        // Create user with view permission
        jdbc.update("DELETE FROM users WHERE username = 'test_md_view'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_md_view', 'md_view@test.com', '$2b$10$test', 'MD View', ?, TRUE, FALSE, 0)", adminRoleId);
        viewUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_md_view'", Long.class);
        ensurePermission(adminRoleId, "material_demand.view");
        viewToken = tokenProvider.generateToken(viewUserId, "test_md_view", "admin");

        // Create user without permission
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("DELETE FROM users WHERE username = 'test_md_noperm'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_md_noperm', 'md_noperm@test.com', '$2b$10$test', 'MD NoPerm', ?, TRUE, FALSE, 0)", salesRoleId);
        noPermUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_md_noperm'", Long.class);
        noPermToken = tokenProvider.generateToken(noPermUserId, "test_md_noperm", "sales");

        // Clean up test data
        jdbc.update("DELETE FROM material_demand WHERE factory = ?", FACTORY);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM material_demand WHERE factory = ?", FACTORY);
        jdbc.update("DELETE FROM users WHERE username IN ('test_md_view', 'test_md_noperm')");
    }

    private void ensurePermission(Long roleId, String permissionCode) {
        Long permId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = ?", Long.class, permissionCode);
        jdbc.update("INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permId);
    }

    @Test
    void testQueryMaterialDemandSuccess() throws Exception {
        // GET 會走 PDCA 同步（測試環境 URL 未設定 → PdcaLocalStub 固定 5 筆），與 DB 預先插入無關
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].materialCode", is("AA08C")))
                .andExpect(jsonPath("$[0].materialName", is("關華豆膠(LF20)/25kg/包")))
                .andExpect(jsonPath("$[0].unit", is("KG")))
                .andExpect(jsonPath("$[0].lastPurchaseDate").doesNotExist())
                .andExpect(jsonPath("$[0].demandDate", is("2026-02-20")))
                .andExpect(jsonPath("$[0].expectedDelivery").value(40))
                .andExpect(jsonPath("$[0].demandQuantity").value(100))
                .andExpect(jsonPath("$[0].estimatedInventory").value(20))
                .andExpect(jsonPath("$[4].materialCode", is("EE08C")));
    }

    @Test
    void testQueryMaterialDemandOrderedByMaterialCode() throws Exception {
        // PDCA stub 依品號排序回傳（AA08C … EE08C）
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].materialCode", is("AA08C")))
                .andExpect(jsonPath("$[1].materialCode", is("BA12D")))
                .andExpect(jsonPath("$[2].materialCode", is("CC05A")))
                .andExpect(jsonPath("$[3].materialCode", is("DD15B")))
                .andExpect(jsonPath("$[4].materialCode", is("EE08C")));
    }

    @Test
    void testQueryMaterialDemandEmptyResult() throws Exception {
        // 無週排程時仍回傳 PDCA stub 固定列，不會 0 筆
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", "2026-12-31")
                        .param("factory", "F999")
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    void testQueryMaterialDemandMissingWeekStart() throws Exception {
        mockMvc.perform(get("/api/material-demand")
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void testQueryMaterialDemandMissingFactory() throws Exception {
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", WEEK_START)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    void testQueryMaterialDemandInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", "invalid")
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testQueryMaterialDemandWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + noPermToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testQueryMaterialDemandWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/material-demand")
                        .param("week_start", WEEK_START)
                        .param("factory", FACTORY))
                .andExpect(status().isUnauthorized());
    }
}
