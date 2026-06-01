package com.pet.walkthroughserver.modules.comment.business.sync;

/**
 * Port: handles inbound comment-sync commands.
 * No messaging imports — the infra listener adapter converts
 * transport messages into {@link CommentSyncCommand} and calls this.
 */
public interface CommentSyncHandler {

    void handle(CommentSyncCommand command);
}
