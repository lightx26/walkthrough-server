package com.pet.walkthroughserver.modules.search.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules.search.business.services.WalkthroughReindexService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final WalkthroughReindexService reindexService;

    @PostMapping("/reindex")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<String>> reindex() {
        int count = reindexService.reindexAll();
        return ResponseEntity.ok(DataResponse.of("Reindexed " + count + " walkthroughs"));
    }
}
