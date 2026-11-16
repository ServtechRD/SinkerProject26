package com.sinker.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    /** 截止、補登、異動紀錄一律以台灣時間判斷，不受容器時區影響；測試可替換為固定時鐘 */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Taipei"));
    }
}
