package com.pet.walkthroughserver.modules.walkthrough.business.models;

import java.time.Instant;

/**
 * Time-bounded activity counters for a single user, used by the home "This week" panel.
 *
 * @param walkthroughCount walkthroughs the user created on/after {@code since}
 * @param commentCount     comments posted on/after {@code since} on the user's walkthroughs
 * @param since            inclusive lower bound of the reporting window
 */
public record ActivitySummary(long walkthroughCount, long commentCount, Instant since) {
}
