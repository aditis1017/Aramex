package com.customcharge.persistencelayer.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ApiResponse<D, E extends ExceptionResponse> {

    private boolean status;
    private D data;
    private E exception;
    private String trackingId;

    public ApiResponse() {
    }

    public ApiResponse(Boolean status, D data,E exception, String trackingId) {
        this.status=status;
        this.data=data;
        this.exception=exception;
        this.trackingId=trackingId;
    }

    public static <D, E extends ExceptionResponse> ApiResponse<D, E> success(D data, String trackingId) {
        return new ApiResponse<>(
                true,
                data,
                null,
                trackingId
        );
    }

    public static <D, E extends ExceptionResponse> ApiResponse<D, E> failure(E exception, String trackingId) {
        return new ApiResponse<>(
                true,
                null,
                exception,
                trackingId
        );
    }

}
