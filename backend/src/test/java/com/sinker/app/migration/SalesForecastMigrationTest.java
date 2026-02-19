package com.sinker.app.migration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SalesForecastMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM sales_forecast WHERE product_code LIKE 'T014%'");
    }

    // 1. testMigrationV5ExecutesSuccessfully
    @Test
    void testMigrationV5ExecutesSuccessfully() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version = '5'");
        assertEquals(1, history.size(), "V5 migration should exist in flyway history");
        Object success = history.get(0).get("success");
        assertTrue(Boolean.TRUE.equals(success) || Integer.valueOf(1).equals(success),
                "V5 migration should be successful");
    }

    // 2. testSalesForecastTableExists
    @Test
    void testSalesForecastTableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast'",
                Integer.class);
        assertEquals(1, count, "Table sales_forecast should exist");
    }

    // 3. testTableSchemaCorrect
    @Test
    void testTableSchemaCorrect() {
        List<Map<String, Object>> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast' " +
                "ORDER BY ORDINAL_POSITION");

        assertEquals(13, columns.size(), "sales_forecast should have 13 columns");

        Map<String, Map<String, Object>> colMap = new java.util.LinkedHashMap<>();
        for (Map<String, Object> col : columns) {
            colMap.put(col.get("COLUMN_NAME").toString(), col);
        }

        // id: int, PK, auto_increment
        assertTrue(colMap.containsKey("id"), "Column id should exist");
        assertEquals("int", colMap.get("id").get("COLUMN_TYPE").toString().toLowerCase().split("\\(")[0].trim());
        assertEquals("auto_increment", colMap.get("id").get("EXTRA").toString().toLowerCase());

        // month: varchar(7), not null
        assertTrue(colMap.containsKey("month"), "Column month should exist");
        assertEquals("varchar(7)", colMap.get("month").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("month").get("IS_NULLABLE").toString());

        // channel: varchar(50), not null
        assertTrue(colMap.containsKey("channel"), "Column channel should exist");
        assertEquals("varchar(50)", colMap.get("channel").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("channel").get("IS_NULLABLE").toString());

        // category: varchar(100), nullable
        assertTrue(colMap.containsKey("category"), "Column category should exist");
        assertEquals("varchar(100)", colMap.get("category").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("YES", colMap.get("category").get("IS_NULLABLE").toString());

        // spec: varchar(200), nullable
        assertTrue(colMap.containsKey("spec"), "Column spec should exist");
        assertEquals("varchar(200)", colMap.get("spec").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("YES", colMap.get("spec").get("IS_NULLABLE").toString());

        // product_code: varchar(50), not null
        assertTrue(colMap.containsKey("product_code"), "Column product_code should exist");
        assertEquals("varchar(50)", colMap.get("product_code").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("product_code").get("IS_NULLABLE").toString());

        // product_name: varchar(200), nullable
        assertTrue(colMap.containsKey("product_name"), "Column product_name should exist");
        assertEquals("varchar(200)", colMap.get("product_name").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("YES", colMap.get("product_name").get("IS_NULLABLE").toString());

        // warehouse_location: varchar(50), nullable
        assertTrue(colMap.containsKey("warehouse_location"), "Column warehouse_location should exist");
        assertEquals("varchar(50)", colMap.get("warehouse_location").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("YES", colMap.get("warehouse_location").get("IS_NULLABLE").toString());

        // quantity: decimal(10,2), not null
        assertTrue(colMap.containsKey("quantity"), "Column quantity should exist");
        assertEquals("decimal(10,2)", colMap.get("quantity").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("quantity").get("IS_NULLABLE").toString());

        // version: varchar(100), not null
        assertTrue(colMap.containsKey("version"), "Column version should exist");
        assertEquals("varchar(100)", colMap.get("version").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("version").get("IS_NULLABLE").toString());

        // is_modified: tinyint(1), not null, default 0
        assertTrue(colMap.containsKey("is_modified"), "Column is_modified should exist");
        assertEquals("NO", colMap.get("is_modified").get("IS_NULLABLE").toString());
        assertNotNull(colMap.get("is_modified").get("COLUMN_DEFAULT"), "is_modified should have default");

        // created_at: timestamp, not null
        assertTrue(colMap.containsKey("created_at"), "Column created_at should exist");
        assertEquals("timestamp", colMap.get("created_at").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("created_at").get("IS_NULLABLE").toString());

        // updated_at: timestamp, not null, on update
        assertTrue(colMap.containsKey("updated_at"), "Column updated_at should exist");
        assertEquals("timestamp", colMap.get("updated_at").get("COLUMN_TYPE").toString().toLowerCase());
        assertEquals("NO", colMap.get("updated_at").get("IS_NULLABLE").toString());
        assertTrue(colMap.get("updated_at").get("EXTRA").toString().toLowerCase().contains("on update"),
                "updated_at should have ON UPDATE CURRENT_TIMESTAMP");
    }

    // 4. testAllIndexesCreated
    @Test
    void testAllIndexesCreated() {
        List<Map<String, Object>> indexes = jdbc.queryForList(
                "SELECT INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX " +
                "FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast' " +
                "ORDER BY INDEX_NAME, SEQ_IN_INDEX");

        // Build a map of index -> list of columns in order
        Map<String, List<String>> indexColumns = new java.util.LinkedHashMap<>();
        for (Map<String, Object> row : indexes) {
            String indexName = row.get("INDEX_NAME").toString();
            String colName = row.get("COLUMN_NAME").toString();
            indexColumns.computeIfAbsent(indexName, k -> new java.util.ArrayList<>()).add(colName);
        }

        // PRIMARY on (id)
        assertTrue(indexColumns.containsKey("PRIMARY"), "PRIMARY key index should exist");
        assertEquals(List.of("id"), indexColumns.get("PRIMARY"));

        // idx_month_channel on (month, channel)
        assertTrue(indexColumns.containsKey("idx_month_channel"), "Index idx_month_channel should exist");
        assertEquals(List.of("month", "channel"), indexColumns.get("idx_month_channel"));

        // idx_product_code on (product_code)
        assertTrue(indexColumns.containsKey("idx_product_code"), "Index idx_product_code should exist");
        assertEquals(List.of("product_code"), indexColumns.get("idx_product_code"));

        // idx_version on (version)
        assertTrue(indexColumns.containsKey("idx_version"), "Index idx_version should exist");
        assertEquals(List.of("version"), indexColumns.get("idx_version"));

        // idx_month_channel_product on (month, channel, product_code)
        assertTrue(indexColumns.containsKey("idx_month_channel_product"), "Index idx_month_channel_product should exist");
        assertEquals(List.of("month", "channel", "product_code"), indexColumns.get("idx_month_channel_product"));
    }

    // 5. testCharsetIsUtf8mb4
    @Test
    void testCharsetIsUtf8mb4() {
        Map<String, Object> tableInfo = jdbc.queryForMap(
                "SELECT TABLE_COLLATION FROM INFORMATION_SCHEMA.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sales_forecast'");
        String collation = tableInfo.get("TABLE_COLLATION").toString();
        assertTrue(collation.startsWith("utf8mb4"), "Table charset should be utf8mb4, got: " + collation);
        assertEquals("utf8mb4_unicode_ci", collation, "Table collation should be utf8mb4_unicode_ci");
    }

    // 6. testInsertWithChineseCharacters
    @Test
    void testInsertWithChineseCharacters() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, warehouse_location, quantity, version) " +
                "VALUES ('202601', '大全聯', '飲料類', '600ml*24入', 'T014P001', '可口可樂', 'A01', 100.50, '2026/01/15 10:30:00(大全聯)')");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT channel, category, product_name, warehouse_location, version FROM sales_forecast WHERE product_code = 'T014P001'");

        assertEquals("大全聯", row.get("channel"), "channel should store Chinese characters");
        assertEquals("飲料類", row.get("category"), "category should store Chinese characters");
        assertEquals("可口可樂", row.get("product_name"), "product_name should store Chinese characters");
        assertEquals("A01", row.get("warehouse_location"));
        assertEquals("2026/01/15 10:30:00(大全聯)", row.get("version"), "version should store Chinese characters");
    }

    // 7. testDecimalPrecision
    @Test
    void testDecimalPrecision() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202601', '家樂福', 'T014P002', 12345678.99, '2026/01/01 00:00:00(家樂福)')");
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202601', '家樂福', 'T014P003', 0.01, '2026/01/01 00:00:00(家樂福)')");

        BigDecimal qty1 = jdbc.queryForObject(
                "SELECT quantity FROM sales_forecast WHERE product_code = 'T014P002'", BigDecimal.class);
        BigDecimal qty2 = jdbc.queryForObject(
                "SELECT quantity FROM sales_forecast WHERE product_code = 'T014P003'", BigDecimal.class);

        assertEquals(new BigDecimal("12345678.99"), qty1, "Decimal precision should be exact for 12345678.99");
        assertEquals(new BigDecimal("0.01"), qty2, "Decimal precision should be exact for 0.01");
    }

    // 8. testDefaultValues
    @Test
    void testDefaultValues() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202602', '家樂福', 'T014P004', 50.00, '2026/02/01 08:00:00(家樂福)')");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_modified, created_at, updated_at FROM sales_forecast WHERE product_code = 'T014P004'");

        Object isModified = row.get("is_modified");
        assertFalse(Boolean.TRUE.equals(isModified) || Integer.valueOf(1).equals(isModified),
                "is_modified should default to FALSE (0)");
        assertNotNull(row.get("created_at"), "created_at should be auto-populated");
        assertNotNull(row.get("updated_at"), "updated_at should be auto-populated");
    }

    // 9. testAutoUpdateTimestamp
    @Test
    void testAutoUpdateTimestamp() throws InterruptedException {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202602', 'PX/大全聯', 'T014P005', 30.00, '2026/02/01 09:00:00(PX/大全聯)')");

        Thread.sleep(1100);

        jdbc.update("UPDATE sales_forecast SET quantity = 75.00 WHERE product_code = 'T014P005'");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT created_at, updated_at FROM sales_forecast WHERE product_code = 'T014P005'");
        java.sql.Timestamp createdAt = (java.sql.Timestamp) row.get("created_at");
        java.sql.Timestamp updatedAt = (java.sql.Timestamp) row.get("updated_at");
        assertTrue(updatedAt.after(createdAt), "updated_at should be after created_at after update");
    }

    // 10. testIndexPerformance_MonthChannel
    @Test
    void testIndexPerformance_MonthChannel() {
        insertBulkTestRecords(100);

        List<Map<String, Object>> explain = jdbc.queryForList(
                "EXPLAIN SELECT * FROM sales_forecast WHERE month = '202601' AND channel = '大全聯'");
        assertFalse(explain.isEmpty());
        boolean usesIndex = explain.stream().anyMatch(row -> {
            Object keyCol = row.get("key");
            return keyCol != null && keyCol.toString().contains("idx_month_channel");
        });
        assertTrue(usesIndex, "Query on month+channel should use idx_month_channel");
    }

    // 11. testIndexPerformance_ProductCode
    @Test
    void testIndexPerformance_ProductCode() {
        insertBulkTestRecords(100);

        List<Map<String, Object>> explain = jdbc.queryForList(
                "EXPLAIN SELECT * FROM sales_forecast WHERE product_code = 'T014BULK001'");
        assertFalse(explain.isEmpty());
        boolean usesIndex = explain.stream().anyMatch(row -> {
            Object keyCol = row.get("key");
            return keyCol != null && keyCol.toString().contains("idx_product_code");
        });
        assertTrue(usesIndex, "Query on product_code should use idx_product_code");
    }

    // 12. testIndexPerformance_Version
    @Test
    void testIndexPerformance_Version() {
        insertBulkTestRecords(100);

        List<Map<String, Object>> explain = jdbc.queryForList(
                "EXPLAIN SELECT * FROM sales_forecast WHERE version = '2026/01/15 10:00:00(大全聯)'");
        assertFalse(explain.isEmpty());
        boolean usesIndex = explain.stream().anyMatch(row -> {
            Object keyCol = row.get("key");
            return keyCol != null && keyCol.toString().contains("idx_version");
        });
        assertTrue(usesIndex, "Query on version should use idx_version");
    }

    // 13. testCompositeIndexForDuplicateDetection
    @Test
    void testCompositeIndexForDuplicateDetection() {
        insertBulkTestRecords(100);

        List<Map<String, Object>> explain = jdbc.queryForList(
                "EXPLAIN SELECT * FROM sales_forecast WHERE month = '202601' AND channel = '大全聯' AND product_code = 'T014BULK001'");
        assertFalse(explain.isEmpty());
        boolean usesIndex = explain.stream().anyMatch(row -> {
            Object keyCol = row.get("key");
            return keyCol != null && (keyCol.toString().contains("idx_month_channel_product")
                    || keyCol.toString().contains("idx_month_channel"));
        });
        assertTrue(usesIndex, "Query on month+channel+product_code should use a composite index");
    }

    // 14. testMultipleVersionsSameProduct
    @Test
    void testMultipleVersionsSameProduct() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202601', '大全聯', 'T014P006', 100.00, '2026/01/10 10:00:00(大全聯)')");
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202601', '大全聯', 'T014P006', 120.00, '2026/01/15 10:00:00(大全聯)')");
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                "VALUES ('202601', '大全聯', 'T014P006', 130.00, '2026/01/20 10:00:00(大全聯)')");

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sales_forecast WHERE month = '202601' AND channel = '大全聯' AND product_code = 'T014P006'",
                Integer.class);
        assertEquals(3, count, "Multiple versions of the same product should all be stored");
    }

    // 15. testIsModifiedFlag
    @Test
    void testIsModifiedFlag() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, product_code, quantity, version, is_modified) " +
                "VALUES ('202601', '全聯', 'T014P007', 50.00, '2026/01/01 00:00:00(全聯)', FALSE)");

        Object before = jdbc.queryForObject(
                "SELECT is_modified FROM sales_forecast WHERE product_code = 'T014P007'", Object.class);
        assertFalse(Boolean.TRUE.equals(before) || Integer.valueOf(1).equals(before),
                "is_modified should be FALSE initially");

        jdbc.update("UPDATE sales_forecast SET is_modified = TRUE WHERE product_code = 'T014P007'");

        Object after = jdbc.queryForObject(
                "SELECT is_modified FROM sales_forecast WHERE product_code = 'T014P007'", Object.class);
        assertTrue(Boolean.TRUE.equals(after) || Integer.valueOf(1).equals(after),
                "is_modified should be TRUE after update");
    }

    // 16. testNullableColumns
    @Test
    void testNullableColumns() {
        jdbc.update(
                "INSERT INTO sales_forecast (month, channel, category, spec, product_code, product_name, warehouse_location, quantity, version) " +
                "VALUES ('202601', '好市多', NULL, NULL, 'T014P008', NULL, NULL, 10.00, '2026/01/01 00:00:00(好市多)')");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT category, spec, product_name, warehouse_location FROM sales_forecast WHERE product_code = 'T014P008'");

        assertNull(row.get("category"), "category should be nullable");
        assertNull(row.get("spec"), "spec should be nullable");
        assertNull(row.get("product_name"), "product_name should be nullable");
        assertNull(row.get("warehouse_location"), "warehouse_location should be nullable");
    }

    // 17. testRequiredColumns
    @Test
    void testRequiredColumns() {
        // Without month
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO sales_forecast (channel, product_code, quantity, version) VALUES ('家樂福', 'T014PX', 1.00, 'v1')"),
                "Insert without month should throw");

        // Without channel
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO sales_forecast (month, product_code, quantity, version) VALUES ('202601', 'T014PX', 1.00, 'v1')"),
                "Insert without channel should throw");

        // Without product_code
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO sales_forecast (month, channel, quantity, version) VALUES ('202601', '家樂福', 1.00, 'v1')"),
                "Insert without product_code should throw");

        // Without quantity
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, version) VALUES ('202601', '家樂福', 'T014PX', 'v1')"),
                "Insert without quantity should throw");

        // Without version
        assertThrows(Exception.class, () ->
                jdbc.update("INSERT INTO sales_forecast (month, channel, product_code, quantity) VALUES ('202601', '家樂福', 'T014PX', 1.00)"),
                "Insert without version should throw");
    }

    // Helper: insert bulk records for index performance tests
    private void insertBulkTestRecords(int count) {
        for (int i = 1; i <= count; i++) {
            String paddedI = String.format("%03d", i);
            jdbc.update(
                    "INSERT INTO sales_forecast (month, channel, product_code, quantity, version) " +
                    "VALUES ('202601', '大全聯', 'T014BULK" + paddedI + "', " + (i * 1.5) + ", '2026/01/15 10:00:00(大全聯)')");
        }
    }
}
