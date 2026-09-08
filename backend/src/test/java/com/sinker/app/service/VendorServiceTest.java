package com.sinker.app.service;

import com.sinker.app.dto.reference.VendorDTO;
import com.sinker.app.entity.Vendor;
import com.sinker.app.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private ErpVendorSyncClient erpVendorSyncClient;

    private VendorService service;

    @BeforeEach
    void setUp() {
        service = new VendorService(vendorRepository, erpVendorSyncClient);
    }

    @Test
    void search_returnsMockData_whenErpNotConfigured() {
        when(erpVendorSyncClient.isConfigured()).thenReturn(false);

        List<VendorDTO> result = service.search(null);

        assertFalse(result.isEmpty());
        verifyNoInteractions(vendorRepository);
    }

    @Test
    void search_filtersMockData_byKeyword() {
        when(erpVendorSyncClient.isConfigured()).thenReturn(false);

        List<VendorDTO> result = service.search("V001");

        assertEquals(1, result.size());
        assertEquals("V001", result.get(0).getCode());
    }

    @Test
    void search_queriesRepository_whenErpConfigured() {
        when(erpVendorSyncClient.isConfigured()).thenReturn(true);
        Vendor entity = new Vendor();
        entity.setCode("R001");
        entity.setName("真實廠商");
        when(vendorRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(entity));

        List<VendorDTO> result = service.search(null);

        assertEquals(1, result.size());
        assertEquals("R001", result.get(0).getCode());
        verify(vendorRepository).findAllByOrderByCodeAsc();
    }
}
