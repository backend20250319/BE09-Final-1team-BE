package com.newnormallist.userservice.common;

import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseEntity;

/**
 * 인증 관련 유틸리티 클래스
 */
@UtilityClass
public class AuthUtils {

    private static final String ANONYMOUS_USER = "anonymousUser";

    /**
     * 사용자 ID 문자열을 Long으로 변환하고 인증 상태를 검증합니다.
     * 
     * @param userIdStr 사용자 ID 문자열
     * @return 변환된 사용자 ID
     * @throws IllegalArgumentException 인증되지 않은 사용자인 경우
     */
    public static Long parseUserId(String userIdStr) {
        if (userIdStr == null || ANONYMOUS_USER.equals(userIdStr)) {
            throw new IllegalArgumentException("인증이 필요합니다.");
        }
        return Long.parseLong(userIdStr);
    }

    /**
     * 인증되지 않은 사용자에 대한 401 응답을 생성합니다.
     * 
     * @param <T> 응답 데이터 타입
     * @return 401 Unauthorized 응답
     */
    public static <T> ResponseEntity<ApiResult<T>> unauthorizedResponse() {
        return ResponseEntity.status(401).body(ApiResult.error("UNAUTHORIZED", "인증이 필요합니다."));
    }

    /**
     * 사용자 ID 문자열이 유효한지 검증합니다.
     * 
     * @param userIdStr 사용자 ID 문자열
     * @return 유효한 사용자 ID인지 여부
     */
    public static boolean isValidUserId(String userIdStr) {
        return userIdStr != null && !ANONYMOUS_USER.equals(userIdStr);
    }
}
