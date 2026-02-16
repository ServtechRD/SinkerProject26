package com.sinker.app.service;

import com.sinker.app.dto.auth.LoginRequest;
import com.sinker.app.dto.auth.LoginResponse;
import com.sinker.app.entity.User;
import com.sinker.app.exception.AccountInactiveException;
import com.sinker.app.exception.AccountLockedException;
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
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();

        if (!StringUtils.hasText(username) && !StringUtils.hasText(email)) {
            throw new BadCredentialsException("Username or email is required");
        }

        Optional<User> optionalUser;
        if (StringUtils.hasText(username)) {
            optionalUser = userRepository.findByUsername(username.trim());
            if (optionalUser.isEmpty()) {
                log.debug("Login failed: user not found by username '{}'", username.trim());
            }
        } else {
            optionalUser = userRepository.findByEmail(email.trim());
            if (optionalUser.isEmpty()) {
                log.debug("Login failed: user not found by email '{}'", email.trim());
            }
        }

        User user = optionalUser
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AccountLockedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            log.debug("Login failed: password mismatch for user '{}'", user.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(
                user.getId(), user.getUsername(), user.getRole().getCode());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().getCode());

        log.info("Login successful for user '{}'", user.getUsername());

        return new LoginResponse(token, userInfo);
    }
}
