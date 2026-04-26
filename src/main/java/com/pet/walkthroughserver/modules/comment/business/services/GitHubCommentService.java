package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.UUID;

public interface GitHubCommentService {

    Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body);

    Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                String body, String commitId, String path, int position);
}
