package com.pet.walkthroughserver.modules.profile.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ProfileResponse {

    private String id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String bio;
    private String githubUrl;
    private Instant joinedAt;
}
