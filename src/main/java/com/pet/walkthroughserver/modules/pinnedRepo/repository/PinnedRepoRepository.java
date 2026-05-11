package com.pet.walkthroughserver.modules.pinnedRepo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PinnedRepoRepository extends JpaRepository<PinnedRepoEntity, UUID> {

    List<PinnedRepoEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<PinnedRepoEntity> findByUserIdAndRepoFullName(UUID userId, String repoFullName);

    boolean existsByUserIdAndRepoFullName(UUID userId, String repoFullName);

    List<PinnedRepoEntity> findByUserIdAndRepoFullNameIn(UUID userId, List<String> repoFullNames);

    void deleteByUserIdAndRepoFullName(UUID userId, String repoFullName);
}
