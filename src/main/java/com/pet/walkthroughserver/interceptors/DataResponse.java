package com.pet.walkthroughserver.interceptors;

import lombok.Getter;

@Getter
public class DataResponse<T> extends ApiResponse {

    private final T data;

    private DataResponse(T data, String message) {
        super(true, message);
        this.data = data;
    }

    public static <T> DataResponse<T> of(T data) {
        return new DataResponse<>(data, "Success");
    }

    public static <T> DataResponse<T> of(T data, String message) {
        return new DataResponse<>(data, message);
    }
}
