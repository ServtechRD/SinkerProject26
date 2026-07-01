package com.sinker.app.service;

import com.sinker.app.dto.reference.VendorDTO;
import com.sinker.app.entity.Vendor;
import com.sinker.app.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    public List<VendorDTO> search(String keyword) {
        List<Vendor> list = StringUtils.hasText(keyword)
                ? vendorRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword)
                : vendorRepository.findAllByOrderByCodeAsc();
        return list.stream().map(VendorDTO::fromEntity).collect(Collectors.toList());
    }
}
