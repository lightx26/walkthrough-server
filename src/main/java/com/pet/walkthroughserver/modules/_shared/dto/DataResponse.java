package com.pet.walkthroughserver.modules._shared.dto;

import lombok.Getter;

@Getter
public class DataResponse<T> extends ApiResponse {

    private final T data;

    private DataResponse(T data, String message) {
        super(true, message);
        this.data = data;
    }

    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data, null);
    }

    public static <T> DataResponse<T> of(String message, T data) {
        return new DataResponse<>(data, message);
    }
}
