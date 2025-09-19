package com.newnormallist.userservice.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private boolean success;
    private T data;
    private String errorCode;
    private String message;

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, data, null, null);
    }

    public static <T> ApiResult<T> error(String errorCode, String message) {
        return new ApiResult<>(false, null, errorCode, message);
    }
}
