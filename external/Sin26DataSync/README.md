# Sin26DataSync

Windows 上用 Java 執行的資料同步程式：從 MSSQL Server 讀取 `prdt` 資料，每小時執行一次，已存在的跳過，每批最多 100 或 1000 筆（可設定），寫入 MySQL；測試模式時改為輸出 CSV。

## 需求

- JDK 17+
- Gradle（或使用專案內建 wrapper）

## 設定

1. 複製 `config.properties.example` 為 `config.properties`
2. 編輯 `config.properties`：
   - **mssql.***：MSSQL Server 連線
   - **mysql.***：MySQL 連線
   - **batch.size**：每批筆數（例如 100 或 1000）
   - **test.mode**：`true` 時只輸出 CSV，不寫入 MySQL
   - **csv.output**：測試模式時的 CSV 檔名（預設 `prdt_export.csv`）
   - **cursor.path**：游標檔路徑（記錄上次處理到的 IDX1，預設 `sync_cursor.txt`）

## 執行

若尚未有 `gradlew`，請先在本機執行一次 `gradle wrapper`（需已安裝 Gradle）。

```bash
# 編譯並執行（需先有 config.properties）
gradlew run
```

或先建置再執行：

```bash
gradlew build
java -cp "build/classes/java/main;build/resources/main;依賴路徑" com.sin26.datasync.Main
```

建議用 `gradlew run` 即可。

## 行為說明

- **MSSQL 查詢**：以字串比較 `IDX1 > 游標`（`CAST(IDX1 AS VARCHAR(100))`），游標為空時從頭抓。
- **已存在就跳過**：  
  - 來源端：用游標檔記錄上次處理到的 `IDX1`，下次只抓 `IDX1 > 游標`。  
  - 目標端：MySQL 表主鍵為 `(PRD_NO, WH)`，使用 `ON DUPLICATE KEY UPDATE`，已存在則更新不重複插入。
- **每小時**：啟動時先執行一次同步，之後每 1 小時執行一次。
- **測試模式**：`test.mode=true` 時不連 MySQL，只把每批資料追加寫入 CSV（檔名由 `csv.output` 設定）。

## MySQL 表結構

程式會自動建立（若不存在）：

```sql
CREATE TABLE IF NOT EXISTS prdt (
    PRD_NO VARCHAR(100) NOT NULL,
    NAME VARCHAR(500),
    SPC VARCHAR(500),
    IDX1 VARCHAR(100),
    WH VARCHAR(100) NOT NULL,
    PRIMARY KEY (PRD_NO, WH)
);
```

## 專案結構（Gradle）

- `build.gradle`：Java 17、MSSQL JDBC、MySQL Connector/J、`application` 主類 `com.sin26.datasync.Main`
- 設定範例：`config.properties.example`
- 來源碼：`src/main/java/com/sin26/datasync/`
