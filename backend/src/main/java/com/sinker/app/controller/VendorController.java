package com.sinker.app.controller;

import com.sinker.app.dto.reference.VendorDTO;
import com.sinker.app.service.VendorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private static final Logger log = LoggerFactory.getLogger(VendorController.class);

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public ResponseEntity<List<VendorDTO>> findVendors(@RequestParam(required = false) String keyword) {
        log.info("GET /api/vendors?keyword={}", keyword);
        return ResponseEntity.ok(vendorService.search(keyword));
    }
}
