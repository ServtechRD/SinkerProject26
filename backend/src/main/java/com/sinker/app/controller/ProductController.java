package com.sinker.app.controller;

import com.sinker.app.dto.reference.ProductDTO;
import com.sinker.app.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<?> findProducts(@RequestParam(required = false) String code) {
        if (code != null && !code.isBlank()) {
            log.info("GET /api/products?code={} - lookup by 品號", code);
            return productService.findByCode(code.trim())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        log.info("GET /api/products - list all product code, name, spec, 庫位");
        List<ProductDTO> list = productService.findAll();
        return ResponseEntity.ok(list);
    }
}
