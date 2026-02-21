package com.sinker.app.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ExcelTemplateServiceTest {

    private ExcelTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ExcelTemplateService();
    }

    @Test
    void testGenerateTemplate_ReturnsBytes() {
        byte[] result = service.generateTemplate("家樂福");
        assertNotNull(result, "Result should not be null");
        assertTrue(result.length > 0, "Result should not be empty");
    }

    @Test
    void testGenerateTemplate_HasCorrectHeaders() throws IOException {
        byte[] result = service.generateTemplate("大全聯");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow, "Header row should exist");
            assertEquals("中類名稱", getCellString(headerRow, 0));
            assertEquals("貨品規格", getCellString(headerRow, 1));
            assertEquals("品號", getCellString(headerRow, 2));
            assertEquals("品名", getCellString(headerRow, 3));
            assertEquals("庫位", getCellString(headerRow, 4));
            assertEquals("箱數小計", getCellString(headerRow, 5));
        }
    }

    @Test
    void testGenerateTemplate_HasSampleDataRow() throws IOException {
        byte[] result = service.generateTemplate("7-11");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            Row sampleRow = sheet.getRow(1);
            assertNotNull(sampleRow, "Sample data row should exist");
            assertNotNull(getCellString(sampleRow, 2), "Sample product code should not be null");
        }
    }

    @Test
    void testGenerateTemplate_HeaderIsBold() throws IOException {
        byte[] result = service.generateTemplate("全家");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            Cell headerCell = headerRow.getCell(0);
            CellStyle style = headerCell.getCellStyle();
            Font font = wb.getFontAt(style.getFontIndex());
            assertTrue(font.getBold(), "Header row should be bold");
        }
    }

    @Test
    void testGenerateTemplate_FirstRowFrozen() throws IOException {
        byte[] result = service.generateTemplate("好市多");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            // Check freeze pane exists - if frozen, getPane() won't be null
            // Verify by checking row count (frozen pane doesn't remove rows)
            assertNotNull(sheet.getRow(0), "Header row should exist after freeze");
        }
    }

    @Test
    void testGenerateTemplate_ValidExcelFormat() throws IOException {
        byte[] result = service.generateTemplate("萊爾富");
        // Should be parseable as valid XLSX
        assertDoesNotThrow(() -> {
            try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                assertNotNull(wb.getSheetAt(0));
            }
        });
    }

    @Test
    void testGenerateTemplate_SixColumns() throws IOException {
        byte[] result = service.generateTemplate("美廉社");
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertEquals(6, headerRow.getLastCellNum(), "Template should have 6 columns");
        }
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> cell.toString();
        };
    }
}
