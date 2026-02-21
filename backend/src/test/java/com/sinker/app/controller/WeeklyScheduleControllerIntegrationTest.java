package com.sinker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.dto.schedule.UpdateScheduleRequest;
import com.sinker.app.security.JwtTokenProvider;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Timeout(60)
class WeeklyScheduleControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private static final String WEEK_START_MONDAY = "2026-02-02";  // Monday
    private static final String WEEK_START_TUESDAY = "2026-02-03"; // Tuesday
    private static final String FACTORY = "FACTORY-A";

    private String adminToken;
    private Long adminId;
    private String uploadToken;
    private Long uploadUserId;
    private String viewToken;
    private Long viewUserId;
    private String editToken;
    private Long editUserId;

    @BeforeEach
    void setUp() {
        // Get admin user
        adminId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
        adminToken = tokenProvider.generateToken(adminId, "admin", "admin");

        // Create test users with specific permissions
        Long salesRoleId = jdbc.queryForObject("SELECT id FROM roles WHERE code = 'sales'", Long.class);

        // User with upload permission
        jdbc.update("DELETE FROM users WHERE username = 'test_ws_upload'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_ws_upload', 'ws_upload@test.com', '$2b$10$test', 'WS Upload', ?, TRUE, FALSE, 0)", salesRoleId);
        uploadUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_ws_upload'", Long.class);
        ensurePermission(salesRoleId, "weekly_schedule.upload");
        uploadToken = tokenProvider.generateToken(uploadUserId, "test_ws_upload", "sales");

        // User with view permission
        jdbc.update("DELETE FROM users WHERE username = 'test_ws_view'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_ws_view', 'ws_view@test.com', '$2b$10$test', 'WS View', ?, TRUE, FALSE, 0)", salesRoleId);
        viewUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_ws_view'", Long.class);
        ensurePermission(salesRoleId, "weekly_schedule.view");
        viewToken = tokenProvider.generateToken(viewUserId, "test_ws_view", "sales");

        // User with edit permission
        jdbc.update("DELETE FROM users WHERE username = 'test_ws_edit'");
        jdbc.update("INSERT INTO users (username, email, hashed_password, full_name, role_id, is_active, is_locked, failed_login_count) " +
                "VALUES ('test_ws_edit', 'ws_edit@test.com', '$2b$10$test', 'WS Edit', ?, TRUE, FALSE, 0)", salesRoleId);
        editUserId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'test_ws_edit'", Long.class);
        ensurePermission(salesRoleId, "weekly_schedule.edit");
        editToken = tokenProvider.generateToken(editUserId, "test_ws_edit", "sales");

        // Clean up test data
        jdbc.update("DELETE FROM production_weekly_schedule WHERE factory = ?", FACTORY);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM production_weekly_schedule WHERE factory = ?", FACTORY);
        jdbc.update("DELETE FROM users WHERE username IN ('test_ws_upload', 'test_ws_view', 'test_ws_edit')");
    }

    private void ensurePermission(Long roleId, String permissionCode) {
        Long permId = jdbc.queryForObject("SELECT id FROM permissions WHERE code = ?", Long.class, permissionCode);
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE role_id = ? AND permission_id = ?",
                Integer.class, roleId, permId);
        if (existing == null || existing == 0) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)", roleId, permId);
        }
    }

    private MockMultipartFile createExcelFile(int rowCount) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("需求日期");
            header.createCell(1).setCellValue("品號");
            header.createCell(2).setCellValue("品名");
            header.createCell(3).setCellValue("庫位");
            header.createCell(4).setCellValue("箱數小計");

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("2026-02-0" + (2 + i % 5)); // Dates in week
                row.createCell(1).setCellValue("PROD" + String.format("%03d", i));
                row.createCell(2).setCellValue("Product " + i);
                row.createCell(3).setCellValue("WH-" + (char)('A' + i % 5));
                row.createCell(4).setCellValue(100.0 + i * 10);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "schedule.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private MockMultipartFile createExcelFileMissingColumns() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("需求日期");
            header.createCell(1).setCellValue("品號");
            // Missing other columns

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "schedule.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    // ============ Upload Tests ============

    @Test
    void testUpload_Success() throws Exception {
        MockMultipartFile file = createExcelFile(5);

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Upload successful"))
                .andExpect(jsonPath("$.recordsInserted").value(5))
                .andExpect(jsonPath("$.weekStart").value(WEEK_START_MONDAY))
                .andExpect(jsonPath("$.factory").value(FACTORY));

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_weekly_schedule WHERE week_start = ? AND factory = ?",
                Integer.class, WEEK_START_MONDAY, FACTORY);
        assertEquals(5, count);
    }

    @Test
    void testUpload_ReplacesExistingData() throws Exception {
        // First upload: 5 rows
        MockMultipartFile file1 = createExcelFile(5);
        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file1)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk());

        Integer count1 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_weekly_schedule WHERE week_start = ? AND factory = ?",
                Integer.class, WEEK_START_MONDAY, FACTORY);
        assertEquals(5, count1);

        // Second upload: 3 rows (should replace)
        MockMultipartFile file2 = createExcelFile(3);
        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file2)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsInserted").value(3));

        Integer count2 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM production_weekly_schedule WHERE week_start = ? AND factory = ?",
                Integer.class, WEEK_START_MONDAY, FACTORY);
        assertEquals(3, count2); // Should be 3, not 8
    }

    @Test
    void testUpload_MondayValidation_Failure() throws Exception {
        MockMultipartFile file = createExcelFile(2);

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file)
                        .param("week_start", WEEK_START_TUESDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must be a Monday")))
                .andExpect(jsonPath("$.message").value(containsString("Tuesday")));
    }

    @Test
    void testUpload_MissingRequiredColumns() throws Exception {
        MockMultipartFile file = createExcelFileMissingColumns();

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("missing required columns")));
    }

    @Test
    void testUpload_WithoutPermission() throws Exception {
        MockMultipartFile file = createExcelFile(2);

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken)) // view-only user
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpload_WithoutAuthentication() throws Exception {
        MockMultipartFile file = createExcelFile(2);

        mockMvc.perform(multipart("/api/weekly-schedule/upload")
                        .file(file)
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY))
                .andExpect(status().isUnauthorized());
    }

    // ============ GET Tests ============

    @Test
    void testGetSchedules_Success() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100.50)", WEEK_START_MONDAY, FACTORY);
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-04', 'PROD002', 'Product 2', 'WH-B', 200.75)", WEEK_START_MONDAY, FACTORY);

        mockMvc.perform(get("/api/weekly-schedule")
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].demandDate").value("2026-02-03"))
                .andExpect(jsonPath("$[0].productCode").value("PROD001"))
                .andExpect(jsonPath("$[0].quantity").value(100.50))
                .andExpect(jsonPath("$[1].demandDate").value("2026-02-04"))
                .andExpect(jsonPath("$[1].productCode").value("PROD002"));
    }

    @Test
    void testGetSchedules_EmptyResult() throws Exception {
        mockMvc.perform(get("/api/weekly-schedule")
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", "NON_EXISTENT")
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testGetSchedules_MissingWeekStart() throws Exception {
        mockMvc.perform(get("/api/weekly-schedule")
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSchedules_MissingFactory() throws Exception {
        mockMvc.perform(get("/api/weekly-schedule")
                        .param("week_start", WEEK_START_MONDAY)
                        .header("Authorization", "Bearer " + viewToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSchedules_WithoutPermission() throws Exception {
        mockMvc.perform(get("/api/weekly-schedule")
                        .param("week_start", WEEK_START_MONDAY)
                        .param("factory", FACTORY)
                        .header("Authorization", "Bearer " + uploadToken)) // upload-only user
                .andExpect(status().isForbidden());
    }

    // ============ PUT Tests ============

    @Test
    void testUpdateSchedule_Success() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100.50)", WEEK_START_MONDAY, FACTORY);
        Integer id = jdbc.queryForObject("SELECT id FROM production_weekly_schedule WHERE product_code = 'PROD001'", Integer.class);

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setDemandDate(java.time.LocalDate.parse("2026-02-05"));
        request.setQuantity(new BigDecimal("250.00"));

        mockMvc.perform(put("/api/weekly-schedule/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + editToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.demandDate").value("2026-02-05"))
                .andExpect(jsonPath("$.quantity").value(250.00));
    }

    @Test
    void testUpdateSchedule_PartialUpdate() throws Exception {
        // Insert test data
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100.50)", WEEK_START_MONDAY, FACTORY);
        Integer id = jdbc.queryForObject("SELECT id FROM production_weekly_schedule WHERE product_code = 'PROD001'", Integer.class);

        // Update only quantity
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setQuantity(new BigDecimal("300.00"));

        mockMvc.perform(put("/api/weekly-schedule/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + editToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demandDate").value("2026-02-03")) // unchanged
                .andExpect(jsonPath("$.quantity").value(300.00)); // updated
    }

    @Test
    void testUpdateSchedule_NotFound() throws Exception {
        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setQuantity(new BigDecimal("100.00"));

        mockMvc.perform(put("/api/weekly-schedule/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + editToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateSchedule_NegativeQuantity() throws Exception {
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100.50)", WEEK_START_MONDAY, FACTORY);
        Integer id = jdbc.queryForObject("SELECT id FROM production_weekly_schedule WHERE product_code = 'PROD001'", Integer.class);

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setQuantity(new BigDecimal("-10.00"));

        mockMvc.perform(put("/api/weekly-schedule/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + editToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("must be >= 0")));
    }

    @Test
    void testUpdateSchedule_WithoutPermission() throws Exception {
        jdbc.update("INSERT INTO production_weekly_schedule (week_start, factory, demand_date, product_code, product_name, warehouse_location, quantity) " +
                "VALUES (?, ?, '2026-02-03', 'PROD001', 'Product 1', 'WH-A', 100.50)", WEEK_START_MONDAY, FACTORY);
        Integer id = jdbc.queryForObject("SELECT id FROM production_weekly_schedule WHERE product_code = 'PROD001'", Integer.class);

        UpdateScheduleRequest request = new UpdateScheduleRequest();
        request.setQuantity(new BigDecimal("200.00"));

        mockMvc.perform(put("/api/weekly-schedule/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", "Bearer " + viewToken)) // view-only user
                .andExpect(status().isForbidden());
    }
}
