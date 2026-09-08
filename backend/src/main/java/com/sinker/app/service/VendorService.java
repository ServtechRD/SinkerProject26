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

    // ponytail: ERP_VENDOR_ENABLED 尚未開啟（未串接外部 API）時的暫代資料，isConfigured() 轉為 true 後會自動改查 DB 的真實同步資料
    private static final List<VendorDTO> MOCK_VENDORS = List.of(
            mockVendor("V001", "測試廠商A"),
            mockVendor("V002", "測試廠商B"),
            mockVendor("V003", "測試廠商C"));

    private final VendorRepository vendorRepository;
    private final ErpVendorSyncClient erpVendorSyncClient;

    public VendorService(VendorRepository vendorRepository, ErpVendorSyncClient erpVendorSyncClient) {
        this.vendorRepository = vendorRepository;
        this.erpVendorSyncClient = erpVendorSyncClient;
    }

    public List<VendorDTO> search(String keyword) {
        if (!erpVendorSyncClient.isConfigured()) {
            return searchMock(keyword);
        }
        List<Vendor> list = StringUtils.hasText(keyword)
                ? vendorRepository.findByCodeContainingIgnoreCaseOrNameContainingIgnoreCase(keyword, keyword)
                : vendorRepository.findAllByOrderByCodeAsc();
        return list.stream().map(VendorDTO::fromEntity).collect(Collectors.toList());
    }

    private List<VendorDTO> searchMock(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return MOCK_VENDORS;
        }
        String lower = keyword.toLowerCase();
        return MOCK_VENDORS.stream()
                .filter(v -> v.getCode().toLowerCase().contains(lower) || v.getName().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }

    private static VendorDTO mockVendor(String code, String name) {
        VendorDTO dto = new VendorDTO();
        dto.setCode(code);
        dto.setName(name);
        return dto;
    }
}
