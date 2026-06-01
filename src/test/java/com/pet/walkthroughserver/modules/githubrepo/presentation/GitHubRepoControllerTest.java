package com.pet.walkthroughserver.modules.githubrepo.presentation;

import com.pet.walkthroughserver.modules._shared.dto.PageData;
import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubApiException;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.githubrepo.business.services.GitHubRepoService;
import com.pet.walkthroughserver.modules.githubrepo.presentation.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.githubrepo.presentation.mapper.RepositoryPresentationMapper;
import com.pet.walkthroughserver.modules.pinnedRepo.business.services.PinnedRepoService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = GitHubRepoController.class, properties = "cors.allowed.origins=http://localhost")
class GitHubRepoControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubRepoService gitHubRepoService;

    @MockitoBean
    private RepositoryPresentationMapper repositoryMapper;

    @MockitoBean
    private WalkthroughService walkthroughService;

    @MockitoBean
    private PinnedRepoService pinnedRepoService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CookieService cookieService;

    @Test
    void getUserRepositories_success_returnsPageData() throws Exception {
        GitHubRepository ghRepo = new GitHubRepository();
        ghRepo.setId(1L);
        ghRepo.setName("my-repo");
        ghRepo.setFullName("testuser/my-repo");

        RepositoryResponse repoResponse = RepositoryResponse.builder()
                .id(1L).name("my-repo").fullName("testuser/my-repo").build();

        when(gitHubRepoService.getUserRepositories(eq(USER_ID), anyInt(), anyInt(), eq("updated")))
                .thenReturn(PageData.of(List.of(ghRepo), 1, 20, 1L, 1));
        when(repositoryMapper.toResponse(any(GitHubRepository.class))).thenReturn(repoResponse);
        when(walkthroughService.countByRepos(anyList(), eq(USER_ID))).thenReturn(Map.of());
        when(pinnedRepoService.findPinnedFullNames(eq(USER_ID), anyList())).thenReturn(Set.of());

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].name").value("my-repo"));
    }

    @Test
    void searchRepositories_withQueryParam_returnsPageData() throws Exception {
        GitHubRepository ghRepo = new GitHubRepository();
        ghRepo.setId(2L);
        ghRepo.setName("search-result");
        ghRepo.setFullName("testuser/search-result");

        RepositoryResponse repoResponse = RepositoryResponse.builder()
                .id(2L).name("search-result").fullName("testuser/search-result").build();

        when(gitHubRepoService.searchRepositories(eq(USER_ID), eq("test"), anyInt(), anyInt()))
                .thenReturn(PageData.of(List.of(ghRepo), 1, 20, 1L, 1));
        when(repositoryMapper.toResponse(any(GitHubRepository.class))).thenReturn(repoResponse);
        when(walkthroughService.countByRepos(anyList(), eq(USER_ID))).thenReturn(Map.of());
        when(pinnedRepoService.findPinnedFullNames(eq(USER_ID), anyList())).thenReturn(Set.of());

        mockMvc.perform(get("/v1/github/repos")
                        .param("q", "test")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("search-result"));
    }

    @Test
    void getUserRepositories_accessTokenNotFound_returns401() throws Exception {
        when(gitHubRepoService.getUserRepositories(any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new GitHubAccessTokenNotFoundException("GitHub access token not found"));

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_ACCESS_TOKEN_NOT_FOUND"));
    }

    @Test
    void getUserRepositories_gitHubApiError_returns502() throws Exception {
        when(gitHubRepoService.getUserRepositories(any(), anyInt(), anyInt(), anyString()))
                .thenThrow(new GitHubApiException("Failed to fetch repositories from GitHub"));

        mockMvc.perform(get("/v1/github/repos")
                        .with(authentication(authToken())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("GITHUB_API_ERROR"));
    }

    private UsernamePasswordAuthenticationToken authToken() {
        AuthUser authUser = AuthUser.builder()
                .userId(USER_ID.toString())
                .username("testuser")
                .build();
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }
}
