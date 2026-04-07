package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadProgressRepository extends JpaRepository<ReadProgressEntity, UUID> {

    Optional<ReadProgressEntity> findByUserIdAndWalkthroughId(UUID userId, UUID walkthroughId);

    List<ReadProgressEntity> findTop10ByUserIdOrderByReadAtDesc(UUID userId);
}
