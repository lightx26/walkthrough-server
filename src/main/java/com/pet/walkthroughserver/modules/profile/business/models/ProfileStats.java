package com.pet.walkthroughserver.modules.profile.business.models;

public record ProfileStats(
        long walkthroughs,
        long chapters,
        long views,
        long comments,
        long pins,
        long reviews
) {}
