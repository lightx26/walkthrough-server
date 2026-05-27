package com.pet.walkthroughserver.modules.walkthrough.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChapterViewEventRepository extends JpaRepository<ChapterViewEventEntity, UUID> {
}
