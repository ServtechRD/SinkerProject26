package com.sinker.app.service;

import com.sinker.app.dto.auth.LoginRequest;
import com.sinker.app.dto.auth.LoginResponse;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.User;
import com.sinker.app.exception.AccountInactiveException;
import com.sinker.app.exception.AccountLockedException;
import com.sinker.app.repository.LoginLogRepository;
import com.sinker.app.repository.UserRepository;
import com.sinker.app.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoginLogRepository loginLogRepository;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private AuthService authService;
    private LoginLogService loginLogService;

    private static final String TEST_IP = "127.0.0.1";
    private static final String TEST_UA = "TestAgent/1.0";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        tokenProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-tests-must-be-at-least-32-bytes!", 86400000L);
        loginLogService = new LoginLogService(loginLogRepository);
        authService = new AuthService(userRepository, passwordEncoder, tokenProvider, loginLogService);
    }

    private User createTestUser(String username, String password, boolean active, boolean locked) {
        return createTestUser(username, password, active, locked, 0);
    }

    private User createTestUser(String username, String password, boolean active, boolean locked, int failedCount) {
        Role role = new Role();
        role.setId(1L);
        role.setCode("admin");
        role.setName("Administrator");

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@sinker.local");
        user.setHashedPassword(passwordEncoder.encode(password));
        user.setFullName("Test User");
        user.setRole(role);
        user.setIsActive(active);
        user.setIsLocked(locked);
        user.setFailedLoginCount(failedCount);
        return user;
    }

    private LoginResponse doLogin(LoginRequest request) {
        return authService.login(request, TEST_IP, TEST_UA);
    }

    // --- Existing tests updated for new signature ---

    @Test
    void loginWithValidCredentials() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResponse response = doLogin(new LoginRequest("admin", "admin123"));

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isBlank());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("admin", response.getUser().getUsername());
        assertEquals("admin@sinker.local", response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getFullName());
        assertEquals("admin", response.getUser().getRoleCode());
        assertEquals(1L, response.getUser().getId());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithEmailReturnsToken() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByEmail("admin@sinker.local")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@sinker.local");
        request.setPassword("admin123");

        LoginResponse response = doLogin(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("admin", response.getUser().getUsername());
        verify(userRepository, never()).findByUsername(any());
        verify(userRepository).findByEmail("admin@sinker.local");
    }

    @Test
    void loginUpdatesLastLoginAt() {
        User user = createTestUser("admin", "admin123", true, false);
        assertNull(user.getLastLoginAt());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        doLogin(new LoginRequest("admin", "admin123"));

        assertNotNull(user.getLastLoginAt());
        verify(userRepository).save(user);
    }

    @Test
    void loginWithInvalidUsername() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> doLogin(new LoginRequest("nonexistent", "password")));
    }

    @Test
    void loginWithInvalidEmail() {
        when(userRepository.findByEmail("bad@email.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("bad@email.com");
        request.setPassword("password");

        assertThrows(UsernameNotFoundException.class, () -> doLogin(request));
    }

    @Test
    void loginWithInvalidPassword() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThrows(BadCredentialsException.class,
                () -> doLogin(new LoginRequest("admin", "wrongpassword")));
    }

    @Test
    void loginWithInactiveAccount() {
        User user = createTestUser("inactive", "password", false, false);
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(user));

        AccountInactiveException ex = assertThrows(AccountInactiveException.class,
                () -> doLogin(new LoginRequest("inactive", "password")));
        assertEquals("Account is inactive", ex.getMessage());
    }

    @Test
    void loginWithLockedAccount() {
        User user = createTestUser("locked", "password", true, true);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(user));

        AccountLockedException ex = assertThrows(AccountLockedException.class,
                () -> doLogin(new LoginRequest("locked", "password")));
        assertEquals("Account is locked", ex.getMessage());
    }

    @Test
    void loginTrimsUsername() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResponse response = doLogin(new LoginRequest("  admin  ", "admin123"));
        assertNotNull(response);
        assertEquals("admin", response.getUser().getUsername());
    }

    @Test
    void loginTokenIsValidJwt() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResponse response = doLogin(new LoginRequest("admin", "admin123"));
        String token = response.getToken();

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("admin", tokenProvider.getUsernameFromToken(token));
        assertEquals(1L, tokenProvider.getUserIdFromToken(token));
        assertEquals("admin", tokenProvider.getRoleCodeFromToken(token));
    }

    @Test
    void loginWithNoUsernameOrEmailThrows() {
        LoginRequest request = new LoginRequest();
        request.setPassword("admin123");

        assertThrows(BadCredentialsException.class, () -> doLogin(request));
    }

    @Test
    void loginPrefersUsernameOverEmail() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginRequest request = new LoginRequest("admin", "admin123");
        request.setEmail("admin@sinker.local");

        LoginResponse response = doLogin(request);
        assertNotNull(response);
        verify(userRepository).findByUsername("admin");
        verify(userRepository, never()).findByEmail(any());
    }

    // --- T010: Login Logging Tests ---

    @Test
    void successfulLoginCreatesLogEntry() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        doLogin(new LoginRequest("admin", "admin123"));

        verify(loginLogRepository).save(argThat(log ->
                log.getLoginType() == com.sinker.app.entity.LoginLog.LoginType.success
                        && "admin".equals(log.getUsername())
                        && log.getUserId().equals(1L)
                        && TEST_IP.equals(log.getIpAddress())
                        && TEST_UA.equals(log.getUserAgent())
                        && log.getFailedReason() == null
        ));
    }

    @Test
    void failedLoginInvalidPasswordCreatesLogEntry() {
        User user = createTestUser("admin", "admin123", true, false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThrows(BadCredentialsException.class,
                () -> doLogin(new LoginRequest("admin", "wrong")));

        verify(loginLogRepository).save(argThat(log ->
                log.getLoginType() == com.sinker.app.entity.LoginLog.LoginType.failed
                        && "admin".equals(log.getUsername())
                        && log.getUserId().equals(1L)
                        && "Invalid username or password".equals(log.getFailedReason())
        ));
    }

    @Test
    void failedLoginNonExistentUserCreatesLogWithNullUserId() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> doLogin(new LoginRequest("ghost", "password")));

        verify(loginLogRepository).save(argThat(log ->
                log.getLoginType() == com.sinker.app.entity.LoginLog.LoginType.failed
                        && "ghost".equals(log.getUsername())
                        && log.getUserId() == null
                        && "Invalid username or password".equals(log.getFailedReason())
        ));
    }

    @Test
    void failedLoginLockedAccountCreatesLogEntry() {
        User user = createTestUser("locked", "password", true, true);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class,
                () -> doLogin(new LoginRequest("locked", "password")));

        verify(loginLogRepository).save(argThat(log ->
                log.getLoginType() == com.sinker.app.entity.LoginLog.LoginType.failed
                        && "locked".equals(log.getUsername())
                        && "Account is locked".equals(log.getFailedReason())
        ));
    }

    @Test
    void failedLoginInactiveAccountCreatesLogEntry() {
        User user = createTestUser("inactive", "password", false, false);
        when(userRepository.findByUsername("inactive")).thenReturn(Optional.of(user));

        assertThrows(AccountInactiveException.class,
                () -> doLogin(new LoginRequest("inactive", "password")));

        verify(loginLogRepository).save(argThat(log ->
                log.getLoginType() == com.sinker.app.entity.LoginLog.LoginType.failed
                        && "inactive".equals(log.getUsername())
                        && "Account is inactive".equals(log.getFailedReason())
        ));
    }

    // --- T010: Lockout Logic Tests ---

    @Test
    void successfulLoginResetsFailedCount() {
        User user = createTestUser("admin", "admin123", true, false, 3);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        doLogin(new LoginRequest("admin", "admin123"));

        assertEquals(0, user.getFailedLoginCount());
        verify(userRepository).save(user);
    }

    @Test
    void failedLoginIncrementsFailedCount() {
        User user = createTestUser("admin", "admin123", true, false, 2);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThrows(BadCredentialsException.class,
                () -> doLogin(new LoginRequest("admin", "wrong")));

        assertEquals(3, user.getFailedLoginCount());
        assertFalse(user.getIsLocked());
    }

    @Test
    void fifthFailedLoginLocksAccount() {
        User user = createTestUser("admin", "admin123", true, false, 4);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThrows(AccountLockedException.class,
                () -> doLogin(new LoginRequest("admin", "wrong")));

        assertEquals(5, user.getFailedLoginCount());
        assertTrue(user.getIsLocked());
    }

    @Test
    void lockedAccountDoesNotIncrementCount() {
        User user = createTestUser("locked", "password", true, true, 5);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(user));

        assertThrows(AccountLockedException.class,
                () -> doLogin(new LoginRequest("locked", "password")));

        assertEquals(5, user.getFailedLoginCount());
        verify(userRepository, never()).save(any());
    }

    @Test
    void fourFailedAttemptsThenSuccessResetsCount() {
        User user = createTestUser("admin", "admin123", true, false, 4);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        LoginResponse response = doLogin(new LoginRequest("admin", "admin123"));

        assertNotNull(response);
        assertEquals(0, user.getFailedLoginCount());
        assertFalse(user.getIsLocked());
    }
}
