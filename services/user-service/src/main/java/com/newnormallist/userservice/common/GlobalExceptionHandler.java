package com.newnormallist.userservice.common;

import com.newnormallist.userservice.common.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResult<String>> handleUserException(UserException e) {
        log.error("UserException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResult.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResult<String>> handleValidationException(Exception e) {
        log.error("Validation Exception: {}", e.getMessage());

        BindingResult bindingResult = null;
        
        // MethodArgumentNotValidException과 BindException 모두 처리
        if (e instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) e).getBindingResult();
        } else if (e instanceof BindException) {
            bindingResult = ((BindException) e).getBindingResult();
        }

        // 2. 여러 에러 중 첫 번째 에러 메시지를 가져옴
        String errorMessage = "유효성 검증에 실패했습니다."; // 기본 메시지
        if (bindingResult != null && bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError(); // 첫 번째 오류 가져오기
            if (fieldError != null) {
                errorMessage = fieldError.getDefaultMessage(); // DTO에 정의된 validation 오류 메시지
            }
        }
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResult.error(ErrorCode.INVALID_INPUT_VALUE.getCode(),
                        errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<String>> handleException(Exception e) {
        log.error("Unexpected Exception: ", e);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResult.error(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
