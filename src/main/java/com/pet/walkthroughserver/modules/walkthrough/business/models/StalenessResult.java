package com.pet.walkthroughserver.modules.walkthrough.business.models;

public record StalenessResult(
        boolean stale,
        String currentCommitSha,
        String latestCommitSha,
        int currentVersion
) {}
