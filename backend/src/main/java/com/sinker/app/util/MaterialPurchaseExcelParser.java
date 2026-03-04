package com.sinker.app.util;

import com.sinker.app.exception.ExcelParseException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaterialPurchaseExcelParser {

    private static final int COL_PRODUCT_CODE = 0;      // 品號
    private static final int COL_PRODUCT_NAME = 1;      // 品名
    private static final int COL_QUANTITY = 2;          // 箱數小計
    private static final int COL_SEMI_PRODUCT_NAME = 3; // 半成品名稱
    private static final int COL_SEMI_PRODUCT_CODE = 4;  // 半成品編號
    private static final int COL_KG_PER_BOX = 5;         // 公斤/箱
    private static final int COL_BASKET_QUANTITY = 6;   // 籃數
    private static final int COL_BOXES_PER_BARREL = 7;  // 箱/桶
    private static final int COL_REQUIRED_BARRELS = 8;  // 所需桶數
    private static final int MIN_COLUMNS = 9;

    public static class MaterialPurchaseRow {
        private final String productCode;
        private final String productName;
        private final BigDecimal quantity;
        private final String semiProductName;
        private final String semiProductCode;
        private final BigDecimal kgPerBox;
        private final BigDecimal basketQuantity;
        private final BigDecimal boxesPerBarrel;
        private final BigDecimal requiredBarrels;

        public MaterialPurchaseRow(String productCode, String productName, BigDecimal quantity,
                                   String semiProductName, String semiProductCode,
                                   BigDecimal kgPerBox, BigDecimal basketQuantity,
                                   BigDecimal boxesPerBarrel, BigDecimal requiredBarrels) {
            this.productCode = productCode;
            this.productName = productName;
            this.quantity = quantity;
            this.semiProductName = semiProductName;
            this.semiProductCode = semiProductCode;
            this.kgPerBox = kgPerBox;
            this.basketQuantity = basketQuantity;
            this.boxesPerBarrel = boxesPerBarrel;
            this.requiredBarrels = requiredBarrels;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public BigDecimal getQuantity() { return quantity; }
        public String getSemiProductName() { return semiProductName; }
        public String getSemiProductCode() { return semiProductCode; }
        public BigDecimal getKgPerBox() { return kgPerBox; }
        public BigDecimal getBasketQuantity() { return basketQuantity; }
        public BigDecimal getBoxesPerBarrel() { return boxesPerBarrel; }
        public BigDecimal getRequiredBarrels() { return requiredBarrels; }
    }

    public List<MaterialPurchaseRow> parse(MultipartFile file) {
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

    private List<MaterialPurchaseRow> parseSheet(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 1) {
            throw new ExcelParseException("Excel file has no data rows");
        }
        Row headerRow = sheet.getRow(0);
        if (headerRow == null || headerRow.getLastCellNum() < MIN_COLUMNS) {
            throw new ExcelParseException("Excel must have 9 columns: 品號,品名,箱數小計,半成品名稱,半成品編號,公斤/箱,籃數,箱/桶,所需桶數");
        }
        List<MaterialPurchaseRow> rows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            try {
                MaterialPurchaseRow parsed = parseRow(row, i + 1);
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

    private MaterialPurchaseRow parseRow(Row row, int rowNumber) {
        String productCode = getStringCell(row, COL_PRODUCT_CODE);
        String productName = getStringCell(row, COL_PRODUCT_NAME);
        if (productCode == null || productCode.isBlank()) {
            throw new ExcelParseException("Row " + rowNumber + ": 品號 is required");
        }
        if (productName == null || productName.isBlank()) {
            throw new ExcelParseException("Row " + rowNumber + ": 品名 is required");
        }
        BigDecimal quantity = getDecimalCell(row, COL_QUANTITY, rowNumber, "箱數小計");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            quantity = BigDecimal.ZERO;
        }
        String semiProductName = getStringCell(row, COL_SEMI_PRODUCT_NAME);
        String semiProductCode = getStringCell(row, COL_SEMI_PRODUCT_CODE);
        if (semiProductName == null) semiProductName = "";
        if (semiProductCode == null) semiProductCode = "";
        BigDecimal kgPerBox = getDecimalCell(row, COL_KG_PER_BOX, rowNumber, "公斤/箱");
        BigDecimal basketQuantity = getDecimalCell(row, COL_BASKET_QUANTITY, rowNumber, "籃數");
        BigDecimal boxesPerBarrel = getDecimalCell(row, COL_BOXES_PER_BARREL, rowNumber, "箱/桶");
        BigDecimal requiredBarrels = getDecimalCell(row, COL_REQUIRED_BARRELS, rowNumber, "所需桶數");
        if (kgPerBox == null) kgPerBox = BigDecimal.ZERO;
        if (basketQuantity == null) basketQuantity = BigDecimal.ZERO;
        if (boxesPerBarrel == null) boxesPerBarrel = BigDecimal.ZERO;
        if (requiredBarrels == null) requiredBarrels = BigDecimal.ZERO;
        return new MaterialPurchaseRow(
                productCode.trim(), productName.trim(), quantity,
                semiProductName.trim(), semiProductCode.trim(),
                kgPerBox, basketQuantity, boxesPerBarrel, requiredBarrels
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
                return String.valueOf((long) cell.getNumericCellValue());
            case BLANK:
                return null;
            default:
                return cell.toString();
        }
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
        for (int i = 0; i < 3; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String v = getStringCell(row, i);
                if (v != null && !v.trim().isEmpty()) return false;
            }
        }
        return true;
    }
}
