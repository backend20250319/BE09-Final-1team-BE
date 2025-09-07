package com.newnormallist.userservice.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 사용자 ID 검증을 위한 커스텀 어노테이션
 * 이 어노테이션이 붙은 메서드는 자동으로 인증 상태를 검증합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidatedUserId {
}
