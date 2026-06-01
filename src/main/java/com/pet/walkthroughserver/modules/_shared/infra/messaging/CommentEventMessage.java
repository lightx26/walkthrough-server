package com.pet.walkthroughserver.modules._shared.infra.messaging;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Transport DTO for comment events sent over the message broker.
 * Wire-compatible with the previous CommentCreatedEvent serialization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentEventMessage {

    private UUID commentId;
    private UUID walkthroughId;
    private UUID userId;
    private String content;
    private UUID walkthroughFileId;
    private Integer diffPosition;
}
