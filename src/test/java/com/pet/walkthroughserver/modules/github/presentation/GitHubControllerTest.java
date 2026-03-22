package com.pet.walkthroughserver.modules.github.presentation;

import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubApiException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubResourceNotFoundException;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.github.business.services.GitHubService;
import com.pet.walkthroughserver.modules.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.github.presentation.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.github.presentation.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.github.presentation.mapper.PullRequestPresentationMapper;
import com.pet.walkthroughserver.modules.github.presentation.mapper.RepositoryPresentationMapper;
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

@WebMvcTest(value = GitHubController.class, properties = "cors.allowed.origins=http://localhost")
class GitHubControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubService gitHubService;

    @MockitoBean
    private RepositoryPresentationMapper repositoryMapper;

    @MockitoBean
    private PullRequestPresentationMapper pullRequestMapper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CookieService cookieService;

    // ── GET /v1/github/repos ──────────────────────────────────────────

    @Test
    void getUserRepositories_success_returnsListData() throws Exception {
        GitHubRepository ghRepo = new GitHubRepository();
        ghRepo.setId(1L);
        ghRepo.setName("my-repo");

        RepositoryResponse repoResponse = RepositoryResponse.builder()
                .id(1L).name("my-repo").build();

        when(gitHubService.getUserRepositories(eq(USER_ID), eq(1), eq(30), eq("updated")))
                .thenReturn(List.of(ghRepo));
        when(repositoryMapper.toResponseList(anyList())).thenReturn(List.of(repoResponse));

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].name").value("my-repo"));
    }

    @Test
    void searchRepositories_withQueryParam_returnsListData() throws Exception {
        GitHubRepository ghRepo = new GitHubRepository();
        ghRepo.setId(2L);
        ghRepo.setName("search-result");

        RepositoryResponse repoResponse = RepositoryResponse.builder()
                .id(2L).name("search-result").build();

        when(gitHubService.searchRepositories(eq(USER_ID), eq("test"), eq(1), eq(30)))
                .thenReturn(List.of(ghRepo));
        when(repositoryMapper.toResponseList(anyList())).thenReturn(List.of(repoResponse));

        mockMvc.perform(get("/v1/github/repos")
                        .param("q", "test")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("search-result"));
    }

    @Test
    void getUserRepositories_accessTokenNotFound_returns401() throws Exception {
        when(gitHubService.getUserRepositories(any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new GitHubAccessTokenNotFoundException("GitHub access token not found"));

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_ACCESS_TOKEN_NOT_FOUND"));
    }

    @Test
    void getUserRepositories_gitHubApiError_returns502() throws Exception {
        when(gitHubService.getUserRepositories(any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new GitHubApiException("Failed to fetch repositories from GitHub"));

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_API_ERROR"));
    }

    // ── GET /v1/github/repos/{owner}/{repo}/pulls ─────────────────────

    @Test
    void getPullRequests_success_returnsListData() throws Exception {
        GitHubPullRequest ghPr = new GitHubPullRequest();
        ghPr.setId(10L);
        ghPr.setTitle("Fix bug");

        PullRequestResponse prResponse = PullRequestResponse.builder()
                .id(10L).title("Fix bug").build();

        when(gitHubService.getPullRequests(eq(USER_ID), eq("owner"), eq("repo"), eq("open"), eq(1), eq(30)))
                .thenReturn(List.of(ghPr));
        when(pullRequestMapper.toResponseList(anyList())).thenReturn(List.of(prResponse));

        mockMvc.perform(get("/v1/github/repos/owner/repo/pulls")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("Fix bug"));
    }

    // ── GET /v1/github/repos/{owner}/{repo}/pulls/{pullNumber} ────────

    @Test
    void getPullRequest_success_returnsDataResponse() throws Exception {
        GitHubPullRequest ghPr = new GitHubPullRequest();
        ghPr.setId(10L);
        ghPr.setNumber(42);
        ghPr.setTitle("Add feature");

        PullRequestResponse prResponse = PullRequestResponse.builder()
                .id(10L).number(42).title("Add feature").build();

        when(gitHubService.getPullRequest(eq(USER_ID), eq("owner"), eq("repo"), eq(42)))
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
        when(gitHubService.getPullRequest(any(), anyString(), anyString(), anyInt()))
                .thenThrow(new GitHubResourceNotFoundException("Pull request not found"));

        mockMvc.perform(get("/v1/github/repos/owner/repo/pulls/999")
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_RESOURCE_NOT_FOUND"));
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private UsernamePasswordAuthenticationToken authToken() {
        AuthUser authUser = AuthUser.builder()
                .userId(USER_ID.toString())
                .username("testuser")
                .build();
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }
}
