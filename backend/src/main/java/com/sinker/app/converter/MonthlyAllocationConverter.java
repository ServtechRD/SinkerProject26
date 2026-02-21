package com.sinker.app.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Converter
public class MonthlyAllocationConverter implements AttributeConverter<Map<String, BigDecimal>, String> {

    private static final Logger log = LoggerFactory.getLogger(MonthlyAllocationConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, BigDecimal> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert monthly allocation to JSON", e);
            throw new IllegalArgumentException("Failed to convert monthly allocation to JSON", e);
        }
    }

    @Override
    public Map<String, BigDecimal> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<Map<String, BigDecimal>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse monthly allocation JSON: {}", dbData, e);
            return new HashMap<>();
        }
    }
}
