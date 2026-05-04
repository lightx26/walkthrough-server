package com.pet.walkthroughserver.modules._shared.infra.github.dto;

import java.util.List;

public record GitHubPagedResult<T>(List<T> items, int page, int totalPages, long totalElements) {

    public static <T> GitHubPagedResult<T> of(List<T> items, int page, int totalPages, long totalElements) {
        return new GitHubPagedResult<>(items, page, totalPages, totalElements);
    }
}
