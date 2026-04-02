package com.sin26.datasync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 將上次處理到的 IDX1 寫入檔案，下次只抓 IDX1 大於此值的資料（已處理的跳過）。
 */
public class CursorStore {
    private final Path path;

    public CursorStore(String cursorPath) {
        this.path = Paths.get(cursorPath);
    }

    /** 游標為空或檔案不存在時，表示從最前面開始（對應 SQL 中與空字串比較）。 */
    public String getLastCursor() {
        if (!Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return "";
        }
    }

    public void saveCursor(String idx1) {
        try {
            Files.writeString(path, idx1 == null ? "" : idx1, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("寫入游標失敗: " + path, e);
        }
    }
}
