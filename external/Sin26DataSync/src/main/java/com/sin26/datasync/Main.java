package com.sin26.datasync;

/**
 * 程式入口：執行單次同步後結束。
 * 定期執行請用工作排程器、cron、容器排程等外部方式啟動本程式。
 */
public class Main {

    public static void main(String[] args) {
        Config config = new Config();
        SyncService sync = new SyncService(config);
        sync.runOnce();
    }
}
