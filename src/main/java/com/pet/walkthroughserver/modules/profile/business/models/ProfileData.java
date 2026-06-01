package com.pet.walkthroughserver.modules.profile.business.models;

import java.time.Instant;
import java.util.UUID;

public record ProfileData(
        UUID id,
        String username,
        String displayName,
        String email,
        String avatarUrl,
        String bio,
        String githubUrl,
        Instant joinedAt
) {}
