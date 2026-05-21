package com.pet.walkthroughserver.modules.template.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateChapterRepository extends JpaRepository<TemplateChapterEntity, UUID> {
}
