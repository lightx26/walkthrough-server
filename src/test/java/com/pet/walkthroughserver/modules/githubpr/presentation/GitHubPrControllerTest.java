package com.pet.walkthroughserver.modules.githubpr.presentation;

import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubResourceNotFoundException;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.CommitPresentationMapper;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.FileChangePresentationMapper;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.PullRequestPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = GitHubPrController.class, properties = "cors.allowed.origins=http://localhost")
class GitHubPrControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubPrService gitHubPrService;

    @MockitoBean
    private WalkthroughService walkthroughService;

    @MockitoBean
    private PullRequestPresentationMapper pullRequestMapper;

    @MockitoBean
    private CommitPresentationMapper commitMapper;

    @MockitoBean
    private FileChangePresentationMapper fileChangeMapper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CookieService cookieService;

    @Test
    void getPullRequests_success_returnsListData() throws Exception {
        GitHubPullRequest ghPr = new GitHubPullRequest();
        ghPr.setId(10L);
        ghPr.setTitle("Fix bug");

        PullRequestResponse prResponse = PullRequestResponse.builder()
                .id(10L).title("Fix bug").build();

        when(gitHubPrService.getPullRequests(eq(USER_ID), eq("owner"), eq("repo"), eq("open"), eq(1), eq(30)))
                .thenReturn(List.of(ghPr));
        when(pullRequestMapper.toResponseList(anyList())).thenReturn(List.of(prResponse));

        mockMvc.perform(get("/v1/github/repos/owner/repo/pulls")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Fix bug"));
    }

    @Test
    void getPullRequest_success_returnsDataResponse() throws Exception {
        GitHubPullRequest ghPr = new GitHubPullRequest();
        ghPr.setId(10L);
        ghPr.setNumber(42);
        ghPr.setTitle("Add feature");

        PullRequestResponse prResponse = PullRequestResponse.builder()
                .id(10L).number(42).title("Add feature").build();

        when(gitHubPrService.getPullRequest(eq(USER_ID), eq("owner"), eq("repo"), eq(42)))
                .thenReturn(ghPr);
        when(pullRequestMapper.toResponse(any(GitHubPullRequest.class))).thenReturn(prResponse);

        mockMvc.perform(get("/v1/github/repos/owner/repo/pulls/42")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Add feature"))
                .andExpect(jsonPath("$.data.number").value(42));
    }

    @Test
    void getPullRequest_notFound_returns404() throws Exception {
        when(gitHubPrService.getPullRequest(any(), anyString(), anyString(), anyInt()))
                .thenThrow(new GitHubResourceNotFoundException("Pull request not found"));

        mockMvc.perform(get("/v1/github/repos/owner/repo/pulls/999")
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_RESOURCE_NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken authToken() {
        AuthUser authUser = AuthUser.builder()
                .userId(USER_ID.toString())
                .username("testuser")
                .build();
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }
}
