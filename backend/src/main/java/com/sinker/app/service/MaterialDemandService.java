package com.sinker.app.service;

import com.sinker.app.dto.materialdemand.MaterialDemandDTO;
import com.sinker.app.entity.MaterialDemand;
import com.sinker.app.repository.MaterialDemandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialDemandService {

    private static final Logger log = LoggerFactory.getLogger(MaterialDemandService.class);

    private final MaterialDemandRepository materialDemandRepository;

    public MaterialDemandService(MaterialDemandRepository materialDemandRepository) {
        this.materialDemandRepository = materialDemandRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialDemandDTO> queryMaterialDemand(LocalDate weekStart, String factory) {
        log.info("Querying material demand for weekStart: {}, factory: {}", weekStart, factory);

        List<MaterialDemand> demands = materialDemandRepository
                .findByWeekStartAndFactoryOrderByMaterialCodeAsc(weekStart, factory);

        return demands.stream()
                .map(MaterialDemandDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
