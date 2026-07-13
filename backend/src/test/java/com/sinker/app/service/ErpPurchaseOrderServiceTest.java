package com.sinker.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sinker.app.config.IntegrationProperties;
import com.sinker.app.repository.ErpPurchaseOrderDetailRepository;
import com.sinker.app.repository.ErpPurchaseOrderRecordRepository;
import com.sinker.app.repository.MaterialDemandRepository;
import com.sinker.app.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderServiceTest {

    @Mock
    private RestTemplate integrationRestTemplate;
    @Mock
    private ErpTokenService erpTokenService;
    @Mock
    private MaterialDemandRepository materialDemandRepository;
    @Mock
    private ErpPurchaseOrderRecordRepository erpPurchaseOrderRecordRepository;
    @Mock
    private ErpPurchaseOrderDetailRepository erpPurchaseOrderDetailRepository;
    @Mock
    private VendorRepository vendorRepository;

    private IntegrationProperties integrationProperties;
    private ErpPurchaseOrderService service;

    @BeforeEach
    void setUp() {
        integrationProperties = new IntegrationProperties();
        integrationProperties.getErp().setEnabled(true);
        integrationProperties.getErp().setPurchaseOrderUrl("http://erp.test/purchase-order");
        service = new ErpPurchaseOrderService(integrationProperties, integrationRestTemplate, erpTokenService,
                materialDemandRepository, erpPurchaseOrderRecordRepository, erpPurchaseOrderDetailRepository,
                vendorRepository, new ObjectMapper());
    }

    @Test
    void createPurchaseOrder_unknownVendorCode_throws() {
        when(vendorRepository.existsByCode("UNKNOWN")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.createPurchaseOrder(LocalDate.of(2026, 2, 17), "F1", "UNKNOWN"));
    }

    @Test
    void createPurchaseOrder_blankFactory_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createPurchaseOrder(LocalDate.of(2026, 2, 17), "", "V001"));
    }
}
