package com.sinker.app.controller;

import com.sinker.app.dto.erp.VendorListRequest;
import com.sinker.app.dto.erp.VendorSyncItem;
import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.ErpVendorSyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class IntegrationControllerVendorSyncIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ErpVendorSyncClient erpVendorSyncClient;

    private String adminToken;
    private String nonAdminToken;

    @BeforeEach
    void setUp() {
        Long adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");
        nonAdminToken = tokenProvider.generateToken(888888L, "test_vendor_sync_noperm", "sales");
    }

    @Test
    void triggerVendorSync_success_returns202AndCompletesWithZeroFetched() throws Exception {
        when(erpVendorSyncClient.isConfigured()).thenReturn(true);
        when(erpVendorSyncClient.fetchPage(any(VendorListRequest.class))).thenReturn(List.of());

        mockMvc.perform(post("/api/integrations/erp/vendor-sync")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.is("同步已開始")));

        waitUntilNotRunning();

        mockMvc.perform(get("/api/integrations/erp/vendor-sync/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.running", org.hamcrest.Matchers.is(false)))
                .andExpect(jsonPath("$.lastResult.totalFetched", org.hamcrest.Matchers.is(0)))
                .andExpect(jsonPath("$.lastError", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void triggerVendorSync_whileAlreadyRunning_returns409() throws Exception {
        when(erpVendorSyncClient.isConfigured()).thenReturn(true);
        when(erpVendorSyncClient.fetchPage(any(VendorListRequest.class))).thenAnswer(invocation -> {
            TimeUnit.MILLISECONDS.sleep(1500);
            return List.of();
        });

        mockMvc.perform(post("/api/integrations/erp/vendor-sync")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/integrations/erp/vendor-sync")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.is("同步已在執行中，請稍後再試")));

        waitUntilNotRunning();
    }

    @Test
    void triggerVendorSync_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/integrations/erp/vendor-sync")
                        .header("Authorization", "Bearer " + nonAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    private void waitUntilNotRunning() throws Exception {
        for (int i = 0; i < 100; i++) {
            String body = mockMvc.perform(get("/api/integrations/erp/vendor-sync/status")
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn().getResponse().getContentAsString();
            if (body.contains("\"running\":false")) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
        throw new IllegalStateException("ERP vendor sync did not finish within timeout");
    }
}
