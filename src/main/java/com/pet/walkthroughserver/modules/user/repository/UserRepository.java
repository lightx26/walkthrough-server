package com.pet.walkthroughserver.modules.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByGithubId(Long githubId);

    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findTop10ByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
            String username, String displayName);
}
