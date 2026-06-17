package com.pet.walkthroughserver.modules.riskzone.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RiskZoneRepository extends JpaRepository<RiskZoneEntity, UUID> {

    List<RiskZoneEntity> findByRiskScanId(UUID riskScanId);
}
