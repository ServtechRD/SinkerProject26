package com.sinker.app.service;

import com.sinker.app.dto.forecast.ChannelCellDTO;
import com.sinker.app.dto.forecast.ChannelVersionInfoDTO;
import com.sinker.app.dto.forecast.CopyVersionResponse;
import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.FormSummaryResponse;
import com.sinker.app.dto.forecast.FormSummaryRowDTO;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.dto.forecast.VersionDiffItemDTO;
import com.sinker.app.dto.forecast.VersionInfo;
import com.sinker.app.entity.SalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.SalesForecastVersionReason;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.SalesForecastRepository;
import com.sinker.app.repository.SalesForecastVersionReasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class SalesForecastService {

    private static final Logger log = LoggerFactory.getLogger(SalesForecastService.class);
    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static final List<String> FORM_SUMMARY_CHANNEL_ORDER = List.of(
            "PX/大全聯", "家樂福", "7-11", "全家", "萊爾富", "OK超商",
            "美廉社", "愛買", "大潤發", "好市多", "頂好", "楓康");

    private final SalesForecastRepository forecastRepository;
    private final SalesForecastConfigRepository configRepository;
    private final SalesForecastVersionReasonRepository versionReasonRepository;
    private final ErpProductService erpProductService;
    private final JdbcTemplate jdbcTemplate;

    public SalesForecastService(SalesForecastRepository forecastRepository,
                               SalesForecastConfigRepository configRepository,
                               SalesForecastVersionReasonRepository versionReasonRepository,
                               ErpProductService erpProductService,
                               JdbcTemplate jdbcTemplate) {
        this.forecastRepository = forecastRepository;
        this.configRepository = configRepository;
        this.versionReasonRepository = versionReasonRepository;
        this.erpProductService = erpProductService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ForecastResponse createForecast(CreateForecastRequest request, Long userId, String roleCode, Set<String> authorities) {
        log.info("Creating forecast: user={}, month={}, channel={}, productCode={}",
                userId, request.getMonth(), request.getChannel(), request.getProductCode());

        validateMonthFormat(request.getMonth());
        if (!Boolean.TRUE.equals(authorities != null && authorities.contains("sales_forecast.update_after_closed"))) {
            validateMonthOpen(request.getMonth());
        }
        checkChannelOwnership(userId, request.getChannel(), roleCode);

        // Validate product via ERP
        if (!erpProductService.validateProduct(request.getProductCode())) {
            throw new IllegalArgumentException("Invalid product_code: " + request.getProductCode());
        }

        // Check for duplicate
        if (forecastRepository.findByMonthAndChannelAndProductCode(
                request.getMonth(), request.getChannel(), request.getProductCode()).isPresent()) {
            throw new DuplicateEntryException(
                    "Product " + request.getProductCode() + " already exists for month "
                            + request.getMonth() + " and channel " + request.getChannel());
        }

        LocalDateTime now = LocalDateTime.now();
        // Use existing version for this month+channel so new row appears in same version list (不加版次)
        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(
                request.getMonth(), request.getChannel());
        String version = (versions != null && !versions.isEmpty())
                ? versions.get(0)
                : now.format(VERSION_FORMATTER) + "(" + request.getChannel() + ")";

        // Create entity
        SalesForecast forecast = new SalesForecast();
        forecast.setMonth(request.getMonth());
        forecast.setChannel(request.getChannel());
        forecast.setCategory(request.getCategory());
        forecast.setSpec(request.getSpec());
        forecast.setProductCode(request.getProductCode());
        forecast.setProductName(request.getProductName());
        forecast.setWarehouseLocation(request.getWarehouseLocation());
        forecast.setQuantity(request.getQuantity());
        forecast.setVersion(version);
        forecast.setIsModified(true);
        forecast.setCreatedAt(now);
        forecast.setUpdatedAt(now);

        SalesForecast saved = forecastRepository.save(forecast);
        log.info("Created forecast: id={}, user={}, month={}, channel={}, productCode={}",
                saved.getId(), userId, request.getMonth(), request.getChannel(), request.getProductCode());

        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public ForecastResponse updateForecast(Integer id, UpdateForecastRequest request,
                                          Long userId, String roleCode, Set<String> authorities) {
        log.info("Updating forecast: id={}, user={}, quantity={}", id, userId, request.getQuantity());

        SalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));

        if (!Boolean.TRUE.equals(authorities != null && authorities.contains("sales_forecast.update_after_closed"))) {
            validateMonthOpen(forecast.getMonth());
        }
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);

        LocalDateTime now = LocalDateTime.now();

        // Update fields only; keep existing version so row stays in same version list
        forecast.setQuantity(request.getQuantity());
        forecast.setIsModified(true);
        forecast.setUpdatedAt(now);

        SalesForecast saved = forecastRepository.save(forecast);
        log.info("Updated forecast: id={}, user={}, newQuantity={}",
                id, userId, request.getQuantity());

        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteForecast(Integer id, Long userId, String roleCode) {
        log.info("Deleting forecast: id={}, user={}", id, userId);

        // Find existing forecast
        SalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));

        // Check month is open
        validateMonthOpen(forecast.getMonth());

        // Check channel ownership
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);

        // Hard delete
        forecastRepository.delete(forecast);
        log.info("Deleted forecast: id={}, user={}, month={}, channel={}, productCode={}",
                id, userId, forecast.getMonth(), forecast.getChannel(), forecast.getProductCode());
    }

    private void validateMonthFormat(String month) {
        if (month == null || !month.matches("\\d{6}")) {
            throw new IllegalArgumentException("Invalid month format. Expected YYYYMM, got: " + month);
        }
    }

    private void validateMonthOpen(String month) {
        SalesForecastConfig config = configRepository.findByMonth(month)
                .orElseThrow(() -> new ResourceNotFoundException("Month " + month + " not configured"));

        if (Boolean.TRUE.equals(config.getIsClosed())) {
            throw new AccessDeniedException("Month " + month + " is closed");
        }
    }

    private void checkChannelOwnership(Long userId, String channel, String roleCode) {
        // Admin bypasses channel ownership check
        if ("admin".equals(roleCode)) {
            return;
        }

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                Integer.class, userId, channel);
        if (count == null || count == 0) {
            throw new AccessDeniedException("No permission for channel: " + channel);
        }
    }

    // T017: Query methods
    @Transactional(readOnly = true)
    public List<ForecastResponse> queryForecasts(String month, String channel, String version,
                                                 Long userId, Set<String> authorities) {
        log.info("Querying forecasts: user={}, month={}, channel={}, version={}", userId, month, channel, version);

        // Validate month format
        validateMonthFormat(month);

        // Check permissions and channel access
        checkQueryPermission(userId, channel, authorities);

        // Query with or without version
        List<SalesForecast> forecasts;
        if (version != null && !version.isEmpty()) {
            forecasts = forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(
                    month, channel, version);
        } else {
            forecasts = forecastRepository.findLatestByMonthAndChannel(month, channel);
        }

        return forecasts.stream()
                .map(ForecastResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VersionInfo> queryVersions(String month, String channel, Long userId, Set<String> authorities) {
        log.info("Querying versions: user={}, month={}, channel={}", userId, month, channel);

        // Validate month format
        validateMonthFormat(month);

        // Check permissions and channel access
        checkQueryPermission(userId, channel, authorities);

        // Get distinct versions
        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);

        // Build version info list
        List<VersionInfo> versionInfos = new ArrayList<>();
        for (String version : versions) {
            Integer count = forecastRepository.countByMonthAndChannelAndVersion(month, channel, version);
            LocalDateTime timestamp = forecastRepository.findMaxUpdatedAtByMonthAndChannelAndVersion(
                    month, channel, version);

            versionInfos.add(new VersionInfo(version, count, timestamp));
        }

        return versionInfos;
    }

    private void checkQueryPermission(Long userId, String channel, Set<String> authorities) {
        // Check if user has sales_forecast.view (can access all channels)
        if (authorities.contains("sales_forecast.view")) {
            log.debug("User {} has sales_forecast.view permission", userId);
            return;
        }

        // Check if user has sales_forecast.view_own (can access only own channels)
        if (authorities.contains("sales_forecast.view_own")) {
            log.debug("User {} has sales_forecast.view_own permission, checking channel ownership", userId);
            // Check if user owns this channel
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                    Integer.class, userId, channel);
            if (count == null || count == 0) {
                throw new AccessDeniedException("No permission for channel: " + channel);
            }
            return;
        }

        // No valid permission
        throw new AccessDeniedException("Missing required permission: sales_forecast.view or sales_forecast.view_own");
    }

    /** Copy latest version to a new version (yyyy/MM/dd HH:mm:ss(channel)). Requires update_after_closed. */
    @Transactional
    public CopyVersionResponse copyLatestToNewVersion(String month, String channel, Long userId, String roleCode) {
        log.info("Copying latest to new version: user={}, month={}, channel={}", userId, month, channel);
        validateMonthFormat(month);
        checkChannelOwnership(userId, channel, roleCode);

        List<SalesForecast> latest = forecastRepository.findLatestByMonthAndChannel(month, channel);
        if (latest.isEmpty()) {
            throw new IllegalArgumentException("No data to copy for month=" + month + ", channel=" + channel);
        }

        LocalDateTime now = LocalDateTime.now();
        String newVersion = now.format(VERSION_FORMATTER) + "(" + channel + ")";

        for (SalesForecast src : latest) {
            SalesForecast copy = new SalesForecast();
            copy.setMonth(src.getMonth());
            copy.setChannel(src.getChannel());
            copy.setCategory(src.getCategory());
            copy.setSpec(src.getSpec());
            copy.setProductCode(src.getProductCode());
            copy.setProductName(src.getProductName());
            copy.setWarehouseLocation(src.getWarehouseLocation());
            copy.setQuantity(src.getQuantity());
            copy.setVersion(newVersion);
            copy.setIsModified(false);
            copy.setCreatedAt(now);
            copy.setUpdatedAt(now);
            forecastRepository.save(copy);
        }
        log.info("Created new version: {} with {} rows", newVersion, latest.size());
        return new CopyVersionResponse(newVersion);
    }

    /** Save change reason for a version. */
    @Transactional
    public void saveVersionReason(String month, String channel, String version, String changeReason) {
        SalesForecastVersionReason entity = versionReasonRepository
                .findByMonthAndChannelAndVersion(month, channel, version)
                .orElseGet(() -> {
                    SalesForecastVersionReason e = new SalesForecastVersionReason();
                    e.setMonth(month);
                    e.setChannel(channel);
                    e.setVersion(version);
                    e.setCreatedAt(LocalDateTime.now());
                    return e;
                });
        entity.setChangeReason(changeReason);
        entity.setUpdatedAt(LocalDateTime.now());
        versionReasonRepository.save(entity);
    }

    /** Delete all forecast rows for a version (cancel new version). */
    @Transactional
    public void deleteVersion(String month, String channel, String version, Long userId, String roleCode) {
        log.info("Deleting version: user={}, month={}, channel={}, version={}", userId, month, channel, version);
        validateMonthFormat(month);
        checkChannelOwnership(userId, channel, roleCode);
        forecastRepository.deleteByMonthAndChannelAndVersion(month, channel, version);
        versionReasonRepository.findByMonthAndChannelAndVersion(month, channel, version)
                .ifPresent(versionReasonRepository::delete);
    }

    /** Diff current version vs previous version: rows where quantity differs. */
    @Transactional(readOnly = true)
    public List<VersionDiffItemDTO> getVersionDiff(String month, String channel, String version,
                                                    Long userId, Set<String> authorities) {
        validateMonthFormat(month);
        checkQueryPermission(userId, channel, authorities);

        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);
        int idx = versions.indexOf(version);
        if (idx < 0 || idx + 1 >= versions.size()) {
            return List.of();
        }
        String previousVersion = versions.get(idx + 1);

        List<SalesForecast> currentRows = forecastRepository
                .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, version);
        List<SalesForecast> previousRows = forecastRepository
                .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, previousVersion);

        Map<String, BigDecimal> prevQtyByProduct = previousRows.stream()
                .collect(Collectors.toMap(SalesForecast::getProductCode, SalesForecast::getQuantity, (a, b) -> a, LinkedHashMap::new));

        List<VersionDiffItemDTO> result = new ArrayList<>();
        for (SalesForecast row : currentRows) {
            BigDecimal prevQty = prevQtyByProduct.get(row.getProductCode());
            if (prevQty == null || row.getQuantity().compareTo(prevQty) != 0) {
                VersionDiffItemDTO dto = new VersionDiffItemDTO();
                dto.setCategory(row.getCategory());
                dto.setSpec(row.getSpec());
                dto.setProductCode(row.getProductCode());
                dto.setProductName(row.getProductName());
                dto.setWarehouseLocation(row.getWarehouseLocation());
                dto.setCurrentQuantity(row.getQuantity());
                dto.setPreviousQuantity(prevQty != null ? prevQty : BigDecimal.ZERO);
                result.add(dto);
            }
        }
        return result;
    }

    public static class DuplicateEntryException extends RuntimeException {
        public DuplicateEntryException(String message) {
            super(message);
        }
    }
}
