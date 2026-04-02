package com.sin26.datasync;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 從 CSV 讀取 Prdt（測試模式用，不連 MSSQL 時可選用）。
 */
public class CsvInput {
    private final Path path;

    public CsvInput(String filename) {
        this.path = Path.of(filename == null || filename.isEmpty() ? "prdt_sample.csv" : filename);
    }

    public List<Prdt> readAll() throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        List<Prdt> list = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || (i == 0 && line.toUpperCase().startsWith("PRD_NO"))) continue;
            String[] parts = parseCsvLine(line);
            if (parts.length >= 5) {
                Prdt p = new Prdt();
                p.setPrdNo(parts[0]);
                p.setName(parts[1]);
                p.setSpc(parts[2]);
                p.setIdx1(parts[3].isEmpty() ? null : parts[3]);
                p.setWh(parts[4]);
                list.add(p);
            }
        }
        return list;
    }

    public List<Prdt> readBatch(int maxRows) throws IOException {
        List<Prdt> all = readAll();
        return all.size() <= maxRows ? all : all.subList(0, maxRows);
    }

    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(cell.toString().trim());
                cell.setLength(0);
            } else {
                cell.append(c);
            }
        }
        result.add(cell.toString().trim());
        return result.toArray(new String[0]);
    }
}
