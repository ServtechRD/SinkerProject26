package com.sinker.app.util;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaterialDemandExcelParser {

    private static final int COL_MATERIAL_CODE = 0;   // 品號
    private static final int COL_MATERIAL_NAME = 1;   // 品名
    private static final int COL_UNIT = 2;             // 單位
    private static final int COL_LAST_PURCHASE = 3;   // 上次進貨日
    private static final int COL_DEMAND_DATE = 4;     // 需求日
    private static final int COL_EXPECTED_DELIVERY = 5; // 預交量
    private static final int COL_DEMAND_QUANTITY = 6;  // 需求量
    private static final int COL_ESTIMATED_INVENTORY = 7; // 預計庫存量
    private static final int MIN_COLUMNS = 8;

    public static class MaterialDemandRow {
        private final String materialCode;
        private final String materialName;
        private final String unit;
        private final LocalDate lastPurchaseDate;
        private final LocalDate demandDate;
        private final BigDecimal expectedDelivery;
        private final BigDecimal demandQuantity;
        private final BigDecimal estimatedInventory;

        public MaterialDemandRow(String materialCode, String materialName, String unit,
                                 LocalDate lastPurchaseDate, LocalDate demandDate,
                                 BigDecimal expectedDelivery, BigDecimal demandQuantity, BigDecimal estimatedInventory) {
            this.materialCode = materialCode;
            this.materialName = materialName;
            this.unit = unit;
            this.lastPurchaseDate = lastPurchaseDate;
            this.demandDate = demandDate;
            this.expectedDelivery = expectedDelivery;
            this.demandQuantity = demandQuantity;
            this.estimatedInventory = estimatedInventory;
        }

        public String getMaterialCode() { return materialCode; }
        public String getMaterialName() { return materialName; }
        public String getUnit() { return unit; }
        public LocalDate getLastPurchaseDate() { return lastPurchaseDate; }
        public LocalDate getDemandDate() { return demandDate; }
        public BigDecimal getExpectedDelivery() { return expectedDelivery; }
        public BigDecimal getDemandQuantity() { return demandQuantity; }
        public BigDecimal getEstimatedInventory() { return estimatedInventory; }
    }

    public List<MaterialDemandRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelParseException("File is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new ExcelParseException("Invalid file format. Only .xlsx files are accepted");
        }
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

    private List<MaterialDemandRow> parseSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            throw new ExcelParseException("Excel file has no data rows");
        }
        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() < MIN_COLUMNS) {
            throw new ExcelParseException("Excel must have 8 columns: 品號,品名,單位,上次進貨日,需求日,預交量,需求量,預計庫存量");
        }
        List<MaterialDemandRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            try {
                MaterialDemandRow parsed = parseRow(row, i + 1);
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

    private MaterialDemandRow parseRow(Row row, int rowNumber) {
        String materialCode = getStringCell(row, COL_MATERIAL_CODE);
        String materialName = getStringCell(row, COL_MATERIAL_NAME);
        String unit = getStringCell(row, COL_UNIT);
        if (materialCode == null || materialCode.isBlank()) {
            throw new ExcelParseException("Row " + rowNumber + ": 品號 is required");
        }
        if (materialName == null || materialName.isBlank()) {
            throw new ExcelParseException("Row " + rowNumber + ": 品名 is required");
        }
        if (unit == null || unit.isBlank()) {
            unit = "PCS";
        }
        LocalDate lastPurchaseDate = getDateCell(row, COL_LAST_PURCHASE);
        LocalDate demandDate = getDateCell(row, COL_DEMAND_DATE);
        if (demandDate == null) {
            throw new ExcelParseException("Row " + rowNumber + ": 需求日 is required");
        }
        BigDecimal expectedDelivery = getDecimalCell(row, COL_EXPECTED_DELIVERY, rowNumber, "預交量");
        BigDecimal demandQuantity = getDecimalCell(row, COL_DEMAND_QUANTITY, rowNumber, "需求量");
        BigDecimal estimatedInventory = getDecimalCell(row, COL_ESTIMATED_INVENTORY, rowNumber, "預計庫存量");
        if (expectedDelivery == null) expectedDelivery = BigDecimal.ZERO;
        if (demandQuantity == null) demandQuantity = BigDecimal.ZERO;
        if (estimatedInventory == null) estimatedInventory = BigDecimal.ZERO;
        return new MaterialDemandRow(
                materialCode.trim(), materialName.trim(), unit.trim(),
                lastPurchaseDate, demandDate,
                expectedDelivery, demandQuantity, estimatedInventory
        );
    }

    private String getStringCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                String v = cell.getStringCellValue();
                return v == null || v.isEmpty() ? null : v;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                    } catch (Exception e) {
                        return String.valueOf((long) cell.getNumericCellValue());
                    }
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
    }

    private LocalDate getDateCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue();
                if (s != null && !s.trim().isEmpty()) {
                    return LocalDate.parse(s.trim());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private BigDecimal getDecimalCell(Row row, int col, int rowNumber, String fieldName) {
        Cell cell = row.getCell(col);
        if (cell == null) return BigDecimal.ZERO;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue();
                if (s != null && !s.trim().isEmpty()) {
                    return new BigDecimal(s.trim());
                }
            }
        } catch (NumberFormatException e) {
            throw new ExcelParseException("Row " + rowNumber + ": " + fieldName + " must be a number");
        }
        return BigDecimal.ZERO;
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < MIN_COLUMNS; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = getStringCell(row, i);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }
}
