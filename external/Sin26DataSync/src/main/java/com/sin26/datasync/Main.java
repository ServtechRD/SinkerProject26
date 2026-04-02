package com.sin26.datasync;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 程式入口：啟動時先執行一次同步，之後每小時執行一次。
 */
public class Main {

    public static void main(String[] args) {
        Config config = new Config();
        SyncService sync = new SyncService(config);

        // 啟動時先跑一次
        sync.runOnce();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Sin26DataSync-Hourly");
            t.setDaemon(false);
            return t;
        });

        scheduler.scheduleAtFixedRate(
            sync::runOnce,
            1,
            1,
            TimeUnit.HOURS
        );

        System.out.println("每小時同步已排程，按 Ctrl+C 結束程式。");
    }
}
