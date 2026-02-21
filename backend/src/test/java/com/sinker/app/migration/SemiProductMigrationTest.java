package com.sinker.app.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SemiProductMigrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void semiProductAdvancePurchaseTableExists() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semi_product_advance_purchase'",
                Integer.class);
        assertEquals(1, count, "semi_product_advance_purchase table should exist");
    }

    @Test
    void semiProductAdvancePurchaseTableHasAllColumns() {
        List<String> columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semi_product_advance_purchase' ORDER BY ORDINAL_POSITION",
                String.class);
        assertEquals(6, columns.size(), "semi_product_advance_purchase table should have 6 columns");
        assertTrue(columns.containsAll(List.of(
                "id", "product_code", "product_name", "advance_days", "created_at", "updated_at")));
    }

    @Test
    void semiProductProductCodeIsVarchar50() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semi_product_advance_purchase' AND COLUMN_NAME = 'product_code'",
                String.class);
        assertEquals("varchar(50)", columnType);
    }

    @Test
    void semiProductProductNameIsVarchar200() {
        String columnType = jdbc.queryForObject(
                "SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semi_product_advance_purchase' AND COLUMN_NAME = 'product_name'",
                String.class);
        assertEquals("varchar(200)", columnType);
    }

    @Test
    void semiProductAdvanceDaysIsInt() {
        String dataType = jdbc.queryForObject(
                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'semi_product_advance_purchase' AND COLUMN_NAME = 'advance_days'",
                String.class);
        assertEquals("int", dataType);
    }

    @Test
    void semiProductUniqueConstraintOnProductCode() {
        jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('TEST001', 'Test Product', 7)");
        try {
            assertThrows(org.springframework.dao.DuplicateKeyException.class, () ->
                    jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('TEST001', 'Another Product', 5)"));
        } finally {
            jdbc.update("DELETE FROM semi_product_advance_purchase WHERE product_code = 'TEST001'");
        }
    }

    @Test
    void semiProductInsertAndQuery() {
        jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('SP001', '半成品A', 7)");
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM semi_product_advance_purchase WHERE product_code = 'SP001'",
                    Integer.class);
            assertEquals(1, count);

            String productName = jdbc.queryForObject(
                    "SELECT product_name FROM semi_product_advance_purchase WHERE product_code = 'SP001'",
                    String.class);
            assertEquals("半成品A", productName);

            Integer advanceDays = jdbc.queryForObject(
                    "SELECT advance_days FROM semi_product_advance_purchase WHERE product_code = 'SP001'",
                    Integer.class);
            assertEquals(7, advanceDays);
        } finally {
            jdbc.update("DELETE FROM semi_product_advance_purchase WHERE product_code = 'SP001'");
        }
    }

    @Test
    void semiProductTimestampsAutoPopulated() {
        jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('SP002', '半成品B', 10)");
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM semi_product_advance_purchase WHERE product_code = 'SP002' AND created_at IS NOT NULL AND updated_at IS NOT NULL",
                    Integer.class);
            assertEquals(1, count, "Timestamps should be auto-populated");
        } finally {
            jdbc.update("DELETE FROM semi_product_advance_purchase WHERE product_code = 'SP002'");
        }
    }

    @Test
    void semiProductIdIsAutoIncrement() {
        jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('SP003', '半成品C', 5)");
        jdbc.update("INSERT INTO semi_product_advance_purchase (product_code, product_name, advance_days) VALUES ('SP004', '半成品D', 6)");
        try {
            List<Integer> ids = jdbc.queryForList(
                    "SELECT id FROM semi_product_advance_purchase WHERE product_code IN ('SP003', 'SP004') ORDER BY id",
                    Integer.class);
            assertEquals(2, ids.size());
            assertTrue(ids.get(1) > ids.get(0), "IDs should auto-increment");
        } finally {
            jdbc.update("DELETE FROM semi_product_advance_purchase WHERE product_code IN ('SP003', 'SP004')");
        }
    }
}
