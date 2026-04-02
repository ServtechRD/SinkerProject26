package com.sin26.datasync;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 從 config.properties 讀取設定；若無則使用預設值。
 */
public class Config {
    private static final String CONFIG_FILE = "config.properties";

    private String mssqlUrl;
    private String mssqlUser;
    private String mssqlPassword;
    private String mysqlUrl;
    private String mysqlUser;
    private String mysqlPassword;
    private int batchSize;
    private boolean testMode;
    private String csvOutputPath;
    private String cursorPath;

    public Config() {
        load();
    }

    private void load() {
        Properties p = new Properties();
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("讀取 config.properties 失敗，使用預設值: " + e.getMessage());
            }
        }

        String baseUrl = p.getProperty("mssql.url", "jdbc:sqlserver://localhost:1433;databaseName=YourDB");
        boolean trustCert = Boolean.parseBoolean(p.getProperty("mssql.trustServerCertificate", "true"));
        // 移除 URL 中既有的 trustServerCertificate，再依設定附加
        baseUrl = baseUrl.replaceAll(";trustServerCertificate=(true|false)", "").replaceFirst("trustServerCertificate=(true|false);?", "");
        baseUrl = baseUrl.replaceAll(";+", ";");
        if (baseUrl.endsWith(";")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        mssqlUrl = baseUrl + ";trustServerCertificate=" + trustCert;
        mssqlUser = p.getProperty("mssql.user", "sa");
        mssqlPassword = p.getProperty("mssql.password", "");
        mysqlUrl = p.getProperty("mysql.url", "jdbc:mysql://localhost:3306/YourDB?useUnicode=true&characterEncoding=UTF-8");
        mysqlUser = p.getProperty("mysql.user", "root");
        mysqlPassword = p.getProperty("mysql.password", "");
        batchSize = Integer.parseInt(p.getProperty("batch.size", "100"));
        testMode = Boolean.parseBoolean(p.getProperty("test.mode", "false"));
        csvOutputPath = p.getProperty("csv.output", "prdt_export.csv");
        cursorPath = p.getProperty("cursor.path", "sync_cursor.txt");
    }

    public String getMssqlUrl() { return mssqlUrl; }
    public String getMssqlUser() { return mssqlUser; }
    public String getMssqlPassword() { return mssqlPassword; }
    public String getMysqlUrl() { return mysqlUrl; }
    public String getMysqlUser() { return mysqlUser; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getBatchSize() { return batchSize; }
    public boolean isTestMode() { return testMode; }
    public String getCsvOutputPath() { return csvOutputPath; }
    public String getCursorPath() { return cursorPath; }
}
