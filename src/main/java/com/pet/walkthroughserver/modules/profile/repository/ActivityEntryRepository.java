package com.pet.walkthroughserver.modules.profile.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ActivityEntryRepository extends JpaRepository<ActivityEntryEntity, UUID> {

    @Query("SELECT a FROM ActivityEntryEntity a WHERE a.userId = :userId " +
            "AND a.occurredAt < :before ORDER BY a.occurredAt DESC")
    Slice<ActivityEntryEntity> findByUserIdBeforeTime(
            @Param("userId") UUID userId,
            @Param("before") Instant before,
            Pageable pageable);

    @Query("SELECT a FROM ActivityEntryEntity a WHERE a.userId = :userId " +
            "AND a.visibility = 'PUBLIC' AND a.occurredAt < :before ORDER BY a.occurredAt DESC")
    Slice<ActivityEntryEntity> findPublicByUserIdBeforeTime(
            @Param("userId") UUID userId,
            @Param("before") Instant before,
            Pageable pageable);
}
