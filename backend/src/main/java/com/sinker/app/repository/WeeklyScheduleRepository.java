package com.sinker.app.repository;

import com.sinker.app.entity.WeeklySchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyScheduleRepository extends JpaRepository<WeeklySchedule, Integer> {

    List<WeeklySchedule> findByWeekStartAndFactoryOrderByDemandDateAscProductCodeAsc(
            LocalDate weekStart, String factory);

    @Modifying
    @Query("DELETE FROM WeeklySchedule ws WHERE ws.weekStart = :weekStart AND ws.factory = :factory")
    void deleteByWeekStartAndFactory(@Param("weekStart") LocalDate weekStart, @Param("factory") String factory);
}
