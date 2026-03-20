package com.pet.walkthroughserver.modules._shared.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PageData<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PageData(List<T> items, int page, int size, long totalElements, int totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PageData<T> of(List<T> items, int page, int size, long totalElements, int totalPages) {
        return new PageData<>(items, page, size, totalElements, totalPages);
    }
}
