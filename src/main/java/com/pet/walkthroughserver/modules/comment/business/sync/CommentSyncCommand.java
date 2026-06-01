package com.pet.walkthroughserver.modules.comment.business.sync;

import java.util.UUID;

/**
 * Inbound command for syncing a comment to GitHub.
 * Transport-agnostic — built from whatever message format the listener adapter receives.
 */
public record CommentSyncCommand(
        UUID commentId,
        UUID walkthroughId,
        UUID userId,
        String content,
        UUID walkthroughFileId,
        Integer diffPosition
) {
}
