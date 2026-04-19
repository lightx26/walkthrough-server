package com.pet.walkthroughserver.modules.profile.presentation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProfileStatsResponse {

    private long walkthroughs;
    private long chapters;
    private long views;
    private long comments;
    private long pins;
}
