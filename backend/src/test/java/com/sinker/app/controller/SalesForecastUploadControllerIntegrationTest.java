package com.sinker.app.controller;

import com.sinker.app.security.JwtTokenProvider;
import com.sinker.app.service.ErpProductService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesForecastUploadControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ErpProductService erpProductService;

    private static final String MONTH = "209801";
    private static final String CHANNEL = "家樂福";

    private String adminToken;
    private Long adminId;

    // A sales user with upload permission but possibly restricted channels
    private String salesToken;
    private Long salesUserId;

    @BeforeEach
    void setUp() {
        // ERP stub always returns true
        when(erpProductService.validateProduct(anyString())).thenReturn(true);

        // Create open test month
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);
        jdbc.update("INSERT INTO sales_forecast_config (month, auto_close_day, is_closed) VALUES (?, 28, FALSE)", MONTH);

        // Get admin user
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");

        // Create a sales role user with upload permission for channel ownership tests
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id IN (SELECT id FROM users WHERE username = 'test_upload_user')");
        jdbc.update("DELETE FROM users WHERE username = 'test_upload_user'");

        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_upload_user', 'test_upload@sinker.local', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'Test Upload User', ?, TRUE, FALSE, 0)",
                salesRoleId);
        salesUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_upload_user'", Long.class);

        // Give sales user the upload permission via role_permissions (add to sales role)
        Long uploadPermId = jdbc.queryForObject(
                "SELECT id FROM permissions WHERE code = 'sales_forecast.upload'", Long.class);
        // Check if already assigned
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ?",
                Integer.class, salesRoleId, uploadPermId);
        if (existing == null || existing == 0) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)",
                    salesRoleId, uploadPermId);
        }

        // Assign channel to sales user
        jdbc.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE channel = channel", salesUserId, CHANNEL);

        salesToken = tokenProvider.generateToken(salesUserId, "test_upload_user", "sales");

        // Clean up any leftover forecast data
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM sales_forecast WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_forecast_config WHERE month = ?", MONTH);
        jdbc.update("DELETE FROM sales_channels_users WHERE user_id = ?", salesUserId);
        jdbc.update("DELETE FROM users WHERE username = 'test_upload_user'");
    }

    // Helper: create a valid Excel file in memory
    private MockMultipartFile createExcelFile(int rowCount) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("中類名稱");
            header.createCell(1).setCellValue("貨品規格");
            header.createCell(2).setCellValue("品號");
            header.createCell(3).setCellValue("品名");
            header.createCell(4).setCellValue("庫位");
            header.createCell(5).setCellValue("箱數小計");

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("飲料類");
                row.createCell(1).setCellValue("600ml*24入");
                row.createCell(2).setCellValue("P" + String.format("%03d", i));
                row.createCell(3).setCellValue("商品" + i);
                row.createCell(4).setCellValue("A" + String.format("%02d", i));
                row.createCell(5).setCellValue(100.0 + i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "forecast.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private MockMultipartFile createEmptyExcelFile() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("中類名稱");
            header.createCell(1).setCellValue("貨品規格");
            header.createCell(2).setCellValue("品號");
            header.createCell(3).setCellValue("品名");
            header.createCell(4).setCellValue("庫位");
            header.createCell(5).setCellValue("箱數小計");
            // No data rows
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "forecast.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    // ============ Upload Tests ============

    @Test
    void testUpload_Success() throws Exception {
        MockMultipartFile file = createExcelFile(10);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_processed").value(10))
                .andExpect(jsonPath("$.month").value(MONTH))
                .andExpect(jsonPath("$.channel").value(CHANNEL))
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.upload_timestamp").isNotEmpty());

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ?",
                Integer.class, MONTH, CHANNEL);
        assertEquals(10, count);
    }

    @Test
    void testUpload_ReplacesExistingData() throws Exception {
        // First upload: 5 rows
        MockMultipartFile file1 = createExcelFile(5);
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file1)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Integer count1 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ?",
                Integer.class, MONTH, CHANNEL);
        assertEquals(5, count1);

        // Second upload: 3 rows
        MockMultipartFile file2 = createExcelFile(3);
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file2)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_processed").value(3));

        // Only 3 rows should remain
        Integer count2 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ?",
                Integer.class, MONTH, CHANNEL);
        assertEquals(3, count2);
    }

    @Test
    void testUpload_Unauthorized() throws Exception {
        MockMultipartFile file = createExcelFile(3);
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testUpload_NoPermission() throws Exception {
        // Create a user with no upload permission
        Long viewerRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'production_planner'", Long.class);
        jdbc.update("DELETE FROM users WHERE username = 'test_noperm_user'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_noperm_user', 'noperm@sinker.local', '$2b$10$QaOvaUqnaUwacjBwj1SP5eF/UohQ/xC6WkTbWXYh1oRXRDLSg8tHS', 'No Perm', ?, TRUE, FALSE, 0)",
                viewerRoleId);
        Long noPermUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_noperm_user'", Long.class);
        String noPermToken = tokenProvider.generateToken(noPermUserId, "test_noperm_user", "production_planner");

        try {
            MockMultipartFile file = createExcelFile(1);
            mockMvc.perform(multipart("/api/sales-forecast/upload")
                            .file(file)
                            .param("month", MONTH)
                            .param("channel", CHANNEL)
                            .header("Authorization", "Bearer " + noPermToken))
                    .andExpect(status().isForbidden());
        } finally {
            jdbc.update("DELETE FROM users WHERE username = 'test_noperm_user'");
        }
    }

    @Test
    void testUpload_DoesNotOwnChannel() throws Exception {
        // Sales user does NOT own "大全聯" (only owns "家樂福")
        String targetChannel = "大全聯";

        MockMultipartFile file = createExcelFile(3);
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", targetChannel)
                        .header("Authorization", "Bearer " + salesToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpload_MonthClosed() throws Exception {
        // Close the month
        jdbc.update("UPDATE sales_forecast_config SET is_closed = TRUE WHERE month = ?", MONTH);

        MockMultipartFile file = createExcelFile(3);
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpload_InvalidFileFormat() throws Exception {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "forecast.csv", "text/csv", "a,b,c\n1,2,3".getBytes());

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(csvFile)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = createEmptyExcelFile();

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(emptyFile)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_InvalidData_NegativeQuantity() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("中類名稱");
            header.createCell(1).setCellValue("貨品規格");
            header.createCell(2).setCellValue("品號");
            header.createCell(3).setCellValue("品名");
            header.createCell(4).setCellValue("庫位");
            header.createCell(5).setCellValue("箱數小計");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("飲料類");
            row.createCell(1).setCellValue("600ml*24入");
            row.createCell(2).setCellValue("P001");
            row.createCell(3).setCellValue("商品");
            row.createCell(4).setCellValue("A01");
            row.createCell(5).setCellValue(-10.0); // negative

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "forecast.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());

            mockMvc.perform(multipart("/api/sales-forecast/upload")
                            .file(file)
                            .param("month", MONTH)
                            .param("channel", CHANNEL)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details").isArray());
        }
    }

    @Test
    void testUpload_VersionFormat() throws Exception {
        MockMultipartFile file = createExcelFile(2);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        String version = jdbc.queryForObject(
                "SELECT version FROM sales_forecast WHERE month = ? AND channel = ? LIMIT 1",
                String.class, MONTH, CHANNEL);
        assertNotNull(version);
        assertTrue(version.matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\(.+\\)"),
                "Version should match format YYYY/MM/DD HH:MM:SS(channel): " + version);
        assertTrue(version.endsWith("(" + CHANNEL + ")"),
                "Version should end with channel: " + version);
    }

    @Test
    void testUpload_IsModifiedFalse() throws Exception {
        MockMultipartFile file = createExcelFile(3);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Integer modifiedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ? AND is_modified = TRUE",
                Integer.class, MONTH, CHANNEL);
        assertEquals(0, modifiedCount, "All uploaded rows should have is_modified=FALSE");
    }

    @Test
    void testUpload_InvalidChannel() throws Exception {
        MockMultipartFile file = createExcelFile(1);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", "UnknownChannel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpload_MonthNotFound() throws Exception {
        MockMultipartFile file = createExcelFile(1);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", "209900")
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpload_SalesUserWithChannelOwnership_Success() throws Exception {
        // Sales user owns CHANNEL (家樂福)
        MockMultipartFile file = createExcelFile(2);

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + salesToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_processed").value(2));
    }

    @Test
    void testUpload_ConcurrentUpload_DifferentChannels() throws Exception {
        // Create month for second channel
        String channel2 = "7-11";
        jdbc.update("DELETE FROM sales_forecast WHERE month = ? AND channel = ?", MONTH, channel2);

        MockMultipartFile file1 = createExcelFile(3);
        MockMultipartFile file2 = createExcelFile(4);

        // Sequential but verify both succeed
        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file1)
                        .param("month", MONTH)
                        .param("channel", CHANNEL)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_processed").value(3));

        mockMvc.perform(multipart("/api/sales-forecast/upload")
                        .file(file2)
                        .param("month", MONTH)
                        .param("channel", channel2)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows_processed").value(4));

        Integer count1 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ?",
                Integer.class, MONTH, CHANNEL);
        Integer count2 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = ? AND channel = ?",
                Integer.class, MONTH, channel2);
        assertEquals(3, count1);
        assertEquals(4, count2);

        // Cleanup
        jdbc.update("DELETE FROM sales_forecast WHERE month = ? AND channel = ?", MONTH, channel2);
    }

    // ============ Template Tests ============

    @Test
    void testTemplateDownload_Success() throws Exception {
        mockMvc.perform(get("/api/sales-forecast/template/大全聯")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        containsString("spreadsheetml.sheet")))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment")));
    }

    @Test
    void testTemplateDownload_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/sales-forecast/template/大全聯"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testTemplateDownload_ValidExcelContent() throws Exception {
        byte[] responseBytes = mockMvc.perform(get("/api/sales-forecast/template/家樂福")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertTrue(responseBytes.length > 0, "Template should not be empty");

        try (XSSFWorkbook wb = new XSSFWorkbook(
                new java.io.ByteArrayInputStream(responseBytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet, "Template should have a sheet");
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow, "Template should have header row");
            assertEquals("中類名稱", headerRow.getCell(0).getStringCellValue());
            assertEquals("品號", headerRow.getCell(2).getStringCellValue());
            assertEquals("箱數小計", headerRow.getCell(5).getStringCellValue());
        }
    }
}
