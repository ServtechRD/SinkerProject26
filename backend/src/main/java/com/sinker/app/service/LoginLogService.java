package com.sinker.app.service;

import com.sinker.app.entity.LoginLog;
import com.sinker.app.entity.LoginLog.LoginType;
import com.sinker.app.entity.User;
import com.sinker.app.repository.LoginLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLogService {

    private static final Logger log = LoggerFactory.getLogger(LoginLogService.class);

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccessfulLogin(User user, String ipAddress, String userAgent) {
        try {
            LoginLog entry = new LoginLog();
            entry.setUserId(user.getId());
            entry.setUsername(user.getUsername());
            entry.setLoginType(LoginType.success);
            entry.setIpAddress(ipAddress);
            entry.setUserAgent(userAgent);
            loginLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to log successful login for user '{}': {}", user.getUsername(), e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(String username, Long userId, String ipAddress, String userAgent, String failedReason) {
        try {
            LoginLog entry = new LoginLog();
            entry.setUserId(userId);
            entry.setUsername(username);
            entry.setLoginType(LoginType.failed);
            entry.setIpAddress(ipAddress);
            entry.setUserAgent(userAgent);
            entry.setFailedReason(failedReason);
            loginLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to log failed login for user '{}': {}", username, e.getMessage());
        }
    }
}
