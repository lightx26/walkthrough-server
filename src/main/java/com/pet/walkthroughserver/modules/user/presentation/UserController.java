package com.pet.walkthroughserver.modules.user.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.presentation.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.presentation.mapper.UserPresentationMapper;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserPresentationMapper userPresentationMapper;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthUser authUser) {
        UserEntity user = userService.getCurrentUser(UUID.fromString(authUser.getUserId()));
        UserResponse response = userPresentationMapper.toResponse(user);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<UserResponse>>> searchUsers(
            @RequestParam String q) {
        List<UserResponse> users = userService.searchUsers(q).stream()
                .map(userPresentationMapper::toResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(users)));
    }
}
