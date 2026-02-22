package com.sinker.app.repository;

import com.sinker.app.entity.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Integer> {

    List<ProductionPlan> findByYearOrderByProductCodeAscChannelAsc(Integer year);
}
