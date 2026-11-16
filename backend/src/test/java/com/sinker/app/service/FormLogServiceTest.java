package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.entity.User;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FormLogServiceTest {

    // 2026-12-10 08:00 台灣時間
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));

    @Mock private SalesForecastFormLogRepository repository;
    @Mock private UserRepository userRepository;

    private FormLogService service;

    @BeforeEach
    void setUp() {
        service = new FormLogService(repository, userRepository, CLOCK);
    }

    @Test
    void record_savesWithTaipeiTimeAndTruncatesDetail() {
        service.record("202612", "家樂福", FormLogAction.REOPEN, 5L, "x".repeat(600));

        ArgumentCaptor<SalesForecastFormLog> captor = ArgumentCaptor.forClass(SalesForecastFormLog.class);
        verify(repository).save(captor.capture());
        SalesForecastFormLog saved = captor.getValue();
        assertEquals("202612", saved.getMonth());
        assertEquals("家樂福", saved.getChannel());
        assertEquals(FormLogAction.REOPEN, saved.getAction());
        assertEquals(5L, saved.getOperatorId());
        assertEquals(500, saved.getDetail().length());
        assertEquals(LocalDateTime.of(2026, 12, 10, 8, 0), saved.getCreatedAt());
    }

    @Test
    void list_mapsOperatorNamesAndSystem() {
        SalesForecastFormLog byUser = log(1, FormLogAction.REOPEN, 5L);
        SalesForecastFormLog bySystem = log(2, FormLogAction.AUTO_CLOSE, null);
        when(repository.findByMonthOrderByCreatedAtDescIdDesc("202612")).thenReturn(List.of(byUser, bySystem));
        User u = new User();
        u.setId(5L);
        u.setUsername("pm");
        u.setFullName("陳美玲");
        when(userRepository.findAllById(Set.of(5L))).thenReturn(List.of(u));

        List<FormLogDTO> result = service.list("202612");

        assertEquals(2, result.size());
        assertEquals("陳美玲", result.get(0).operatorName());
        assertEquals("REOPEN", result.get(0).action());
        assertEquals("系統", result.get(1).operatorName());
    }

    private static SalesForecastFormLog log(int id, FormLogAction action, Long operatorId) {
        SalesForecastFormLog l = new SalesForecastFormLog();
        l.setId(id);
        l.setMonth("202612");
        l.setAction(action);
        l.setOperatorId(operatorId);
        l.setCreatedAt(LocalDateTime.of(2026, 12, 10, 8, 0));
        return l;
    }
}
