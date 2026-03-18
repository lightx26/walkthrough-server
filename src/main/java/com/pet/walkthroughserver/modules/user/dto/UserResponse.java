package com.pet.walkthroughserver.modules.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private Instant createdAt;
}
