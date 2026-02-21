package com.sinker.app.util;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SemiProductExcelParser {

    private static final Logger log = LoggerFactory.getLogger(SemiProductExcelParser.class);

    // Expected column indices (0-based)
    private static final int COL_PRODUCT_CODE = 0;  // 品號
    private static final int COL_PRODUCT_NAME = 1;  // 品名
    private static final int COL_ADVANCE_DAYS = 2;  // 提前日數
    private static final int MIN_COLUMNS = 3;

    public static class SemiProductRow {
        private final String productCode;
        private final String productName;
        private final Integer advanceDays;
        private final int rowNumber;

        public SemiProductRow(String productCode, String productName, Integer advanceDays, int rowNumber) {
            this.productCode = productCode;
            this.productName = productName;
            this.advanceDays = advanceDays;
            this.rowNumber = rowNumber;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public Integer getAdvanceDays() { return advanceDays; }
        public int getRowNumber() { return rowNumber; }
    }

    public List<SemiProductRow> parse(MultipartFile file) {
        validateFileFormat(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ExcelParseException("Excel file contains no sheets");
            }
            return parseSheet(sheet);
        } catch (IOException e) {
            throw new ExcelParseException("Failed to read Excel file: " + e.getMessage());
        }
    }

    private void validateFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelParseException("Invalid file format. Only .xlsx files are accepted");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.contains("spreadsheet") && !contentType.contains("octet-stream")) {
            throw new ExcelParseException("Invalid file content type: " + contentType);
        }
    }

    private List<SemiProductRow> parseSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            throw new ExcelParseException("Excel file has no data rows (only header or empty)");
        }

        // Validate header row
        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() < MIN_COLUMNS) {
            throw new ExcelParseException("Excel file is missing required columns. Expected 3 columns: 品號, 品名, 提前日數");
        }

        List<SemiProductRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            try {
                SemiProductRow parsed = parseRow(row, i + 1);
                rows.add(parsed);
            } catch (ExcelParseException e) {
                errors.addAll(e.getErrors());
            }
        }

        if (!errors.isEmpty()) {
            throw new ExcelParseException(errors);
        }

        if (rows.isEmpty()) {
            throw new ExcelParseException("Excel file contains no valid data rows");
        }

        return rows;
    }

    private SemiProductRow parseRow(Row row, int rowNumber) {
        String productCode = getStringCell(row, COL_PRODUCT_CODE);
        String productName = getStringCell(row, COL_PRODUCT_NAME);
        Integer advanceDays = parseAdvanceDays(row, rowNumber);

        List<String> rowErrors = new ArrayList<>();

        if (productCode == null || productCode.isBlank()) {
            rowErrors.add("Row " + rowNumber + ": product_code is required");
        }
        if (productName == null || productName.isBlank()) {
            rowErrors.add("Row " + rowNumber + ": product_name is required");
        }

        if (!rowErrors.isEmpty()) {
            throw new ExcelParseException(rowErrors);
        }

        return new SemiProductRow(productCode.trim(), productName.trim(), advanceDays, rowNumber);
    }

    private Integer parseAdvanceDays(Row row, int rowNumber) {
        Cell cell = row.getCell(COL_ADVANCE_DAYS);
        if (cell == null) {
            throw new ExcelParseException("Row " + rowNumber + ": advance_days is required");
        }

        Integer advanceDays;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                advanceDays = (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) {
                    throw new ExcelParseException("Row " + rowNumber + ": advance_days is required");
                }
                advanceDays = Integer.parseInt(val);
            } else {
                throw new ExcelParseException("Row " + rowNumber + ": advance_days must be a number");
            }
        } catch (NumberFormatException e) {
            throw new ExcelParseException("Row " + rowNumber + ": advance_days must be a valid integer");
        }

        if (advanceDays <= 0) {
            throw new ExcelParseException("Row " + rowNumber + ": advance_days must be positive");
        }

        return advanceDays;
    }

    private String getStringCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                String val = cell.getStringCellValue();
                return val.isEmpty() ? null : val;
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < MIN_COLUMNS; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
