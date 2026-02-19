package com.sinker.app.service;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    // Expected column indices (0-based)
    private static final int COL_CATEGORY = 0;
    private static final int COL_SPEC = 1;
    private static final int COL_PRODUCT_CODE = 2;
    private static final int COL_PRODUCT_NAME = 3;
    private static final int COL_WAREHOUSE_LOCATION = 4;
    private static final int COL_QUANTITY = 5;
    private static final int MIN_COLUMNS = 6;

    public static class SalesForecastRow {
        private final String category;
        private final String spec;
        private final String productCode;
        private final String productName;
        private final String warehouseLocation;
        private final BigDecimal quantity;
        private final int rowNumber;

        public SalesForecastRow(String category, String spec, String productCode,
                                String productName, String warehouseLocation,
                                BigDecimal quantity, int rowNumber) {
            this.category = category;
            this.spec = spec;
            this.productCode = productCode;
            this.productName = productName;
            this.warehouseLocation = warehouseLocation;
            this.quantity = quantity;
            this.rowNumber = rowNumber;
        }

        public String getCategory() { return category; }
        public String getSpec() { return spec; }
        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public String getWarehouseLocation() { return warehouseLocation; }
        public BigDecimal getQuantity() { return quantity; }
        public int getRowNumber() { return rowNumber; }
    }

    public List<SalesForecastRow> parse(MultipartFile file) {
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

    private List<SalesForecastRow> parseSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            throw new ExcelParseException("Excel file has no data rows (only header or empty)");
        }

        // Validate header row
        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() < MIN_COLUMNS) {
            throw new ExcelParseException("Excel file is missing required columns. Expected 6 columns: 中類名稱, 貨品規格, 品號, 品名, 庫位, 箱數小計");
        }

        List<SalesForecastRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            try {
                SalesForecastRow parsed = parseRow(row, i + 1);
                rows.add(parsed);
            } catch (ExcelParseException e) {
                errors.addAll(e.getErrors());
            }
        }

        if (!errors.isEmpty()) {
            throw new ExcelParseException(errors);
        }

        return rows;
    }

    private SalesForecastRow parseRow(Row row, int rowNumber) {
        String category = getStringCell(row, COL_CATEGORY);
        String spec = getStringCell(row, COL_SPEC);
        String productCode = getStringCell(row, COL_PRODUCT_CODE);
        String productName = getStringCell(row, COL_PRODUCT_NAME);
        String warehouseLocation = getStringCell(row, COL_WAREHOUSE_LOCATION);

        if (productCode == null || productCode.isBlank()) {
            throw new ExcelParseException("Row " + rowNumber + ": 品號 (product_code) is required");
        }

        BigDecimal quantity = parseQuantity(row, rowNumber);

        return new SalesForecastRow(category, spec, productCode, productName,
                warehouseLocation, quantity, rowNumber);
    }

    private BigDecimal parseQuantity(Row row, int rowNumber) {
        Cell cell = row.getCell(COL_QUANTITY);
        if (cell == null) {
            throw new ExcelParseException("Row " + rowNumber + ": 箱數小計 (quantity) is required");
        }

        BigDecimal quantity;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                quantity = BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (val.isEmpty()) {
                    throw new ExcelParseException("Row " + rowNumber + ": 箱數小計 (quantity) is required");
                }
                quantity = new BigDecimal(val);
            } else {
                throw new ExcelParseException("Row " + rowNumber + ": 箱數小計 (quantity) must be a number");
            }
        } catch (NumberFormatException e) {
            throw new ExcelParseException("Row " + rowNumber + ": 箱數小計 (quantity) must be a valid number");
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExcelParseException("Row " + rowNumber + ": 箱數小計 (quantity) must be positive");
        }

        return quantity;
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
