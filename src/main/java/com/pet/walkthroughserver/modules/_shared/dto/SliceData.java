package com.pet.walkthroughserver.modules._shared.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class SliceData<T> {

    private final List<T> items;
    private final boolean hasNext;

    private SliceData(List<T> items, boolean hasNext) {
        this.items = items;
        this.hasNext = hasNext;
    }

    public static <T> SliceData<T> of(List<T> items, boolean hasNext) {
        return new SliceData<>(items, hasNext);
    }
}
