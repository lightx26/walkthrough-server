package com.pet.walkthroughserver.modules.walkthrough.business.events;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules.github.business.services.GitHubService;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughCommentRepository;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSyncConsumer {

    private final WalkthroughCommentRepository commentRepository;
    private final WalkthroughRepository walkthroughRepository;
    private final GitHubService gitHubService;
    private final UserService userService;

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    public void handleCommentCreated(CommentCreatedEvent event) {
        log.info("Received CommentCreatedEvent for comment {}", event.getCommentId());

        Optional<WalkthroughCommentEntity> commentOpt = commentRepository.findById(event.getCommentId());
        if (commentOpt.isEmpty()) {
            log.warn("Comment {} not found, skipping sync", event.getCommentId());
            return;
        }

        WalkthroughCommentEntity comment = commentOpt.get();

        Optional<WalkthroughEntity> walkthroughOpt = walkthroughRepository.findById(event.getWalkthroughId());
        if (walkthroughOpt.isEmpty()) {
            log.warn("Walkthrough {} not found, marking comment {} as failed",
                    event.getWalkthroughId(), event.getCommentId());
            comment.setSyncStatus("failed");
            commentRepository.save(comment);
            return;
        }

        WalkthroughEntity walkthrough = walkthroughOpt.get();

        try {
            UserEntity user = userService.findById(event.getUserId());
            String body = String.format("**[Walkthrough: %s]** %s commented:\n\n%s",
                    walkthrough.getTitle(), user.getUsername(), event.getContent());

            Long githubCommentId = gitHubService.createPrComment(
                    event.getUserId(),
                    walkthrough.getOwner(),
                    walkthrough.getRepo(),
                    walkthrough.getPrNumber(),
                    body
            );

            comment.setGithubCommentId(githubCommentId);
            comment.setSyncStatus("synced");
            log.info("Comment {} synced to GitHub as comment {}", event.getCommentId(), githubCommentId);
        } catch (Exception e) {
            log.error("Failed to sync comment {} to GitHub: {}", event.getCommentId(), e.getMessage());
            comment.setSyncStatus("failed");
        }

        commentRepository.save(comment);
    }
}
