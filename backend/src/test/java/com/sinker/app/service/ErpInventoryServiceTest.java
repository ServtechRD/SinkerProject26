package com.sinker.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ErpInventoryServiceTest {

    private ErpInventoryService erpInventoryService;

    @BeforeEach
    void setUp() {
        erpInventoryService = new ErpInventoryService();
    }

    @Test
    void getInventoryBalance_PROD001_returnsExpectedValue() {
        BigDecimal balance = erpInventoryService.getInventoryBalance("PROD001", "2026-01");
        assertEquals(new BigDecimal("250.00"), balance);
    }

    @Test
    void getInventoryBalance_PROD002_returnsExpectedValue() {
        BigDecimal balance = erpInventoryService.getInventoryBalance("PROD002", "2026-01");
        assertEquals(new BigDecimal("150.00"), balance);
    }

    @Test
    void getInventoryBalance_genericProduct_returnsValue() {
        BigDecimal balance = erpInventoryService.getInventoryBalance("PROD999", "2026-01");
        assertNotNull(balance);
        assertTrue(balance.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(balance.compareTo(new BigDecimal("300")) <= 0);
    }

    @Test
    void getInventoryBalance_nullProductCode_returnsZero() {
        BigDecimal balance = erpInventoryService.getInventoryBalance(null, "2026-01");
        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void getInventoryBalance_emptyProductCode_returnsZero() {
        BigDecimal balance = erpInventoryService.getInventoryBalance("", "2026-01");
        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    void getSalesQuantity_PROD001_returnsExpectedValue() {
        BigDecimal sales = erpInventoryService.getSalesQuantity("PROD001", "2026-01-01", "2026-01-31");
        assertEquals(new BigDecimal("100.00"), sales);
    }

    @Test
    void getSalesQuantity_PROD002_returnsExpectedValue() {
        BigDecimal sales = erpInventoryService.getSalesQuantity("PROD002", "2026-01-01", "2026-01-31");
        assertEquals(new BigDecimal("75.00"), sales);
    }

    @Test
    void getSalesQuantity_genericProduct_returnsValue() {
        BigDecimal sales = erpInventoryService.getSalesQuantity("PROD999", "2026-01-01", "2026-01-31");
        assertNotNull(sales);
        assertTrue(sales.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(sales.compareTo(new BigDecimal("150")) <= 0);
    }

    @Test
    void getSalesQuantity_nullProductCode_returnsZero() {
        BigDecimal sales = erpInventoryService.getSalesQuantity(null, "2026-01-01", "2026-01-31");
        assertEquals(BigDecimal.ZERO, sales);
    }

    @Test
    void getSalesQuantity_emptyProductCode_returnsZero() {
        BigDecimal sales = erpInventoryService.getSalesQuantity("", "2026-01-01", "2026-01-31");
        assertEquals(BigDecimal.ZERO, sales);
    }

    @Test
    void getSalesQuantity_withDateRange_returnsValue() {
        BigDecimal sales = erpInventoryService.getSalesQuantity("PROD001", "2026-01-10", "2026-01-20");
        assertNotNull(sales);
        assertEquals(new BigDecimal("100.00"), sales);
    }

    @Test
    void getInventoryBalance_consistentResults() {
        // Same product code should return same balance
        BigDecimal balance1 = erpInventoryService.getInventoryBalance("PROD123", "2026-01");
        BigDecimal balance2 = erpInventoryService.getInventoryBalance("PROD123", "2026-01");
        assertEquals(balance1, balance2);
    }

    @Test
    void getSalesQuantity_consistentResults() {
        // Same product code should return same sales quantity
        BigDecimal sales1 = erpInventoryService.getSalesQuantity("PROD123", "2026-01-01", "2026-01-31");
        BigDecimal sales2 = erpInventoryService.getSalesQuantity("PROD123", "2026-01-01", "2026-01-31");
        assertEquals(sales1, sales2);
    }
}
