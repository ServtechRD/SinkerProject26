package com.sinker.app.service;

import com.sinker.app.dto.reference.ProductDTO;
import com.sinker.app.entity.Product;
import com.sinker.app.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDTO> findAll() {
        List<Product> list = productRepository.findAllByOrderByCodeAsc();
        return list.stream().map(ProductDTO::fromEntity).collect(Collectors.toList());
    }

    public Optional<ProductDTO> findByCode(String code) {
        return productRepository.findByCode(code).map(ProductDTO::fromEntity);
    }
}
