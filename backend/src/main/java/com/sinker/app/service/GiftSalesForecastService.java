package com.sinker.app.service;

import com.sinker.app.dto.forecast.CopyVersionResponse;
import com.sinker.app.dto.forecast.CreateForecastRequest;
import com.sinker.app.dto.forecast.ForecastResponse;
import com.sinker.app.dto.forecast.UpdateForecastRequest;
import com.sinker.app.dto.forecast.VersionDiffItemDTO;
import com.sinker.app.dto.forecast.VersionInfo;
import com.sinker.app.entity.GiftSalesForecast;
import com.sinker.app.entity.SalesForecastConfig;
import com.sinker.app.entity.GiftSalesForecastVersionReason;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.SalesForecastConfigRepository;
import com.sinker.app.repository.GiftSalesForecastRepository;
import com.sinker.app.repository.GiftSalesForecastVersionReasonRepository;
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
import java.util.stream.Collectors;

@Service
public class GiftSalesForecastService {

    private static final Logger log = LoggerFactory.getLogger(GiftSalesForecastService.class);
    private static final DateTimeFormatter VERSION_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final GiftSalesForecastRepository forecastRepository;
    private final SalesForecastConfigRepository configRepository;
    private final GiftSalesForecastVersionReasonRepository versionReasonRepository;
    private final ErpProductService erpProductService;
    private final JdbcTemplate jdbcTemplate;

    public GiftSalesForecastService(GiftSalesForecastRepository forecastRepository,
                                    SalesForecastConfigRepository configRepository,
                                    GiftSalesForecastVersionReasonRepository versionReasonRepository,
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
        log.info("Creating gift forecast: user={}, month={}, channel={}, productCode={}",
                userId, request.getMonth(), request.getChannel(), request.getProductCode());

        validateMonthFormat(request.getMonth());
        if (!Boolean.TRUE.equals(authorities != null && authorities.contains("sales_forecast.update_after_closed"))) {
            validateMonthOpen(request.getMonth());
        }
        checkChannelOwnership(userId, request.getChannel(), roleCode);

        if (!erpProductService.validateProduct(request.getProductCode())) {
            throw new IllegalArgumentException("Invalid product_code: " + request.getProductCode());
        }

        if (forecastRepository.findByMonthAndChannelAndProductCode(
                request.getMonth(), request.getChannel(), request.getProductCode()).isPresent()) {
            throw new DuplicateEntryException(
                    "Product " + request.getProductCode() + " already exists for month "
                            + request.getMonth() + " and channel " + request.getChannel());
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(
                request.getMonth(), request.getChannel());
        String version = (versions != null && !versions.isEmpty())
                ? versions.get(0)
                : now.format(VERSION_FORMATTER) + "(" + request.getChannel() + ")";

        GiftSalesForecast forecast = new GiftSalesForecast();
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

        GiftSalesForecast saved = forecastRepository.save(forecast);
        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public ForecastResponse updateForecast(Integer id, UpdateForecastRequest request,
                                           Long userId, String roleCode, Set<String> authorities) {
        GiftSalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));

        if (!Boolean.TRUE.equals(authorities != null && authorities.contains("sales_forecast.update_after_closed"))) {
            validateMonthOpen(forecast.getMonth());
        }
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);

        forecast.setQuantity(request.getQuantity());
        forecast.setIsModified(true);
        forecast.setUpdatedAt(LocalDateTime.now());

        GiftSalesForecast saved = forecastRepository.save(forecast);
        return ForecastResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteForecast(Integer id, Long userId, String roleCode) {
        GiftSalesForecast forecast = forecastRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Forecast not found with id: " + id));
        validateMonthOpen(forecast.getMonth());
        checkChannelOwnership(userId, forecast.getChannel(), roleCode);
        forecastRepository.delete(forecast);
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
        if ("admin".equals(roleCode)) return;
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                Integer.class, userId, channel);
        if (count == null || count == 0) {
            throw new AccessDeniedException("No permission for channel: " + channel);
        }
    }

    @Transactional(readOnly = true)
    public List<ForecastResponse> queryForecasts(String month, String channel, String version,
                                                  Long userId, Set<String> authorities) {
        validateMonthFormat(month);
        checkQueryPermission(userId, channel, authorities);

        List<GiftSalesForecast> forecasts;
        if (version != null && !version.isEmpty()) {
            forecasts = forecastRepository.findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(
                    month, channel, version);
        } else {
            forecasts = forecastRepository.findLatestByMonthAndChannel(month, channel);
        }
        return forecasts.stream().map(ForecastResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VersionInfo> queryVersions(String month, String channel, Long userId, Set<String> authorities) {
        validateMonthFormat(month);
        checkQueryPermission(userId, channel, authorities);

        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);
        List<VersionInfo> versionInfos = new ArrayList<>();
        for (String v : versions) {
            Integer count = forecastRepository.countByMonthAndChannelAndVersion(month, channel, v);
            LocalDateTime timestamp = forecastRepository.findMaxUpdatedAtByMonthAndChannelAndVersion(month, channel, v);
            versionInfos.add(new VersionInfo(v, count, timestamp));
        }
        return versionInfos;
    }

    private void checkQueryPermission(Long userId, String channel, Set<String> authorities) {
        if (authorities.contains("sales_forecast.view")) return;
        if (authorities.contains("sales_forecast.view_own")) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sales_channels_users WHERE user_id = ? AND channel = ?",
                    Integer.class, userId, channel);
            if (count == null || count == 0) {
                throw new AccessDeniedException("No permission for channel: " + channel);
            }
            return;
        }
        throw new AccessDeniedException("Missing required permission: sales_forecast.view or sales_forecast.view_own");
    }

    @Transactional
    public CopyVersionResponse copyLatestToNewVersion(String month, String channel, Long userId, String roleCode) {
        validateMonthFormat(month);
        checkChannelOwnership(userId, channel, roleCode);

        List<GiftSalesForecast> latest = forecastRepository.findLatestByMonthAndChannel(month, channel);
        if (latest.isEmpty()) {
            throw new IllegalArgumentException("No data to copy for month=" + month + ", channel=" + channel);
        }

        LocalDateTime now = LocalDateTime.now();
        String newVersion = now.format(VERSION_FORMATTER) + "(" + channel + ")";

        for (GiftSalesForecast src : latest) {
            GiftSalesForecast copy = new GiftSalesForecast();
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
        return new CopyVersionResponse(newVersion);
    }

    @Transactional
    public void saveVersionReason(String month, String channel, String version, String changeReason) {
        GiftSalesForecastVersionReason entity = versionReasonRepository
                .findByMonthAndChannelAndVersion(month, channel, version)
                .orElseGet(() -> {
                    GiftSalesForecastVersionReason e = new GiftSalesForecastVersionReason();
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

    @Transactional
    public void deleteVersion(String month, String channel, String version, Long userId, String roleCode) {
        validateMonthFormat(month);
        checkChannelOwnership(userId, channel, roleCode);
        forecastRepository.deleteByMonthAndChannelAndVersion(month, channel, version);
        versionReasonRepository.findByMonthAndChannelAndVersion(month, channel, version)
                .ifPresent(versionReasonRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<VersionDiffItemDTO> getVersionDiff(String month, String channel, String version,
                                                   Long userId, Set<String> authorities) {
        validateMonthFormat(month);
        checkQueryPermission(userId, channel, authorities);

        List<String> versions = forecastRepository.findDistinctVersionsByMonthAndChannel(month, channel);
        int idx = versions.indexOf(version);
        if (idx < 0 || idx + 1 >= versions.size()) return List.of();
        String previousVersion = versions.get(idx + 1);

        List<GiftSalesForecast> currentRows = forecastRepository
                .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, version);
        List<GiftSalesForecast> previousRows = forecastRepository
                .findByMonthAndChannelAndVersionOrderByCategoryAscSpecAscProductCodeAsc(month, channel, previousVersion);

        Map<String, BigDecimal> prevQtyByProduct = previousRows.stream()
                .collect(Collectors.toMap(GiftSalesForecast::getProductCode, GiftSalesForecast::getQuantity, (a, b) -> a, LinkedHashMap::new));

        List<VersionDiffItemDTO> result = new ArrayList<>();
        for (GiftSalesForecast row : currentRows) {
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
        public DuplicateEntryException(String message) { super(message); }
    }
}
