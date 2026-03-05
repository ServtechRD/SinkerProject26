package com.sinker.app.repository;

import com.sinker.app.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findByUsernameOrderByCreatedAtDesc(String username);

    List<LoginLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
