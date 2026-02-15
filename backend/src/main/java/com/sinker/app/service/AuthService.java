package com.sinker.app.service;

import com.sinker.app.dto.auth.LoginRequest;
import com.sinker.app.dto.auth.LoginResponse;
import com.sinker.app.entity.User;
import com.sinker.app.exception.AccountInactiveException;
import com.sinker.app.exception.AccountLockedException;
import com.sinker.app.repository.UserRepository;
import com.sinker.app.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

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
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AccountInactiveException("Account is inactive");
        }

        if (Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AccountLockedException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
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

        return new LoginResponse(token, userInfo);
    }
}
