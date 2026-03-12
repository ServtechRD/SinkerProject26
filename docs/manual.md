# SinkerProject26 操作手冊

## 📋 目錄

1. [系統概述](#系統概述)
2. [系統需求](#系統需求)
3. [快速啟動](#快速啟動)
4. [日常操作](#日常操作)
5. [資料庫管理](#資料庫管理)
6. [測試與覆蓋率](#測試與覆蓋率)
7. [故障排除](#故障排除)
8. [常用命令參考](#常用命令參考)

---

## 系統概述

SinkerProject26 是一個全端 Web 應用程式，採用現代化的技術棧：

### 技術架構

| 元件 | 技術 | 說明 |
|------|------|------|
| **後端** | Spring Boot 3.x + Java 17 | RESTful API 服務 |
| **前端** | React + Vite | 現代化單頁應用 |
| **資料庫** | MariaDB 10 | 關聯式資料庫 |
| **容器化** | Docker + Docker Compose | 開發與部署環境 |
| **資料庫遷移** | Flyway | 版本控制與遷移 |

### 服務說明

| 服務名稱 | 說明 | 端口 | 健康檢查 |
|---------|------|------|----------|
| `db` | MariaDB 資料庫 | 3306 | mariadb-admin ping |
| `backend` | Spring Boot API 伺服器 | 8080 | /actuator/health |
| `frontend` | React 前端應用 | 5173 | HTTP 200 |

### 存取位址

- **前端應用**: http://localhost:5173
- **後端 API**: http://localhost:8080/api
- **API 文檔 (Swagger)**: http://localhost:5173/swagger-ui/index.html
- **健康檢查**: http://localhost:8080/api/health

---

## 系統需求

### 必要軟體

- **Docker Engine**: 20.10 或更高版本
- **Docker Compose**: 2.0 或更高版本
- **Make**: Linux/macOS 系統預設安裝

### 檢查安裝

```bash
# 檢查 Docker 版本
docker --version

# 檢查 Docker Compose 版本
docker compose version

# 檢查 Make 版本
make --version
```

---

## 快速啟動

### 首次啟動

1. **進入專案目錄**
   ```bash
   cd /path/to/SinkerProject26
   ```

2. **啟動所有服務**
   ```bash
   make dev-up
   ```

   此命令會：
   - 建置 Docker 映像檔
   - 啟動資料庫服務並等待就緒
   - 自動執行 Flyway 資料庫遷移
   - 啟動後端和前端服務

3. **驗證服務狀態**
   ```bash
   docker compose ps
   ```

   預期輸出：
   ```
   NAME                          STATUS              PORTS
   sinkerproject26-backend-1     Up                  0.0.0.0:8080->8080/tcp
   sinkerproject26-db-1          Up (healthy)        0.0.0.0:3306->3306/tcp
   sinkerproject26-frontend-1    Up                  0.0.0.0:5173->80/tcp
   ```

4. **存取應用程式**
   - 開啟瀏覽器，訪問 http://localhost:5173

---

## 日常操作

### 啟動服務

```bash
# 啟動所有服務（包含建置）
make dev-up

# 僅啟動服務（不重新建置）
docker compose start
```

### 停止服務

```bash
# 停止服務（保留資料）
docker compose stop

# 停止並移除服務（保留資料卷）
docker compose down

# 完全移除（⚠️ 會刪除資料庫資料）
make dev-down
```

### 重新建置服務

```bash
# 重新建置所有服務
make dev-down
make dev-up

# 僅重新建置後端與前端（保留資料庫資料）
make dev-build

# 僅重新建置後端
make dev-build-backend

# 僅重新建置前端
make dev-build-frontend

# 強制無快取重新建置特定服務
docker compose build --no-cache backend
docker compose up -d backend
```

### 查看日誌

```bash
# 查看所有服務日誌（實時追蹤）
docker compose logs -f

# 查看特定服務日誌
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db

# 查看最近 100 行日誌
docker compose logs -f --tail=100 backend

# 查看啟動日誌（最近 200 行）
docker compose logs -f --tail=200
```

### 進入容器執行命令

```bash
# 進入後端容器
docker compose exec backend bash

# 進入資料庫容器
docker compose exec db bash

# 進入前端容器
docker compose exec frontend sh

# 在後端執行 Gradle 命令
docker compose exec backend ./gradlew tasks
```

---

## 資料庫管理

### 檢視資料庫資訊

```bash
# 查看遷移歷史、當前版本和資料表
make db-info
```

### 開啟 MariaDB 命令列

```bash
# 進入 MariaDB CLI
make db-shell

# 或直接使用 docker compose
docker compose exec db mariadb -uapp -papp app
```

常用 SQL 命令：
```sql
-- 查看所有資料表
SHOW TABLES;

-- 查看資料表結構
DESCRIBE table_name;

-- 查詢資料
SELECT * FROM table_name LIMIT 10;

-- 查看 Flyway 遷移歷史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 離開
quit
```

### 執行資料庫遷移

```bash
# 重啟後端以執行待處理的遷移
make db-migrate
```

### 資料庫連線參數

- **主機**: localhost
- **端口**: 3306
- **資料庫名稱**: app
- **使用者名稱**: app
- **密碼**: app
- **Root 密碼**: root

### 備份與還原

```bash
# 備份資料庫
docker compose exec -T db mariadb-dump -uapp -papp app > backup_$(date +%Y%m%d_%H%M%S).sql

# 還原資料庫
docker compose exec -T db mariadb -uapp -papp app < backup.sql
```

### Flyway 遷移檔案

- **位置**: `backend/src/main/resources/db/migration/`
- **命名規則**: `V{版本號}__{描述}.sql`
- **範例**: `V1__initial_schema.sql`

更多資料庫操作詳情請參閱 [DB_MIGRATION.md](./DB_MIGRATION.md)

---

## 測試與覆蓋率

### 執行完整測試

```bash
# 執行端到端測試（包含後端單元測試和前端測試）
make test-compose
```

此命令會：
1. 啟動獨立的測試環境（不影響開發環境）
2. 執行後端單元測試
3. 執行前端端到端測試
4. 清理測試環境

### 停止測試環境

```bash
make test-down
```

### 產生覆蓋率報告

```bash
# 產生後端覆蓋率報告（JaCoCo）
make coverage-backend

# 產生前端覆蓋率報告（Vitest）
make coverage-frontend

# 產生完整覆蓋率報告（前端 + 後端）
make coverage
```

### 查看覆蓋率報告

產生後的報告位置：

- **後端報告**: `coverage-reports/backend/index.html`
- **前端報告**: `coverage-reports/frontend/index.html`

使用瀏覽器開啟 HTML 檔案查看詳細報告。

更多測試覆蓋率資訊請參閱 [COVERAGE.md](./COVERAGE.md)

---

## 故障排除

### 問題 1：端口已被佔用

**症狀**:
```
Error: driver failed programming external connectivity
bind: address already in use
```

**解決方案**:
1. 檢查哪個程式佔用端口：
   ```bash
   sudo lsof -i :8080   # 或 :3306, :5173
   ```
2. 停止衝突的程序，或修改 `docker-compose.yml` 中的端口配置

### 問題 2：資料庫未就緒

**症狀**:
- 後端無法啟動
- 持續顯示「Waiting for db to be healthy」

**解決方案**:
```bash
# 1. 檢查資料庫日誌
docker compose logs db

# 2. 驗證資料庫健康狀態
docker compose exec db mariadb-admin ping -h localhost -uapp -papp

# 3. 重啟資料庫服務
docker compose restart db

# 4. 如果仍無法解決，重建資料庫
docker compose stop db
docker compose rm -f db
docker volume rm sinkerproject26_db-data
make dev-up
```

### 問題 3：Flyway 遷移失敗

**症狀**:
- 後端啟動時出現 Flyway 錯誤
- 日誌中顯示遷移失敗訊息

**解決方案**:
```bash
# 1. 查看遷移歷史
make db-info

# 2. 檢查失敗的遷移腳本
docker compose logs backend | grep -i flyway

# 3. 手動修復（進入資料庫）
make db-shell
# 然後執行修復 SQL

# 4. 清空遷移記錄並重新開始（⚠️ 慎用）
# DELETE FROM flyway_schema_history WHERE success = 0;
```

更多遷移問題請參閱 [DB_MIGRATION.md](./DB_MIGRATION.md)

### 問題 4：後端建置失敗

**症狀**:
- 後端容器立即退出
- 日誌中顯示建置錯誤

**解決方案**:
```bash
# 1. 檢查後端日誌
docker compose logs backend

# 2. 無快取重新建置
docker compose build --no-cache backend
make dev-up

# 3. 檢查 Java 版本和依賴
docker compose exec backend java -version
docker compose exec backend ./gradlew dependencies
```

### 問題 5：前端無法載入

**症狀**:
- 瀏覽器顯示「502 Bad Gateway」
- 前端頁面無法開啟

**解決方案**:
```bash
# 1. 檢查前端日誌
docker compose logs frontend

# 2. 重新建置前端
docker compose build --no-cache frontend
docker compose up -d frontend

# 3. 檢查 Nginx 配置
docker compose exec frontend cat /etc/nginx/conf.d/default.conf
```

### 問題 6：無法連接 Docker 守護程序

**症狀**:
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock
```

**解決方案**:
```bash
# 1. 確保 Docker 守護程序正在執行
sudo systemctl start docker
sudo systemctl enable docker

# 2. 將使用者加入 docker 群組
sudo usermod -aG docker $USER
newgrp docker

# 3. 驗證 Docker 狀態
docker ps
```

### 問題 7：磁碟空間不足

**症狀**:
- 建置失敗
- 容器無法啟動

**解決方案**:
```bash
# 檢查 Docker 使用的磁碟空間
docker system df

# 清理未使用的映像檔、容器和卷
docker system prune -a --volumes

# 僅清理未使用的容器和網路
docker system prune
```

### 問題 8：服務健康檢查失敗

**症狀**:
- 服務顯示為 unhealthy
- 無法存取應用程式

**解決方案**:
```bash
# 檢查後端健康狀態
curl http://localhost:8080/api/health

# 檢查前端健康狀態
curl -I http://localhost:5173

# 檢查資料庫健康狀態
docker compose exec db mariadb-admin ping -h localhost -uapp -papp --silent
echo $?  # 應該輸出 0
```

---

## 常用命令參考

### 開發命令

```bash
# 啟動所有服務
make dev-up

# 重新建置後端和前端（保留資料庫）
make dev-build

# 僅重新建置後端
make dev-build-backend

# 僅重新建置前端
make dev-build-frontend

# 停止並移除所有服務（⚠️ 刪除資料）
make dev-down
```

### 測試命令

```bash
# 執行完整測試
make test-compose

# 停止測試環境
make test-down

# 產生後端覆蓋率報告
make coverage-backend

# 產生前端覆蓋率報告
make coverage-frontend

# 產生完整覆蓋率報告
make coverage
```

### 資料庫命令

```bash
# 查看資料庫資訊
make db-info

# 開啟 MariaDB CLI
make db-shell

# 執行資料庫遷移
make db-migrate
```

### 測試資料命令

```bash
# 產生測試 Excel 檔案
make gen-test-excels
```

### Docker Compose 命令

```bash
# 查看服務狀態
docker compose ps

# 查看日誌
docker compose logs -f [service_name]

# 重啟服務
docker compose restart [service_name]

# 停止服務
docker compose stop [service_name]

# 啟動服務
docker compose start [service_name]

# 進入容器
docker compose exec [service_name] bash
```

### 幫助命令

```bash
# 顯示所有可用命令
make help
```

---

## 環境變數配置

### 資料庫環境變數

預設配置在 `docker-compose.yml` 中：

```yaml
MARIADB_DATABASE=app
MARIADB_USER=app
MARIADB_PASSWORD=app
MARIADB_ROOT_PASSWORD=root
```

### 後端環境變數

```yaml
SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/app
SPRING_DATASOURCE_USERNAME=app
SPRING_DATASOURCE_PASSWORD=app
SPRING_PROFILES_ACTIVE=docker
```

### 自訂環境變數

可在專案根目錄建立 `.env` 檔案覆蓋預設值：

```bash
# .env 範例
MARIADB_PASSWORD=custom_password
BACKEND_PORT=8081
FRONTEND_PORT=3000
```

---

## 網路架構

所有服務運行在 `appnet` 橋接網路上，服務間可使用服務名稱進行通訊：

- 後端連接資料庫：`jdbc:mariadb://db:3306/app`
- 前端代理 API 請求到後端：`http://backend:8080`

---

## 資料持久化

- **資料庫資料**：儲存在 Docker 卷中
  - 使用 `docker compose stop` 保留資料
  - 使用 `make dev-down` 會刪除所有資料（包含卷）

- **程式碼**：掛載到容器中，即時更新
  - 後端：需要重新建置才能生效
  - 前端：Vite 支援熱重載（開發模式）

---

## 效能優化建議

### 開發環境優化

1. **增加 Docker 資源**：
   - CPU：至少 2 核心
   - 記憶體：至少 4GB
   - 磁碟空間：至少 20GB

2. **使用本地快取**：
   - Gradle 依賴快取：`~/.gradle`
   - npm 依賴快取：`~/.npm`

3. **避免不必要的重建**：
   - 使用 `docker compose start/stop` 而非 `up/down`
   - 僅重建變更的服務

### 生產環境注意事項

本專案目前配置為開發環境，生產部署需要：

1. 修改資料庫密碼
2. 啟用 HTTPS
3. 配置反向代理（如 Nginx）
4. 設定環境變數安全管理
5. 啟用日誌收集與監控

---

## 相關文檔

- **[啟動指南 (英文)](./STARTUP.md)** - 詳細的啟動說明
- **[資料庫遷移指南 (英文)](./DB_MIGRATION.md)** - Flyway 遷移操作
- **[測試覆蓋率指南 (英文)](./COVERAGE.md)** - 測試覆蓋率報告生成
- **[Excel 上傳規格 (英文)](./upload_excel_spec.md)** - Excel 檔案上傳格式

---

## 技術支援

- **GitHub Issues**: https://github.com/ServtechRD/SinkerProject26/issues
- **專案根目錄**: `/home/devuser/work/servtech/sinker26/code/SinkerProject26`

---

## 版本資訊

- **專案版本**: 2026
- **Java 版本**: 17
- **Spring Boot 版本**: 3.x
- **Node.js 版本**: 18+
- **MariaDB 版本**: 10
- **Docker Compose 版本**: 2.0+

---

## 常見工作流程

### 工作流程 1：開始新的一天

```bash
# 1. 進入專案目錄
cd /path/to/SinkerProject26

# 2. 拉取最新程式碼
git pull

# 3. 啟動服務
make dev-up

# 4. 查看服務狀態
docker compose ps

# 5. 開始開發
# 開啟瀏覽器訪問 http://localhost:5173
```

### 工作流程 2：更新後端程式碼

```bash
# 1. 修改程式碼
vim backend/src/main/java/...

# 2. 重新建置後端
make dev-build-backend

# 3. 查看日誌確認
docker compose logs -f backend

# 4. 測試 API
curl http://localhost:8080/api/...
```

### 工作流程 3：更新前端程式碼

```bash
# 1. 修改程式碼
vim frontend/src/...

# 2. 重新建置前端（如需要）
make dev-build-frontend

# 3. 查看日誌
docker compose logs -f frontend

# 4. 重新整理瀏覽器測試
```

### 工作流程 4：新增資料庫遷移

```bash
# 1. 建立遷移檔案
vim backend/src/main/resources/db/migration/V2__add_new_table.sql

# 2. 執行遷移
make db-migrate

# 3. 驗證遷移
make db-info

# 4. 測試新功能
```

### 工作流程 5：結束一天的工作

```bash
# 1. 提交程式碼
git add .
git commit -m "描述變更"
git push

# 2. 停止服務（保留資料）
docker compose stop

# 或完全清理（不保留資料）
# make dev-down
```

---

**最後更新**: 2026-03-12
**維護者**: Team
