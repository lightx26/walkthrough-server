package com.pet.walkthroughserver.modules.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, UUID> {

    List<SearchHistoryEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
