package com.pet.walkthroughserver.modules.githubpr.presentation.assembler;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.PullRequestPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GitHubPrAssembler {

    private final PullRequestPresentationMapper pullRequestMapper;
    private final WalkthroughService walkthroughService;

    /**
     * Maps pull requests to responses with walkthrough counts populated.
     */
    public List<PullRequestResponse> toResponseWithCounts(
            List<GitHubPullRequest> prs, String owner, String repo, UUID userId) {
        List<PullRequestResponse> responses = pullRequestMapper.toResponseList(prs);
        List<Integer> prNumbers = responses.stream()
                .map(PullRequestResponse::getNumber)
                .toList();
        Map<Integer, Long> countMap = walkthroughService.countByPrs(owner, repo, prNumbers, userId);
        responses.forEach(pr -> pr.setWalkthroughsCount(
                countMap.getOrDefault(pr.getNumber(), 0L)));
        return responses;
    }
}
