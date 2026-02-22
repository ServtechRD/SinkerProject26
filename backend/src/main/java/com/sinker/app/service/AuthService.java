package com.sinker.app.service;

import com.sinker.app.dto.auth.LoginRequest;
import com.sinker.app.dto.auth.LoginResponse;
import com.sinker.app.entity.User;
import com.sinker.app.exception.AccountInactiveException;
import com.sinker.app.exception.AccountLockedException;
import com.sinker.app.repository.PermissionRepository;
import com.sinker.app.repository.UserRepository;
import com.sinker.app.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginLogService loginLogService;
    private final PermissionRepository permissionRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       LoginLogService loginLogService,
                       PermissionRepository permissionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginLogService = loginLogService;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, AccountLockedException.class,
            UsernameNotFoundException.class, AccountInactiveException.class})
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String username = request.getUsername();
        String email = request.getEmail();

        if (!StringUtils.hasText(username) && !StringUtils.hasText(email)) {
            throw new BadCredentialsException("Username or email is required");
        }

        String loginIdentifier = StringUtils.hasText(username) ? username.trim() : email.trim();

        Optional<User> optionalUser;
        if (StringUtils.hasText(username)) {
            optionalUser = userRepository.findByUsername(username.trim());
        } else {
            optionalUser = userRepository.findByEmail(email.trim());
        }

        if (optionalUser.isEmpty()) {
            log.debug("Login failed: user not found '{}'", loginIdentifier);
            loginLogService.logFailedLogin(loginIdentifier, null, ipAddress, userAgent,
                    "Invalid username or password");
            throw new UsernameNotFoundException("Invalid username or password");
        }

        User user = optionalUser.get();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            loginLogService.logFailedLogin(user.getUsername(), user.getId(), ipAddress, userAgent,
                    "Account is inactive");
            throw new AccountInactiveException("Account is inactive");
        }

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            loginLogService.logFailedLogin(user.getUsername(), user.getId(), ipAddress, userAgent,
                    "Account is locked");
            throw new AccountLockedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            log.debug("Login failed: password mismatch for user '{}'", user.getUsername());
            int newCount = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(newCount);
            if (newCount >= MAX_FAILED_ATTEMPTS) {
                user.setIsLocked(true);
                log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), newCount);
            }
            userRepository.save(user);
            loginLogService.logFailedLogin(user.getUsername(), user.getId(), ipAddress, userAgent,
                    "Invalid username or password");
            if (Boolean.TRUE.equals(user.getIsLocked())) {
                throw new AccountLockedException("Account is locked");
            }
            throw new BadCredentialsException("Invalid username or password");
        }

        // Successful login: reset failed count and update last login
        user.setFailedLoginCount(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        loginLogService.logSuccessfulLogin(user, ipAddress, userAgent);

        String token = tokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole().getCode());

        String roleCode = user.getRole().getCode();
        List<String> permissionCodes = permissionRepository.findPermissionCodesByRoleCode(roleCode);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                roleCode,
                permissionCodes);

        log.info("Login successful for user '{}'", user.getUsername());

        return new LoginResponse(token, userInfo);
    }
}
