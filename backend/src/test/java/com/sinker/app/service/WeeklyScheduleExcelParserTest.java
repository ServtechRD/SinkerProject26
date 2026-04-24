package com.sinker.app.service;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyScheduleExcelParserTest {

    private WeeklyScheduleExcelParser parser;

    @BeforeEach
    void setUp() {
        parser = new WeeklyScheduleExcelParser();
    }

    @Test
    void parse_validWorkbook_returnsParsedRows() throws Exception {
        MultipartFile file = buildWorkbook(List.of(
                List.of(LocalDate.of(2026, 1, 15), "P001", "N1", "A01", 10),
                List.of(LocalDate.of(2026, 1, 16), "P002", "N2", "A02", 20)
        ));

        List<WeeklyScheduleExcelParser.WeeklyScheduleRow> rows = parser.parse(file);

        assertEquals(2, rows.size());
        assertEquals("P001", rows.get(0).getProductCode());
        assertEquals(0, new BigDecimal("10.0").compareTo(rows.get(0).getQuantity()));
        assertEquals(2, rows.get(0).getRowNumber());
    }

    @Test
    void parse_missingRequiredHeader_throws() throws Exception {
        MultipartFile file = buildWorkbookWithoutQuantityHeader();

        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));

        assertTrue(ex.getMessage().contains("missing required columns"));
        assertTrue(ex.getMessage().contains("箱數小計"));
    }

    @Test
    void parse_negativeQuantity_throws() throws Exception {
        MultipartFile file = buildWorkbook(List.of(
                List.of(LocalDate.of(2026, 1, 15), "P001", "N1", "A01", -1)
        ));

        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(file));
        assertTrue(ex.getMessage().contains("must be >= 0"));
    }

    @Test
    void parse_skipsEmptyRows() throws Exception {
        MultipartFile file = buildWorkbookWithEmptyRows();

        List<WeeklyScheduleExcelParser.WeeklyScheduleRow> rows = parser.parse(file);

        assertEquals(1, rows.size());
        assertEquals("P001", rows.get(0).getProductCode());
    }

    @Test
    void parse_invalidExtension_throws() {
        MockMultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(txt));
        assertTrue(ex.getMessage().contains("Only .xlsx and .xls"));
    }

    @Test
    void parse_emptyFile_throws() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.xlsx", "application/xlsx", new byte[0]);
        ExcelParseException ex = assertThrows(ExcelParseException.class, () -> parser.parse(empty));
        assertEquals("File is empty", ex.getMessage());
    }

    private MultipartFile buildWorkbook(List<List<Object>> rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("需求日期");
            h.createCell(1).setCellValue("品號");
            h.createCell(2).setCellValue("品名");
            h.createCell(3).setCellValue("庫位");
            h.createCell(4).setCellValue("箱數小計");

            int r = 1;
            for (List<Object> data : rows) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(((LocalDate) data.get(0)).toString());
                row.createCell(1).setCellValue((String) data.get(1));
                row.createCell(2).setCellValue((String) data.get(2));
                row.createCell(3).setCellValue((String) data.get(3));
                row.createCell(4).setCellValue(((Number) data.get(4)).doubleValue());
            }

            workbook.write(bos);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());
        }
    }

    private MultipartFile buildWorkbookWithoutQuantityHeader() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("需求日期");
            h.createCell(1).setCellValue("品號");
            h.createCell(2).setCellValue("品名");
            h.createCell(3).setCellValue("庫位");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(LocalDate.of(2026, 1, 15).toString());
            row.createCell(1).setCellValue("P001");
            row.createCell(2).setCellValue("N1");
            row.createCell(3).setCellValue("A01");
            workbook.write(bos);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());
        }
    }

    private MultipartFile buildWorkbookWithEmptyRows() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("需求日期");
            h.createCell(1).setCellValue("品號");
            h.createCell(2).setCellValue("品名");
            h.createCell(3).setCellValue("庫位");
            h.createCell(4).setCellValue("箱數小計");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(LocalDate.of(2026, 1, 15).toString());
            r1.createCell(1).setCellValue("P001");
            r1.createCell(2).setCellValue("N1");
            r1.createCell(3).setCellValue("A01");
            r1.createCell(4).setCellValue(10);

            sheet.createRow(2);
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("");
            r3.createCell(1).setCellValue("");

            workbook.write(bos);
            return new MockMultipartFile("file", "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());
        }
    }
}
