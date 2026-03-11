package com.sinker.app.repository;

import com.sinker.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findAllByOrderByCodeAsc();

    Optional<Product> findByCode(String code);
}
