package com.pet.walkthroughserver.modules.comment.business.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreatedEvent implements Serializable {

    private UUID commentId;
    private UUID walkthroughId;
    private UUID userId;
    private String content;
}
