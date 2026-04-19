package com.pet.walkthroughserver.modules.profile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalkthroughPinRepository extends JpaRepository<WalkthroughPinEntity, UUID> {

    List<WalkthroughPinEntity> findByUserIdOrderBySortOrderAsc(UUID userId);

    Optional<WalkthroughPinEntity> findByUserIdAndWalkthroughId(UUID userId, UUID walkthroughId);

    long countByUserId(UUID userId);

    void deleteByUserIdAndWalkthroughId(UUID userId, UUID walkthroughId);

    void deleteAllByUserId(UUID userId);
}
