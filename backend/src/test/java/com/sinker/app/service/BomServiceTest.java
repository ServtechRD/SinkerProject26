package com.sinker.app.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BomServiceTest {

    private final BomService bomService = new BomService();

    @Test
    void testGetKgPerBoxKnownProducts() {
        assertEquals(new BigDecimal("5.50"), bomService.getKgPerBox("P001"));
        assertEquals(new BigDecimal("3.00"), bomService.getKgPerBox("P002"));
        assertEquals(new BigDecimal("4.20"), bomService.getKgPerBox("P003"));
    }

    @Test
    void testGetKgPerBoxUnknownProduct() {
        assertEquals(new BigDecimal("1.00"), bomService.getKgPerBox("UNKNOWN"));
        assertEquals(new BigDecimal("1.00"), bomService.getKgPerBox("P999"));
    }

    @Test
    void testGetBoxesPerBarrelKnownProducts() {
        assertEquals(new BigDecimal("20.00"), bomService.getBoxesPerBarrel("P001"));
        assertEquals(new BigDecimal("15.00"), bomService.getBoxesPerBarrel("P002"));
        assertEquals(new BigDecimal("25.00"), bomService.getBoxesPerBarrel("P003"));
    }

    @Test
    void testGetBoxesPerBarrelUnknownProduct() {
        assertEquals(new BigDecimal("10.00"), bomService.getBoxesPerBarrel("UNKNOWN"));
        assertEquals(new BigDecimal("10.00"), bomService.getBoxesPerBarrel("P999"));
    }
}
