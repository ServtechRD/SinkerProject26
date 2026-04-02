package com.sin26.datasync;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 從 MSSQL 查詢 prdt，依 IDX1 游標分批取回，已處理過的（IDX1 <= lastCursor）跳過。
 */
public class MssqlSource implements AutoCloseable {
    private final Config config;
    private Connection conn;

    public MssqlSource(Config config) {
        this.config = config;
    }

    public void connect() throws SQLException {
        conn = DriverManager.getConnection(
            config.getMssqlUrl(),
            config.getMssqlUser(),
            config.getMssqlPassword()
        );
    }

    /**
     * 抓下一批：IDX1 > lastCursor（字串比較），最多 batchSize 筆。
     * lastCursor 為空或 null 時從頭開始抓。
     */
    public List<Prdt> fetchNextBatch(String lastCursor, int batchSize) throws SQLException {
        String sql = "SELECT PRD_NO, NAME, SPC, IDX1, WH FROM prdt WHERE "
            + "(CASE WHEN ? = '' THEN 1 ELSE CASE WHEN CAST(IDX1 AS VARCHAR(100)) > ? THEN 1 ELSE 0 END END) = 1 "
            + "ORDER BY CAST(IDX1 AS VARCHAR(100)) OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
        List<Prdt> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String lc = lastCursor == null ? "" : lastCursor;
            ps.setString(1, lc);
            ps.setString(2, lc);
            ps.setInt(3, batchSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Prdt p = new Prdt();
                    p.setPrdNo(rs.getString("PRD_NO"));
                    p.setName(rs.getString("NAME"));
                    p.setSpc(rs.getString("SPC"));
                    p.setIdx1(rs.getObject("IDX1") != null ? rs.getString("IDX1") : null);
                    p.setWh(rs.getString("WH"));
                    list.add(p);
                }
            }
        }
        return list;
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
