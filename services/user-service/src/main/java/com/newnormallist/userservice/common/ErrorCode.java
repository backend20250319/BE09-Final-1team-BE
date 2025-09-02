package com.newnormallist.userservice.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // User 관련 에러
    EMAIL_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "U001", "이미 존재하는 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U002", "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 올바르지 않습니다."),
    CURRENT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "U004", "현재 비밀번호를 입력해주세요."),
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U005", "현재 비밀번호가 일치하지 않습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "U006", "유효하지 않은 카테고리입니다."),

    // Auth 관련 에러
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A003", "리프레시 토큰을 찾을 수 없습니다."),
    INVALID_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 비밀번호 재설정 토큰입니다."),
    EXPIRED_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "토큰이 만료되었습니다."),
    PASSWORD_CONTAINS_NAME(HttpStatus.BAD_REQUEST, "A006", "비밀번호에 이름이 포함되어 있습니다."),
    PASSWORD_CONTAINS_EMAIL(HttpStatus.BAD_REQUEST, "A007", "비밀번호에 이메일이 포함되어 있습니다."),

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "C003", "비밀번호가 일치하지 않습니다."),
    INVALID_STATUS(HttpStatus.CONFLICT, "C004", "유효하지 않은 상태입니다."),
    OPERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "작업을 수행할 수 없습니다."),
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "뉴스 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
