package com.pet.walkthroughserver.modules.walkthrough.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalkthroughFileRepository extends JpaRepository<WalkthroughFileEntity, UUID> {
}
