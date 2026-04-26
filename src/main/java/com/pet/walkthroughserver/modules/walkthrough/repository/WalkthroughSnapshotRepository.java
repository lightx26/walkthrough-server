package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalkthroughSnapshotRepository extends JpaRepository<WalkthroughSnapshotEntity, UUID> {

    Optional<WalkthroughSnapshotEntity> findByWalkthroughIdAndVersion(UUID walkthroughId, Integer version);

    List<WalkthroughSnapshotEntity> findByWalkthroughIdOrderByVersionAsc(UUID walkthroughId);
}
