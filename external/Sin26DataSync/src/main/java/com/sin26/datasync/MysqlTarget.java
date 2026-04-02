package com.sin26.datasync;

import java.sql.*;
import java.util.List;

/**
 * 寫入 MySQL product 表；以 code 為唯一鍵，已存在時更新。
 */
public class MysqlTarget implements AutoCloseable {
    private final Config config;
    private Connection conn;

    public MysqlTarget(Config config) {
        this.config = config;
    }

    public void connect() throws SQLException {
        conn = DriverManager.getConnection(
            config.getMysqlUrl(),
            config.getMysqlUser(),
            config.getMysqlPassword()
        );
    }

    public void ensureTable() throws SQLException {
        String ddl = """
            CREATE TABLE IF NOT EXISTS product (
                code VARCHAR(100) NOT NULL,
                name VARCHAR(500),
                category_name VARCHAR(100),
                spec VARCHAR(500),
                warehouse_location VARCHAR(100),
                PRIMARY KEY (code)
            )
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(ddl);
        }
    }

    /**
     * 批次寫入；若 code 已存在則更新，否則插入。
     */
    public int upsert(List<Prdt> batch) throws SQLException {
        if (batch.isEmpty()) return 0;
        String sql = """
            INSERT INTO product (code, name, category_name, spec, warehouse_location)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                category_name = VALUES(category_name),
                spec = VALUES(spec),
                warehouse_location = VALUES(warehouse_location)
            """;
        int count = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Prdt p : batch) {
                ps.setString(1, p.getPrdNo());
                ps.setString(2, p.getName());
                ps.setString(3, p.getIdx1());
                ps.setString(4, p.getSpc());
                ps.setString(5, p.getWh());
                count += ps.executeUpdate();
            }
        }
        return count;
    }

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // ignore
            }
            conn = null;
        }
    }
}
