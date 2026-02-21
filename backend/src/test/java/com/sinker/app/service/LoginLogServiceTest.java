package com.sinker.app.service;

import com.sinker.app.entity.LoginLog;
import com.sinker.app.entity.LoginLog.LoginType;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.User;
import com.sinker.app.repository.LoginLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginLogServiceTest {

    @Mock
    private LoginLogRepository loginLogRepository;

    private LoginLogService loginLogService;

    @BeforeEach
    void setUp() {
        loginLogService = new LoginLogService(loginLogRepository);
    }

    private User createUser() {
        Role role = new Role();
        role.setId(1L);
        role.setCode("admin");
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole(role);
        return user;
    }

    @Test
    void logSuccessfulLoginCreatesEntry() {
        User user = createUser();

        loginLogService.logSuccessfulLogin(user, "192.168.1.1", "Chrome/120");

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository).save(captor.capture());

        LoginLog log = captor.getValue();
        assertEquals(LoginType.success, log.getLoginType());
        assertEquals(1L, log.getUserId());
        assertEquals("admin", log.getUsername());
        assertEquals("192.168.1.1", log.getIpAddress());
        assertEquals("Chrome/120", log.getUserAgent());
        assertNull(log.getFailedReason());
    }

    @Test
    void logFailedLoginCreatesEntry() {
        loginLogService.logFailedLogin("hacker", null, "10.0.0.1", "curl/7.0", "Invalid username or password");

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository).save(captor.capture());

        LoginLog log = captor.getValue();
        assertEquals(LoginType.failed, log.getLoginType());
        assertNull(log.getUserId());
        assertEquals("hacker", log.getUsername());
        assertEquals("10.0.0.1", log.getIpAddress());
        assertEquals("curl/7.0", log.getUserAgent());
        assertEquals("Invalid username or password", log.getFailedReason());
    }

    @Test
    void logSuccessfulLoginWithNullIP() {
        User user = createUser();

        loginLogService.logSuccessfulLogin(user, null, "Chrome/120");

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository).save(captor.capture());
        assertNull(captor.getValue().getIpAddress());
    }

    @Test
    void logFailedLoginWithNullUserAgent() {
        loginLogService.logFailedLogin("user1", 2L, "192.168.1.1", null, "Account is locked");

        ArgumentCaptor<LoginLog> captor = ArgumentCaptor.forClass(LoginLog.class);
        verify(loginLogRepository).save(captor.capture());
        assertNull(captor.getValue().getUserAgent());
    }

    @Test
    void logSuccessfulLoginSwallowsException() {
        User user = createUser();
        when(loginLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> loginLogService.logSuccessfulLogin(user, "1.2.3.4", "agent"));
    }

    @Test
    void logFailedLoginSwallowsException() {
        when(loginLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> loginLogService.logFailedLogin("user", null, "1.2.3.4", "agent", "reason"));
    }
}
