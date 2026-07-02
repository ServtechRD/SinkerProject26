package com.sinker.app.service;

import com.sinker.app.dto.erp.VendorListRequest;
import com.sinker.app.dto.erp.VendorSyncItem;
import com.sinker.app.dto.erp.VendorSyncParam;
import com.sinker.app.entity.Vendor;
import com.sinker.app.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpVendorSyncServiceTest {

    @Mock
    private ErpVendorSyncClient client;

    @Mock
    private VendorRepository vendorRepository;

    private ErpVendorSyncService service;

    @BeforeEach
    void setUp() {
        service = new ErpVendorSyncService(client, vendorRepository);
    }

    private VendorSyncItem item(String cusNo, String name, String sysDate, String modifyDate) {
        VendorSyncItem item = new VendorSyncItem();
        item.setCusNo(cusNo);
        item.setName(name);
        item.setSysDate(sysDate);
        item.setModifyDate(modifyDate);
        return item;
    }

    @Test
    @SuppressWarnings("unchecked")
    void doSync_stopsWhenPageReturnsFewerThanPageSize() {
        when(client.isConfigured()).thenReturn(true);
        VendorSyncParam param = new VendorSyncParam();
        param.setPageSize(2);

        List<VendorSyncItem> page1 = List.of(
                item("V001", "廠商1", "2020-01-01 00:00:00", "2020-01-02 00:00:00"),
                item("V002", "廠商2", null, null));
        List<VendorSyncItem> page2 = List.of(
                item("V003", "廠商3", null, null));

        when(client.fetchPage(any(VendorListRequest.class))).thenReturn(page1, page2);
        when(vendorRepository.findByCodeIn(any())).thenReturn(List.of());

        service.syncVendorsAsync(param);

        ArgumentCaptor<VendorListRequest> captor = ArgumentCaptor.forClass(VendorListRequest.class);
        verify(client, times(2)).fetchPage(captor.capture());
        assertEquals(1, captor.getAllValues().get(0).getNowPage());
        assertEquals(2, captor.getAllValues().get(1).getNowPage());

        Map<String, Object> status = service.getSyncStatus();
        Map<String, Object> result = (Map<String, Object>) status.get("lastResult");
        assertEquals(3, result.get("totalFetched"));
        assertNull(status.get("lastError"));
        assertFalse(service.isRunning());
    }

    @Test
    void doSync_isOnlyUpdateTrue_doesNotInsertNewVendor() {
        when(client.isConfigured()).thenReturn(true);
        VendorSyncParam param = new VendorSyncParam();
        param.setIsOnlyUpdate(true);
        param.setPageSize(10);

        when(client.fetchPage(any(VendorListRequest.class)))
                .thenReturn(List.of(item("V999", "新廠商", null, null)));
        when(vendorRepository.findByCodeIn(any())).thenReturn(List.of());

        service.syncVendorsAsync(param);

        verify(vendorRepository).saveAll(argThat(iterable -> !iterable.iterator().hasNext()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void doSync_invalidDate_savesNullWithoutThrowing() {
        when(client.isConfigured()).thenReturn(true);
        VendorSyncParam param = new VendorSyncParam();
        param.setPageSize(10);

        when(client.fetchPage(any(VendorListRequest.class)))
                .thenReturn(List.of(item("V010", "壞日期廠商", "not-a-date", "2020-01-02 00:00:00")));
        when(vendorRepository.findByCodeIn(any())).thenReturn(List.of());

        service.syncVendorsAsync(param);

        ArgumentCaptor<List<Vendor>> captor = ArgumentCaptor.forClass(List.class);
        verify(vendorRepository).saveAll(captor.capture());
        Vendor saved = captor.getValue().iterator().next();
        assertNull(saved.getSysDate());
        assertEquals(LocalDateTime.of(2020, 1, 2, 0, 0), saved.getModifyDate());
        assertNull(service.getSyncStatus().get("lastError"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void doSync_existingVendorMatchedCaseInsensitive_updatesInsteadOfInserting() {
        when(client.isConfigured()).thenReturn(true);
        VendorSyncParam param = new VendorSyncParam();
        param.setPageSize(10);

        Vendor existing = new Vendor();
        existing.setCode("v020");
        existing.setName("舊名稱");

        when(client.fetchPage(any(VendorListRequest.class)))
                .thenReturn(List.of(item("V020", "新名稱", null, null)));
        when(vendorRepository.findByCodeIn(any())).thenReturn(List.of(existing));

        service.syncVendorsAsync(param);

        ArgumentCaptor<List<Vendor>> captor = ArgumentCaptor.forClass(List.class);
        verify(vendorRepository).saveAll(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("新名稱", captor.getValue().iterator().next().getName());
    }

    @Test
    void doSync_notConfigured_setsLastError() {
        when(client.isConfigured()).thenReturn(false);
        VendorSyncParam param = new VendorSyncParam();

        service.syncVendorsAsync(param);

        assertNotNull(service.getSyncStatus().get("lastError"));
        assertFalse(service.isRunning());
        verify(vendorRepository, never()).saveAll(any());
    }
}
