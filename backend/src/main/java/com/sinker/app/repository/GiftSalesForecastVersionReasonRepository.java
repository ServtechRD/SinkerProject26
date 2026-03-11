package com.sinker.app.repository;

import com.sinker.app.entity.GiftSalesForecastVersionReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GiftSalesForecastVersionReasonRepository extends JpaRepository<GiftSalesForecastVersionReason, Long> {

    Optional<GiftSalesForecastVersionReason> findByMonthAndChannelAndVersion(String month, String channel, String version);
}
