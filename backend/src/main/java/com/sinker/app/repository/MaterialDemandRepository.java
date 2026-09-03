package com.sinker.app.repository;

import com.sinker.app.entity.MaterialDemand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaterialDemandRepository extends JpaRepository<MaterialDemand, Integer> {

    List<MaterialDemand> findByWeekStartAndFactoryOrderByMaterialCodeAsc(LocalDate weekStart, String factory);

    void deleteByWeekStartAndFactory(LocalDate weekStart, String factory);
}
