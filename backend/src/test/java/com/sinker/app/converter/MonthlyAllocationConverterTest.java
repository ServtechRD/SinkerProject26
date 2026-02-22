package com.sinker.app.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonthlyAllocationConverterTest {

    private MonthlyAllocationConverter converter;

    @BeforeEach
    void setUp() {
        converter = new MonthlyAllocationConverter();
    }

    @Test
    void testConvertToDatabaseColumn_ValidMap() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("01", new BigDecimal("100.50"));
        allocation.put("02", new BigDecimal("200.75"));
        allocation.put("03", new BigDecimal("150.25"));

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"01\""));
        assertTrue(json.contains("\"02\""));
        assertTrue(json.contains("\"03\""));
        assertTrue(json.contains("100.50") || json.contains("100.5"));
        assertTrue(json.contains("200.75"));
        assertTrue(json.contains("150.25"));
    }

    @Test
    void testConvertToDatabaseColumn_EmptyMap() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertEquals("{}", json);
    }

    @Test
    void testConvertToDatabaseColumn_NullMap() {
        // When
        String json = converter.convertToDatabaseColumn(null);

        // Then
        assertEquals("{}", json);
    }

    @Test
    void testConvertToDatabaseColumn_MapWithNullValues() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("01", new BigDecimal("100"));
        allocation.put("02", null);

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"01\""));
        assertTrue(json.contains("100"));
    }

    @Test
    void testConvertToEntityAttribute_ValidJson() {
        // Given
        String json = "{\"01\":100.50,\"02\":200.75,\"03\":150.25}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(allocation);
        assertEquals(3, allocation.size());
        assertEquals(new BigDecimal("100.50"), allocation.get("01"));
        assertEquals(new BigDecimal("200.75"), allocation.get("02"));
        assertEquals(new BigDecimal("150.25"), allocation.get("03"));
    }

    @Test
    void testConvertToEntityAttribute_EmptyJson() {
        // Given
        String json = "{}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }

    @Test
    void testConvertToEntityAttribute_NullJson() {
        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(null);

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }

    @Test
    void testConvertToEntityAttribute_EmptyString() {
        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute("");

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }

    @Test
    void testConvertToEntityAttribute_InvalidJson() {
        // Given
        String invalidJson = "{invalid json}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(invalidJson);

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }

    @Test
    void testConvertToEntityAttribute_MalformedJson() {
        // Given
        String malformedJson = "{\"01\":100,\"02\":";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(malformedJson);

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }

    @Test
    void testRoundTrip_ValidData() {
        // Given
        Map<String, BigDecimal> original = new HashMap<>();
        original.put("01", new BigDecimal("100.50"));
        original.put("02", new BigDecimal("200.75"));
        original.put("03", new BigDecimal("150.25"));

        // When
        String json = converter.convertToDatabaseColumn(original);
        Map<String, BigDecimal> result = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(result);
        assertEquals(original.size(), result.size());
        assertEquals(original.get("01"), result.get("01"));
        assertEquals(original.get("02"), result.get("02"));
        assertEquals(original.get("03"), result.get("03"));
    }

    @Test
    void testRoundTrip_EmptyMap() {
        // Given
        Map<String, BigDecimal> original = new HashMap<>();

        // When
        String json = converter.convertToDatabaseColumn(original);
        Map<String, BigDecimal> result = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToDatabaseColumn_SingleEntry() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("12", new BigDecimal("999.99"));

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"12\""));
        assertTrue(json.contains("999.99"));
    }

    @Test
    void testConvertToEntityAttribute_SingleEntry() {
        // Given
        String json = "{\"12\":999.99}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(allocation);
        assertEquals(1, allocation.size());
        assertEquals(new BigDecimal("999.99"), allocation.get("12"));
    }

    @Test
    void testConvertToDatabaseColumn_LargeNumbers() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("01", new BigDecimal("99999999.99"));

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("99999999.99"));
    }

    @Test
    void testConvertToEntityAttribute_LargeNumbers() {
        // Given
        String json = "{\"01\":99999999.99}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(allocation);
        assertEquals(new BigDecimal("99999999.99"), allocation.get("01"));
    }

    @Test
    void testConvertToDatabaseColumn_ZeroValue() {
        // Given
        Map<String, BigDecimal> allocation = new HashMap<>();
        allocation.put("01", BigDecimal.ZERO);

        // When
        String json = converter.convertToDatabaseColumn(allocation);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"01\""));
        assertTrue(json.contains("0"));
    }

    @Test
    void testConvertToEntityAttribute_ZeroValue() {
        // Given
        String json = "{\"01\":0}";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(allocation);
        assertEquals(BigDecimal.ZERO, allocation.get("01"));
    }

    @Test
    void testConvertToEntityAttribute_ArrayInsteadOfObject() {
        // Given
        String invalidJson = "[1,2,3]";

        // When
        Map<String, BigDecimal> allocation = converter.convertToEntityAttribute(invalidJson);

        // Then
        assertNotNull(allocation);
        assertTrue(allocation.isEmpty());
    }
}
