package com.pet.walkthroughserver.modules._shared.infra.messaging;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transport DTO for review-decision sync events sent over the message broker.
 * Carries both submit and dismiss payloads, discriminated by {@code action}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSyncEventMessage {

    /** SUBMIT or DISMISS. */
    private String action;

    private UUID reviewDecisionId;
    private UUID walkthroughId;
    private UUID userId;

    // Dismiss-only payload (the row is gone by the time it is processed).
    private Long githubReviewId;
    private String owner;
    private String repo;
    private Integer prNumber;
}
