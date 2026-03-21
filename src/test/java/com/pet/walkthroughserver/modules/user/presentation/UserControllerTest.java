package com.pet.walkthroughserver.modules.user.presentation;

import com.pet.walkthroughserver.modules._shared.infra.cookie.CookieService;
import com.pet.walkthroughserver.modules._shared.infra.jwt.TokenService;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.exceptions.UserNotFoundException;
import com.pet.walkthroughserver.modules.user.presentation.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.presentation.mapper.UserPresentationMapper;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.security.AuthUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, properties = "cors.allowed.origins=http://localhost")
class UserControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserPresentationMapper userPresentationMapper;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CookieService cookieService;

    @Test
    void getCurrentUser_success_returnsDataResponse() throws Exception {
        UserEntity entity = UserEntity.builder()
                .id(USER_ID).githubId(1L).username("testuser")
                .displayName("Test").createdAt(Instant.now()).build();
        UserResponse response = UserResponse.builder()
                .id(USER_ID).username("testuser").displayName("Test")
                .createdAt(Instant.now()).build();

        when(userService.getCurrentUser(USER_ID)).thenReturn(entity);
        when(userPresentationMapper.toResponse(any(UserEntity.class))).thenReturn(response);

        mockMvc.perform(get("/v1/users/me")
                        .with(authentication(authToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()));
    }

    @Test
    void getCurrentUser_notFound_returns404() throws Exception {
        when(userService.getCurrentUser(USER_ID))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/v1/users/me")
                        .with(authentication(authToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    private UsernamePasswordAuthenticationToken authToken() {
        AuthUser authUser = AuthUser.builder()
                .userId(USER_ID.toString())
                .username("testuser")
                .build();
        return new UsernamePasswordAuthenticationToken(authUser, null, List.of());
    }
}
