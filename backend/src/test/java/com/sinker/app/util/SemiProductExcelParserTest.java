package com.sinker.app.util;

import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.util.SemiProductExcelParser.SemiProductRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SemiProductExcelParserTest {

    private SemiProductExcelParser parser;

    @BeforeEach
    void setUp() {
        parser = new SemiProductExcelParser();
    }

    // Helper: create a valid Excel in memory
    private MockMultipartFile createExcel(Object[][] data) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    Object val = data[r][c];
                    if (val == null) {
                        row.createCell(c);
                    } else if (val instanceof Number) {
                        row.createCell(c).setCellValue(((Number) val).doubleValue());
                    } else {
                        row.createCell(c).setCellValue(val.toString());
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }

    private Object[][] headerRow() {
        return new Object[][]{{
                "品號", "品名", "提前日數"
        }};
    }

    private Object[][] withHeader(Object[]... dataRows) {
        Object[][] result = new Object[1 + dataRows.length][];
        result[0] = headerRow()[0];
        for (int i = 0; i < dataRows.length; i++) {
            result[i + 1] = dataRows[i];
        }
        return result;
    }

    @Test
    void testParseValidExcel() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", 7},
                new Object[]{"SP002", "半成品B", 10},
                new Object[]{"SP003", "半成品C", 5}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(3, rows.size());
        assertEquals("SP001", rows.get(0).getProductCode());
        assertEquals("半成品A", rows.get(0).getProductName());
        assertEquals(7, rows.get(0).getAdvanceDays());
    }

    @Test
    void testParseExcel_SkipsHeaderRow() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", 7},
                new Object[]{"SP002", "半成品B", 10}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(2, rows.size());
        // Verify header row was skipped
        assertEquals("SP001", rows.get(0).getProductCode());
    }

    @Test
    void testParseExcel_EmptyFile() throws IOException {
        Object[][] data = headerRow(); // Only header, no data
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("no data rows") || ex.getMessage().contains("no valid data"),
                "Should indicate no data rows: " + ex.getMessage());
    }

    @Test
    void testParseExcel_MissingRequiredColumn_ProductCode() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"", "半成品A", 7}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("product_code") || ex.getMessage().contains("required"),
                "Should mention product_code: " + ex.getMessage());
    }

    @Test
    void testParseExcel_MissingRequiredColumn_ProductName() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "", 7}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("product_name") || ex.getMessage().contains("required"),
                "Should mention product_name: " + ex.getMessage());
    }

    @Test
    void testParseExcel_InvalidAdvanceDaysFormat() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", "abc"}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("Row 2") && ex.getMessage().contains("advance_days"),
                "Should mention row number and advance_days: " + ex.getMessage());
    }

    @Test
    void testParseExcel_NegativeAdvanceDays() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", -5}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("positive") || ex.getMessage().contains("Row 2"),
                "Should mention positive advance_days: " + ex.getMessage());
    }

    @Test
    void testParseExcel_ZeroAdvanceDays() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", 0}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("positive"),
                "Zero advance_days should be rejected");
    }

    @Test
    void testParseExcel_ChineseCharacters() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"半成品001", "半成品測試產品名稱", 7}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertEquals("半成品001", rows.get(0).getProductCode());
        assertEquals("半成品測試產品名稱", rows.get(0).getProductName());
        assertEquals(7, rows.get(0).getAdvanceDays());
    }

    @Test
    void testParseExcel_InvalidFile_NotXlsx() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", "a,b,c".getBytes());
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(csvFile));
        assertTrue(ex.getMessage().contains("xlsx") || ex.getMessage().contains("format"),
                "Should reject non-xlsx file: " + ex.getMessage());
    }

    @Test
    void testParseExcel_EmptyMultipartFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(emptyFile));
        assertTrue(ex.getMessage().toLowerCase().contains("empty"),
                "Should reject empty file: " + ex.getMessage());
    }

    @Test
    void testParseExcel_RowNumberInError() throws IOException {
        // Row 5 (index 4) has negative advance_days
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", 7},
                new Object[]{"SP002", "半成品B", 10},
                new Object[]{"SP003", "半成品C", 5},
                new Object[]{"SP004", "半成品D", -3}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("Row 5"),
                "Error should include row number 5: " + ex.getMessage());
    }

    @Test
    void testParseExcel_NumericStringAdvanceDays() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", "7"}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertEquals(7, rows.get(0).getAdvanceDays());
    }

    @Test
    void testParseExcel_LargeAdvanceDays() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"SP001", "半成品A", 365}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertEquals(365, rows.get(0).getAdvanceDays());
    }

    @Test
    void testParseExcel_WhitespaceInFields() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"  SP001  ", "  半成品A  ", 7}
        );
        List<SemiProductRow> rows = parser.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertEquals("SP001", rows.get(0).getProductCode(), "Whitespace should be trimmed from product_code");
        assertEquals("半成品A", rows.get(0).getProductName(), "Whitespace should be trimmed from product_name");
    }
}
