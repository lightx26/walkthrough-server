package com.pet.walkthroughserver.modules.template.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.template.business.services.TemplateService;
import com.pet.walkthroughserver.modules.template.presentation.dto.CreateTemplateRequest;
import com.pet.walkthroughserver.modules.template.presentation.dto.DuplicateTemplateRequest;
import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateResponse;
import com.pet.walkthroughserver.modules.template.presentation.dto.TemplateSummaryResponse;
import com.pet.walkthroughserver.modules.template.presentation.dto.UpdateTemplateRequest;
import com.pet.walkthroughserver.modules.template.presentation.mapper.TemplatePresentationMapper;
import com.pet.walkthroughserver.modules.template.repository.TemplateEntity;
import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final TemplatePresentationMapper templateMapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<TemplateResponse>>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) TemplatePrType prType) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<TemplateEntity> entities = templateService.listVisible(userId, prType);
        List<TemplateResponse> responses = templateMapper.toResponseList(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<TemplateResponse>> getById(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        TemplateEntity entity = templateService.getById(id, userId);
        return ResponseEntity.ok(DataResponse.of(templateMapper.toResponse(entity)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<TemplateResponse>> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateTemplateRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        TemplateEntity entity = templateService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataResponse.of(templateMapper.toResponse(entity)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<TemplateResponse>> update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        TemplateEntity entity = templateService.update(id, userId, request);
        return ResponseEntity.ok(DataResponse.of(templateMapper.toResponse(entity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        templateService.delete(id, userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/stats/top-duplicated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<TemplateSummaryResponse>>> topDuplicated(
            @RequestParam(defaultValue = "5") int limit) {
        List<TemplateEntity> entities = templateService.topDuplicatedBuiltins(limit);
        List<TemplateSummaryResponse> responses = entities.stream()
                .map(templateMapper::toSummaryResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @PostMapping("/{id}/duplicate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<TemplateResponse>> duplicate(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id,
            @RequestBody(required = false) DuplicateTemplateRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        String overrideName = request != null ? request.getName() : null;
        TemplateEntity entity = templateService.duplicate(id, userId, overrideName);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataResponse.of(templateMapper.toResponse(entity)));
    }
}
