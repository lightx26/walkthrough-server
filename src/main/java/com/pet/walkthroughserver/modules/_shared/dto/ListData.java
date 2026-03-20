package com.pet.walkthroughserver.modules._shared.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class ListData<T> {

    private final List<T> items;

    private ListData(List<T> items) {
        this.items = items;
    }

    public static <T> ListData<T> of(List<T> items) {
        return new ListData<>(items);
    }
}
