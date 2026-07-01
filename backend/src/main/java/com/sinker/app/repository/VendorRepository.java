package com.sinker.app.repository;

import com.sinker.app.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {

    List<Vendor> findAllByOrderByCodeAsc();

    List<Vendor> findByCodeIn(Collection<String> codes);

    List<Vendor> findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(String code, String name);
}
