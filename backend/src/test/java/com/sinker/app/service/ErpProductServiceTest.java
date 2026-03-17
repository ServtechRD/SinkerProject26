package com.sinker.app.service;

import com.sinker.app.entity.Product;
import com.sinker.app.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ErpProductService service;

    @BeforeEach
    void setUp() {
        service = new ErpProductService(productRepository);
    }

    @Test
    void testValidateProduct_WhenProductExists_ReturnsTrue() {
        when(productRepository.findByCode("P001")).thenReturn(Optional.of(new Product()));
        assertTrue(service.validateProduct("P001"));
    }

    @Test
    void testValidateProduct_WhenProductNotExists_ReturnsFalse() {
        when(productRepository.findByCode(anyString())).thenReturn(Optional.empty());
        assertFalse(service.validateProduct("UNKNOWN-CODE-XYZ"));
        assertFalse(service.validateProduct("P999"));
    }

    @Test
    void testValidateProduct_WhenCodeNullOrBlank_ReturnsFalse() {
        assertFalse(service.validateProduct(null));
        assertFalse(service.validateProduct(""));
        assertFalse(service.validateProduct("   "));
    }
}
