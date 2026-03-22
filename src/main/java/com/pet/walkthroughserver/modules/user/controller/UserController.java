package com.pet.walkthroughserver.modules.user.controller;

import com.pet.walkthroughserver.modules._shared.dto.ApiResponse;
import com.pet.walkthroughserver.modules.user.dto.UserResponse;
import com.pet.walkthroughserver.modules.user.service.UserService;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthUser authUser) {
        UserResponse response = userService.getCurrentUser(UUID.fromString(authUser.getUserId()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
