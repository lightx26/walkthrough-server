package com.pet.walkthroughserver.modules.riskzone.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RiskZoneRepository extends JpaRepository<RiskZoneEntity, UUID> {

    List<RiskZoneEntity> findByRiskScanId(UUID riskScanId);

    @Modifying
    @Query("UPDATE RiskZoneEntity z SET z.walkthroughFileId = :newFileId WHERE z.id = :zoneId")
    void relinkToFile(@Param("zoneId") UUID zoneId, @Param("newFileId") UUID newFileId);
}
