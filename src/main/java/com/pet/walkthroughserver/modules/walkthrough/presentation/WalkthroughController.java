package com.pet.walkthroughserver.modules.walkthrough.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.WalkthroughPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/walkthroughs")
@RequiredArgsConstructor
public class WalkthroughController {

    private final WalkthroughService walkthroughService;
    private final WalkthroughPresentationMapper walkthroughMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateWalkthroughRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.create(userId, request);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<WalkthroughSummaryResponse>>> list(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Integer prNumber) {
        List<WalkthroughEntity> entities = walkthroughService.listByPr(owner, repo, prNumber);
        List<WalkthroughSummaryResponse> summaries = walkthroughMapper.toSummaryResponseList(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(summaries)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> getById(@PathVariable UUID id) {
        WalkthroughEntity entity = walkthroughService.getById(id);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWalkthroughRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.update(userId, id, request);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        walkthroughService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
