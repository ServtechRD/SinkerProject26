package com.sinker.app.service;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class WeeklyScheduleExcelParser {

    private static final Logger log = LoggerFactory.getLogger(WeeklyScheduleExcelParser.class);

    // Chinese column headers
    private static final String HEADER_DEMAND_DATE = "需求日期";
    private static final String HEADER_PRODUCT_CODE = "品號";
    private static final String HEADER_PRODUCT_NAME = "品名";
    private static final String HEADER_WAREHOUSE_LOCATION = "庫位";
    private static final String HEADER_QUANTITY = "箱數小計";

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            HEADER_DEMAND_DATE,
            HEADER_PRODUCT_CODE,
            HEADER_PRODUCT_NAME,
            HEADER_WAREHOUSE_LOCATION,
            HEADER_QUANTITY
    );

    public static class WeeklyScheduleRow {
        private final LocalDate demandDate;
        private final String productCode;
        private final String productName;
        private final String warehouseLocation;
        private final BigDecimal quantity;
        private final int rowNumber;

        public WeeklyScheduleRow(LocalDate demandDate, String productCode, String productName,
                                 String warehouseLocation, BigDecimal quantity, int rowNumber) {
            this.demandDate = demandDate;
            this.productCode = productCode;
            this.productName = productName;
            this.warehouseLocation = warehouseLocation;
            this.quantity = quantity;
            this.rowNumber = rowNumber;
        }

        public LocalDate getDemandDate() { return demandDate; }
        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public String getWarehouseLocation() { return warehouseLocation; }
        public BigDecimal getQuantity() { return quantity; }
        public int getRowNumber() { return rowNumber; }
    }

    public List<WeeklyScheduleRow> parse(MultipartFile file) {
        validateFileFormat(file);

        try (Workbook workbook = createWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ExcelParseException("Excel file contains no sheets");
            }
            return parseSheet(sheet);
        } catch (IOException e) {
            log.error("Failed to read Excel file", e);
            throw new ExcelParseException("Invalid Excel file format or corrupted file");
        }
    }

    private void validateFileFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new ExcelParseException("Filename is missing");
        }
        String lowerFilename = filename.toLowerCase();
        if (!lowerFilename.endsWith(".xlsx") && !lowerFilename.endsWith(".xls")) {
            throw new ExcelParseException("Invalid file format. Only .xlsx and .xls files are accepted");
        }
    }

    private Workbook createWorkbook(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(file.getInputStream());
        } else {
            return new HSSFWorkbook(file.getInputStream());
        }
    }

    private List<WeeklyScheduleRow> parseSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            throw new ExcelParseException("Excel file has no data rows (only header or empty)");
        }

        // Parse header row to get column indices
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new ExcelParseException("Excel file is missing header row");
        }

        Map<String, Integer> columnIndices = parseHeaderRow(headerRow);
        validateRequiredColumns(columnIndices);

        // Parse data rows
        List<WeeklyScheduleRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            try {
                WeeklyScheduleRow scheduleRow = parseDataRow(row, columnIndices, i + 1);
                rows.add(scheduleRow);
            } catch (Exception e) {
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new ExcelParseException(errors);
        }

        if (rows.isEmpty()) {
            throw new ExcelParseException("Excel file has no valid data rows");
        }

        log.info("Parsed {} rows from Excel file", rows.size());
        return rows;
    }

    private Map<String, Integer> parseHeaderRow(Row headerRow) {
        Map<String, Integer> columnIndices = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellStringValue(cell).trim();
                if (!headerValue.isEmpty()) {
                    columnIndices.put(headerValue, i);
                }
            }
        }
        return columnIndices;
    }

    private void validateRequiredColumns(Map<String, Integer> columnIndices) {
        List<String> missingColumns = new ArrayList<>();
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!columnIndices.containsKey(requiredHeader)) {
                missingColumns.add(requiredHeader);
            }
        }

        if (!missingColumns.isEmpty()) {
            throw new ExcelParseException("Excel file missing required columns: " + String.join(", ", missingColumns));
        }
    }

    private WeeklyScheduleRow parseDataRow(Row row, Map<String, Integer> columnIndices, int rowNumber) {
        LocalDate demandDate = parseDateCell(row, columnIndices.get(HEADER_DEMAND_DATE), "需求日期");
        String productCode = parseStringCell(row, columnIndices.get(HEADER_PRODUCT_CODE), "品號");
        String productName = parseStringCell(row, columnIndices.get(HEADER_PRODUCT_NAME), "品名");
        String warehouseLocation = parseStringCell(row, columnIndices.get(HEADER_WAREHOUSE_LOCATION), "庫位");
        BigDecimal quantity = parseNumericCell(row, columnIndices.get(HEADER_QUANTITY), "箱數小計");

        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("箱數小計 must be >= 0");
        }

        return new WeeklyScheduleRow(demandDate, productCode, productName, warehouseLocation, quantity, rowNumber);
    }

    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellStringValue(cell).trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private LocalDate parseDateCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            throw new IllegalArgumentException(columnName + " is required");
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } else if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                return LocalDate.parse(dateStr);
            } else {
                throw new IllegalArgumentException(columnName + " must be a valid date");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(columnName + " invalid format: " + e.getMessage());
        }
    }

    private String parseStringCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            throw new IllegalArgumentException(columnName + " is required");
        }

        String value = getCellStringValue(cell).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(columnName + " is required");
        }
        return value;
    }

    private BigDecimal parseNumericCell(Row row, int columnIndex, String columnName) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            throw new IllegalArgumentException(columnName + " is required");
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                return new BigDecimal(value);
            } else {
                throw new IllegalArgumentException(columnName + " must be a number");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(columnName + " invalid number format");
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
