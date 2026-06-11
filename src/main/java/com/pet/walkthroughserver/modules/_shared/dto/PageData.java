package com.pet.walkthroughserver.modules._shared.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class PageData<T> {

    private final List<T> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    @JsonCreator
    private PageData(@JsonProperty("items") List<T> items,
                     @JsonProperty("page") int page,
                     @JsonProperty("size") int size,
                     @JsonProperty("totalElements") long totalElements,
                     @JsonProperty("totalPages") int totalPages) {
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
