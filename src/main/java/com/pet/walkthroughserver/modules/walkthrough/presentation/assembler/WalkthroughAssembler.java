package com.pet.walkthroughserver.modules.walkthrough.presentation.assembler;

import com.pet.walkthroughserver.modules.walkthrough.business.services.ReadProgressService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ReadProgressResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.ReadProgressPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.WalkthroughPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalkthroughAssembler {

    private final WalkthroughPresentationMapper walkthroughMapper;
    private final ReadProgressPresentationMapper readProgressMapper;
    private final WalkthroughService walkthroughService;
    private final ReadProgressService readProgressService;

    /**
     * Maps walkthrough entities to summary responses with comment counts populated.
     */
    public List<WalkthroughSummaryResponse> toSummaryWithComments(List<WalkthroughEntity> entities) {
        List<WalkthroughSummaryResponse> summaries = walkthroughMapper.toSummaryResponseList(entities);
        List<UUID> ids = entities.stream().map(WalkthroughEntity::getId).toList();
        Map<UUID, Long> commentCounts = walkthroughService.getCommentCounts(ids);
        summaries.forEach(s -> s.setCommentCount(commentCounts.getOrDefault(s.getId(), 0L).intValue()));
        return summaries;
    }

    /**
     * Maps a read-progress entity to a response with read chapter IDs populated.
     */
    public ReadProgressResponse toProgressResponse(UUID userId, UUID walkthroughId, ReadProgressEntity entity) {
        ReadProgressResponse response = readProgressMapper.toResponse(entity);
        response.setReadChapterIds(readProgressService.getReadChapterIds(userId, walkthroughId));
        return response;
    }
}
