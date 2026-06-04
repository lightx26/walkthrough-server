package com.pet.walkthroughserver.modules.riskzone.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskScanRepository extends JpaRepository<RiskScanEntity, UUID> {

    Optional<RiskScanEntity> findTopByWalkthroughIdOrderByCreatedAtDesc(UUID walkthroughId);

    boolean existsByWalkthroughIdAndStatus(UUID walkthroughId, RiskScanStatus status);
}
