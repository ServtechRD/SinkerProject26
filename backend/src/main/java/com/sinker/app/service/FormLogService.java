package com.sinker.app.service;

import com.sinker.app.dto.forecast.FormLogDTO;
import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import com.sinker.app.repository.SalesForecastFormLogRepository;
import com.sinker.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 表單異動紀錄：只新增、不提供修改與刪除 */
@Service
public class FormLogService {

    private static final int DETAIL_MAX = 500;

    private final SalesForecastFormLogRepository repository;
    private final UserRepository userRepository;
    private final Clock clock;

    public FormLogService(SalesForecastFormLogRepository repository, UserRepository userRepository, Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void record(String month, String channel, FormLogAction action, Long operatorId, String detail) {
        SalesForecastFormLog entry = new SalesForecastFormLog();
        entry.setMonth(month);
        entry.setChannel(channel);
        entry.setAction(action);
        entry.setOperatorId(operatorId);
        entry.setDetail(detail != null && detail.length() > DETAIL_MAX ? detail.substring(0, DETAIL_MAX) : detail);
        entry.setCreatedAt(LocalDateTime.now(clock));
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<FormLogDTO> list(String month) {
        List<SalesForecastFormLog> logs = repository.findByMonthOrderByCreatedAtDescIdDesc(month);
        Set<Long> ids = logs.stream().map(SalesForecastFormLog::getOperatorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            userRepository.findAllById(ids).forEach(u -> names.put(u.getId(),
                    u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername()));
        }
        return logs.stream().map(l -> new FormLogDTO(
                l.getId(),
                l.getCreatedAt(),
                l.getOperatorId() == null ? "系統" : names.getOrDefault(l.getOperatorId(), "使用者#" + l.getOperatorId()),
                l.getAction().name(),
                l.getChannel(),
                l.getDetail())).toList();
    }
}
