package com.sinker.app.service;

import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastReopenWindow;
import com.sinker.app.repository.SalesForecastReopenWindowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForecastEditGuardTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-12-10T00:00:00Z"), ZoneId.of("Asia/Taipei"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Mock private SalesForecastReopenWindowRepository windowRepository;

    private ForecastEditGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ForecastEditGuard(windowRepository, CLOCK);
    }

    private static SalesForecastConfig config(boolean closed) {
        SalesForecastConfig c = new SalesForecastConfig();
        c.setMonth("202612");
        c.setIsClosed(closed);
        return c;
    }

    @Test
    void open_isEditable() {
        assertDoesNotThrow(() -> guard.assertBusinessEditable(config(false), "家樂福"));
        assertEquals(ForecastEditGuard.Status.OPEN, guard.status(config(false), "家樂福"));
        verifyNoInteractions(windowRepository);
    }

    @Test
    void closedWithoutWindow_throws() {
        when(windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter("202612", "家樂福", NOW))
                .thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> guard.assertBusinessEditable(config(true), "家樂福"));
        assertEquals(ForecastEditGuard.Status.CLOSED, guard.status(config(true), "家樂福"));
    }

    @Test
    void closedWithActiveWindow_isEditable() {
        SalesForecastReopenWindow w = new SalesForecastReopenWindow();
        w.setExpiresAt(NOW.plusHours(1));
        when(windowRepository.findFirstByMonthAndChannelAndEndedAtIsNullAndExpiresAtAfter("202612", "家樂福", NOW))
                .thenReturn(Optional.of(w));
        assertDoesNotThrow(() -> guard.assertBusinessEditable(config(true), "家樂福"));
        assertEquals(ForecastEditGuard.Status.REOPENED, guard.status(config(true), "家樂福"));
    }
}
