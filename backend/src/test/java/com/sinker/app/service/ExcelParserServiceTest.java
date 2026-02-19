package com.sinker.app.service;

import com.sinker.app.exception.ExcelParseException;
import com.sinker.app.service.ExcelParserService.SalesForecastRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelParserServiceTest {

    private ExcelParserService service;

    @BeforeEach
    void setUp() {
        service = new ExcelParserService();
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
                "中類名稱", "貨品規格", "品號", "品名", "庫位", "箱數小計"
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
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", 100.50},
                new Object[]{"零食類", "150g*12包", "P002", "樂事洋芋片", "B02", 50.00},
                new Object[]{"日用品", "1入", "P003", "牙刷", "C03", 30.00}
        );
        List<SalesForecastRow> rows = service.parse(createExcel(data));
        assertEquals(3, rows.size());
        assertEquals("P001", rows.get(0).getProductCode());
        assertEquals("飲料類", rows.get(0).getCategory());
        assertEquals("可口可樂", rows.get(0).getProductName());
    }

    @Test
    void testParseExcel_SkipsHeaderRow() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", 100.50},
                new Object[]{"零食類", "150g*12包", "P002", "樂事洋芋片", "B02", 50.00}
        );
        List<SalesForecastRow> rows = service.parse(createExcel(data));
        assertEquals(2, rows.size());
        // Verify header row was skipped (first data row has P001, not "品號")
        assertEquals("P001", rows.get(0).getProductCode());
    }

    @Test
    void testParseExcel_EmptyFile() throws IOException {
        Object[][] data = headerRow(); // Only header, no data
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("no data rows") || ex.getMessage().contains("empty"),
                "Should indicate empty file: " + ex.getMessage());
    }

    @Test
    void testParseExcel_MissingRequiredColumn_ProductCode() throws IOException {
        // Create Excel with blank product_code
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "", "可口可樂", "A01", 100.50}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("品號") || ex.getMessage().contains("product_code"),
                "Should mention product_code: " + ex.getMessage());
    }

    @Test
    void testParseExcel_InvalidQuantityFormat() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", "abc"}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("Row 2") || ex.getMessage().contains("quantity"),
                "Should mention row number and quantity: " + ex.getMessage());
    }

    @Test
    void testParseExcel_NegativeQuantity() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", -10.5}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("positive") || ex.getMessage().contains("Row 2"),
                "Should mention positive quantity: " + ex.getMessage());
    }

    @Test
    void testParseExcel_ChineseCharacters() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01倉庫", 100.50}
        );
        List<SalesForecastRow> rows = service.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertEquals("飲料類", rows.get(0).getCategory());
        assertEquals("600ml*24入", rows.get(0).getSpec());
        assertEquals("可口可樂", rows.get(0).getProductName());
        assertEquals("A01倉庫", rows.get(0).getWarehouseLocation());
    }

    @Test
    void testParseExcel_BlankOptionalFields() throws IOException {
        // category, spec, warehouse_location are optional (nullable)
        Object[][] data = withHeader(
                new Object[]{null, null, "P001", null, null, 100.50}
        );
        List<SalesForecastRow> rows = service.parse(createExcel(data));
        assertEquals(1, rows.size());
        assertNull(rows.get(0).getCategory());
        assertNull(rows.get(0).getSpec());
        assertNull(rows.get(0).getWarehouseLocation());
    }

    @Test
    void testParseExcel_BlankProductCode_ThrowsException() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", null, "可口可樂", "A01", 100.50}
        );
        MockMultipartFile file = createExcel(data);
        assertThrows(ExcelParseException.class, () -> service.parse(file));
    }

    @Test
    void testParseExcel_DecimalPrecision() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", 100.99}
        );
        List<SalesForecastRow> rows = service.parse(createExcel(data));
        assertEquals(1, rows.size());
        BigDecimal qty = rows.get(0).getQuantity();
        assertEquals(0, new BigDecimal("100.99").compareTo(qty),
                "Quantity should be exactly 100.99, got: " + qty);
    }

    @Test
    void testParseExcel_ZeroQuantity_Rejected() throws IOException {
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", 0.0}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("positive"), "Zero quantity should be rejected");
    }

    @Test
    void testParseExcel_InvalidFile_NotXlsx() {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file", "test.csv", "text/csv", "a,b,c".getBytes());
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(csvFile));
        assertTrue(ex.getMessage().contains("xlsx") || ex.getMessage().contains("format"),
                "Should reject non-xlsx file: " + ex.getMessage());
    }

    @Test
    void testParseExcel_EmptyMultipartFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(emptyFile));
        assertTrue(ex.getMessage().toLowerCase().contains("empty"),
                "Should reject empty file: " + ex.getMessage());
    }

    @Test
    void testParseExcel_RowNumberInError() throws IOException {
        // Row 5 (index 4) has negative quantity
        Object[][] data = withHeader(
                new Object[]{"飲料類", "600ml*24入", "P001", "可口可樂", "A01", 100.50},
                new Object[]{"飲料類", "600ml*24入", "P002", "可口可樂", "A01", 50.00},
                new Object[]{"飲料類", "600ml*24入", "P003", "可口可樂", "A01", 30.00},
                new Object[]{"飲料類", "600ml*24入", "P004", "可口可樂", "A01", -5.00}
        );
        MockMultipartFile file = createExcel(data);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> service.parse(file));
        assertTrue(ex.getMessage().contains("Row 5"),
                "Error should include row number 5: " + ex.getMessage());
    }
}
