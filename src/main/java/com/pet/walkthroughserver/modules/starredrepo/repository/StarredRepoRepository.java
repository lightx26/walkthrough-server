package com.pet.walkthroughserver.modules.starredrepo.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StarredRepoRepository extends JpaRepository<StarredRepoEntity, UUID> {

    List<StarredRepoEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<StarredRepoEntity> findByUserIdAndRepoFullName(UUID userId, String repoFullName);

    boolean existsByUserIdAndRepoFullName(UUID userId, String repoFullName);

    void deleteByUserIdAndRepoFullName(UUID userId, String repoFullName);
}
