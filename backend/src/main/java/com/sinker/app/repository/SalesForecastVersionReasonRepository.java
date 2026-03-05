package com.sinker.app.repository;

import com.sinker.app.entity.SalesForecastVersionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalesForecastVersionReasonRepository extends JpaRepository<SalesForecastVersionReason, Long> {

    Optional<SalesForecastVersionReason> findByMonthAndChannelAndVersion(String month, String channel, String version);
}
