package com.sinker.app.repository;

import com.sinker.app.entity.FormLogAction;
import com.sinker.app.entity.SalesForecastFormLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SalesForecastFormLogRepository extends JpaRepository<SalesForecastFormLog, Integer> {

    List<SalesForecastFormLog> findByMonthOrderByCreatedAtDescIdDesc(String month);

    Optional<SalesForecastFormLog> findFirstByMonthAndActionInOrderByCreatedAtDescIdDesc(
            String month, Collection<FormLogAction> actions);
}
