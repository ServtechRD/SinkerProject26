package com.sin26.datasync;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 單次同步：從 MSSQL 抓一批（已處理的依游標跳過）。
 * 測試模式：連 MSSQL，只輸出 CSV，不連 MySQL。
 * 正式模式：連 MSSQL + MySQL，寫入 MySQL。
 */
public class SyncService {
    private final Config config;
    private final CursorStore cursorStore;

    public SyncService(Config config) {
        this.config = config;
        this.cursorStore = new CursorStore(config.getCursorPath());
    }

    public void runOnce() {
        int batchSize = config.getBatchSize();
        String lastCursor = cursorStore.getLastCursor();
        boolean testMode = config.isTestMode();

        System.out.println(LocalDateTime.now() + " 開始同步，游標(PRD_NO)=" + (lastCursor.isEmpty() ? "(空)" : lastCursor) + "，批次大小=" + batchSize + "，測試模式=" + testMode);

        int totalFetched = 0;
        int totalWritten = 0;
        int batchNo = 0;

        try (MssqlSource source = new MssqlSource(config)) {
            source.connect();

            MysqlTarget target = null;
            CsvOutput csv = null;

            if (testMode) {
                csv = new CsvOutput(config.getCsvOutputPath());
            } else {
                target = new MysqlTarget(config);
                target.connect();
                target.ensureTable();
            }

            try {
                while (true) {
                    List<Prdt> batch = source.fetchNextBatch(lastCursor, batchSize);
                    if (batch.isEmpty()) {
                        if (batchNo == 0) {
                            System.out.println("  無新資料，跳過。");
                        }
                        break;
                    }

                    batchNo++;
                    totalFetched += batch.size();
                    System.out.println("  第 " + batchNo + " 批，從 MSSQL 取得 " + batch.size() + " 筆");

                    if (testMode) {
                        csv.append(batch);
                        totalWritten += batch.size();
                    } else {
                        totalWritten += target.upsert(batch);
                    }

                    String nextCursor = batch.get(batch.size() - 1).getPrdNo();
                    if (nextCursor == null || nextCursor.isEmpty() || nextCursor.equals(lastCursor)) {
                        System.out.println("  偵測到游標(PRD_NO)未前進，停止以避免無限迴圈。");
                        break;
                    }
                    lastCursor = nextCursor;
                    cursorStore.saveCursor(lastCursor);
                }
            } finally {
                if (target != null) {
                    target.close();
                }
            }

            if (batchNo > 0) {
                if (testMode) {
                    System.out.println("  已寫入 CSV: " + config.getCsvOutputPath());
                }
                System.out.println("  本次完成，總抓取 " + totalFetched + " 筆，總處理 " + totalWritten + " 筆，游標(PRD_NO)=" + lastCursor);
            }
        } catch (SQLException e) {
            System.err.println("  同步失敗: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("  寫入 CSV 失敗: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
