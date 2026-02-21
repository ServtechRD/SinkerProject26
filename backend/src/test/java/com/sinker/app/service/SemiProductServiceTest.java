package com.sinker.app.service;

import com.sinker.app.dto.semiproduct.SemiProductDTO;
import com.sinker.app.dto.semiproduct.SemiProductUpdateDTO;
import com.sinker.app.dto.semiproduct.SemiProductUploadResponse;
import com.sinker.app.entity.SemiProductAdvancePurchase;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SemiProductAdvancePurchaseRepository;
import com.sinker.app.util.SemiProductExcelParser;
import com.sinker.app.util.SemiProductExcelParser.SemiProductRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemiProductServiceTest {

    @Mock private SemiProductAdvancePurchaseRepository repository;
    @Mock private SemiProductExcelParser excelParser;

    private SemiProductService service;

    @BeforeEach
    void setUp() {
        service = new SemiProductService(repository, excelParser);
    }

    private MockMultipartFile dummyFile() {
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());
    }

    private List<SemiProductRow> makeRows(int count) {
        List<SemiProductRow> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(new SemiProductRow("SP00" + i, "半成品" + i, i + 5, i + 1));
        }
        return rows;
    }

    private SemiProductAdvancePurchase makeEntity(Integer id, String code, String name, Integer days) {
        SemiProductAdvancePurchase entity = new SemiProductAdvancePurchase();
        entity.setId(id);
        entity.setProductCode(code);
        entity.setProductName(name);
        entity.setAdvanceDays(days);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    @Test
    void testUpload_Success() {
        when(excelParser.parse(any())).thenReturn(makeRows(5));
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        SemiProductUploadResponse response = service.upload(dummyFile());

        assertEquals("Upload successful", response.getMessage());
        assertEquals(5, response.getCount());

        verify(repository).truncateTable();
        verify(repository).saveAll(anyList());
    }

    @Test
    void testUpload_TruncateCalledBeforeSave() {
        when(excelParser.parse(any())).thenReturn(makeRows(3));
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        service.upload(dummyFile());

        var inOrder = inOrder(repository);
        inOrder.verify(repository).truncateTable();
        inOrder.verify(repository).saveAll(anyList());
    }

    @Test
    void testUpload_SavesCorrectData() {
        List<SemiProductRow> rows = List.of(
                new SemiProductRow("SP001", "半成品A", 7, 2),
                new SemiProductRow("SP002", "半成品B", 10, 3)
        );
        when(excelParser.parse(any())).thenReturn(rows);
        when(repository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        service.upload(dummyFile());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SemiProductAdvancePurchase>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<SemiProductAdvancePurchase> saved = captor.getValue();
        assertEquals(2, saved.size());

        assertEquals("SP001", saved.get(0).getProductCode());
        assertEquals("半成品A", saved.get(0).getProductName());
        assertEquals(7, saved.get(0).getAdvanceDays());

        assertEquals("SP002", saved.get(1).getProductCode());
        assertEquals("半成品B", saved.get(1).getProductName());
        assertEquals(10, saved.get(1).getAdvanceDays());
    }

    @Test
    void testFindAll_ReturnsAllProducts() {
        List<SemiProductAdvancePurchase> entities = List.of(
                makeEntity(1, "SP001", "半成品A", 7),
                makeEntity(2, "SP002", "半成品B", 10)
        );
        when(repository.findAll()).thenReturn(entities);

        List<SemiProductDTO> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("SP001", result.get(0).getProductCode());
        assertEquals("半成品A", result.get(0).getProductName());
        assertEquals(7, result.get(0).getAdvanceDays());

        assertEquals("SP002", result.get(1).getProductCode());
        assertEquals("半成品B", result.get(1).getProductName());
        assertEquals(10, result.get(1).getAdvanceDays());
    }

    @Test
    void testFindAll_EmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<SemiProductDTO> result = service.findAll();

        assertEquals(0, result.size());
    }

    @Test
    void testUpdate_Success() {
        SemiProductAdvancePurchase entity = makeEntity(1, "SP001", "半成品A", 7);
        when(repository.findById(1)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(15);

        SemiProductDTO result = service.update(1, updateDTO);

        assertEquals(1, result.getId());
        assertEquals("SP001", result.getProductCode());
        assertEquals("半成品A", result.getProductName());
        assertEquals(15, result.getAdvanceDays());

        verify(repository).save(entity);
        assertEquals(15, entity.getAdvanceDays());
    }

    @Test
    void testUpdate_ProductNotFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(15);

        assertThrows(ResourceNotFoundException.class, () -> service.update(999, updateDTO));
    }

    @Test
    void testUpdate_NullAdvanceDays() {
        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(null);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, updateDTO));
    }

    @Test
    void testUpdate_NegativeAdvanceDays() {
        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(-5);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, updateDTO));
    }

    @Test
    void testUpdate_ZeroAdvanceDays() {
        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(0);

        assertThrows(IllegalArgumentException.class, () -> service.update(1, updateDTO));
    }

    @Test
    void testUpdate_DoesNotChangeProductCodeOrName() {
        SemiProductAdvancePurchase entity = makeEntity(1, "SP001", "半成品A", 7);
        when(repository.findById(1)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        SemiProductUpdateDTO updateDTO = new SemiProductUpdateDTO();
        updateDTO.setAdvanceDays(20);

        service.update(1, updateDTO);

        // Verify product_code and product_name remain unchanged
        assertEquals("SP001", entity.getProductCode());
        assertEquals("半成品A", entity.getProductName());
        assertEquals(20, entity.getAdvanceDays());
    }

    @Test
    void testGenerateTemplate_ReturnsNonEmpty() {
        byte[] template = service.generateTemplate();

        assertNotNull(template);
        assertTrue(template.length > 0, "Template should not be empty");
    }
}
