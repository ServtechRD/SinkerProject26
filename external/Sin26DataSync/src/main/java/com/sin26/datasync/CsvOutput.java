package com.sin26.datasync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 測試模式：將本批資料輸出為 CSV（追加寫入同一檔案）。
 */
public class CsvOutput {
    private static final String CSV_HEADER = "PRD_NO,NAME,SPC,IDX1,WH";
    private final Path path;

    public CsvOutput(String filename) {
        this.path = Path.of(filename == null || filename.isEmpty() ? "prdt_export.csv" : filename);
    }

    public void append(List<Prdt> batch) throws IOException {
        boolean writeHeader = !Files.exists(path);
        StringBuilder sb = new StringBuilder();
        if (writeHeader) {
            sb.append(CSV_HEADER).append("\n");
        }
        for (Prdt p : batch) {
            sb.append(escape(p.getPrdNo())).append(",")
              .append(escape(p.getName())).append(",")
              .append(escape(p.getSpc())).append(",")
              .append(p.getIdx1() != null ? p.getIdx1() : "").append(",")
              .append(escape(p.getWh())).append("\n");
        }
        Files.writeString(
            path,
            sb.toString(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
