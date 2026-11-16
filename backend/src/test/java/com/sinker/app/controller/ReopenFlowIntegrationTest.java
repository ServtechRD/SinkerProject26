package com.sinker.app.controller;

import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.ForecastEditGuard;
import com.sinker.app.service.FormSummaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReopenFlowIntegrationTest {

    private static final String MONTH = "209811";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SalesForecastConfigRepository configRepository;
    @Autowired private SalesForecastRepository forecastRepository;
    @Autowired private FormSummaryService formSummaryService;
    @Autowired private ForecastEditGuard editGuard;

    private String adminToken;
    private String salesToken;
    private Integer configId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_form_version WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_reopen_window WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_form_log WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);

        Long adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");
        // 業務角色（擁有 sales_forecast_config.view）
        salesToken = tokenProvider.generateToken(adminId, "admin", "sales");

        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_at, is_closed, closed_at, created_at, updated_at) " +
                "VALUES (?, '2098-11-01 00:00:00', true, NOW(), NOW(), NOW())", MONTH);
        configId = jdbc.queryForObject("SELECT id FROM sales_forecast_config WHERE month = ?", Integer.class, MONTH);

        SalesForecast biz = new SalesForecast();
        biz.setMonth(MONTH);
        biz.setChannel("家樂福");
        biz.setVersion("2098/10/30 10:00:00(家樂福)");
        biz.setCategory("果凍");
        biz.setSpec("1000g");
        biz.setProductName("蒟蒻果凍");
        biz.setProductCode("P1");
        biz.setWarehouseLocation("C425");
        biz.setQuantity(new BigDecimal("10"));
        biz.setRemark("");
        biz.setIsModified(false);
        biz.setCreatedAt(LocalDateTime.now());
        biz.setUpdatedAt(LocalDateTime.now());
        forecastRepository.save(biz);

        SalesForecastConfig config = configRepository.findByMonth(MONTH).orElseThrow();
        formSummaryService.createFormVersion1Snapshot(MONTH, config);
    }

    @Test
    void reopen_upload_end_createsNewFormVersionAndLogs() throws Exception {
        String expiresAt = LocalDateTime.now(ZoneId.of("Asia/Taipei")).plusHours(2)
                .withSecond(0).withNano(0).toString();

        // 截止後未補登 → CLOSED
        mockMvc.perform(get("/api/sales-forecast/config/edit-status")
                        .param("month", MONTH).param("channel", "家樂福")
                        .header("Authorization", "Bearer " + salesToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"" + expiresAt + "\",\"reason\":\"新品追加\"}"))
                .andExpect(status().isCreated());

        // 開放補登後 → REOPENED
        mockMvc.perform(get("/api/sales-forecast/config/edit-status")
                        .param("month", MONTH).param("channel", "家樂福")
                        .header("Authorization", "Bearer " + salesToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"));

        // 重複開放同一通路 → 400
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"" + expiresAt + "\",\"reason\":\"再一次\"}"))
                .andExpect(status().isBadRequest());

        SalesForecastConfig config = configRepository.findByMonth(MONTH).orElseThrow();
        assertDoesNotThrow(() -> editGuard.assertBusinessEditable(config, "家樂福"));
        assertThrows(AccessDeniedException.class, () -> editGuard.assertBusinessEditable(config, "愛買"));

        mockMvc.perform(get("/api/sales-forecast/config/" + configId + "/detail")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channels[?(@.channel=='家樂福')].activeWindow.reason").value("新品追加"));

        // 模擬業務補登：業務層數量改為 99
        jdbc.update("UPDATE sales_forecast SET quantity = 99 WHERE month = ? AND channel = '家樂福' " +
                "AND version NOT LIKE 'form_v%'", MONTH);

        Integer windowId = jdbc.queryForObject(
                "SELECT id FROM sales_forecast_reopen_window WHERE month = ? AND channel = '家樂福'", Integer.class, MONTH);
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen/" + windowId + "/end")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        BigDecimal merged = jdbc.queryForObject(
                "SELECT quantity FROM sales_forecast WHERE month = ? AND channel = '家樂福' " +
                        "AND version = 'form_v2' AND product_code = 'P1'", BigDecimal.class, MONTH);
        assertEquals(0, new BigDecimal("99").compareTo(merged));
        assertEquals("form_v2", jdbc.queryForObject(
                "SELECT merged_version FROM sales_forecast_reopen_window WHERE id = ?", String.class, windowId));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_form_log WHERE month = ? AND action = 'REOPEN'", Integer.class, MONTH));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast_form_log WHERE month = ? AND action = 'REOPEN_END'", Integer.class, MONTH));
        assertThrows(AccessDeniedException.class, () -> editGuard.assertBusinessEditable(config, "家樂福"));

        mockMvc.perform(get("/api/sales-forecast/config/" + configId + "/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REOPEN_END"));
    }

    @Test
    void reopen_withoutPermission_forbidden() throws Exception {
        String noPermToken = tokenProvider.generateToken(999L, "noperm", "nonexistent_role");
        mockMvc.perform(post("/api/sales-forecast/config/" + configId + "/reopen")
                        .header("Authorization", "Bearer " + noPermToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channels\":[\"家樂福\"],\"expiresAt\":\"2098-11-02T10:00\",\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveFormVersion_blankReason_badRequest() throws Exception {
        mockMvc.perform(post("/api/sales-forecast/form-summary/save-version")
                        .param("month", MONTH)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"change_reason\":\"  \",\"rows\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
