package com.sinker.app.repository;

import com.sinker.app.entity.ProductionForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionFormRepository extends JpaRepository<ProductionForm, Integer> {

    List<ProductionForm> findByYearOrderByProductCodeAsc(Integer year);

    Optional<ProductionForm> findByYearAndProductCode(Integer year, String productCode);
}
