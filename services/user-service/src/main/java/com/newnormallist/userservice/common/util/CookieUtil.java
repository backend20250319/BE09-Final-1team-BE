package com.newnormallist.userservice.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

public class CookieUtil {

    /**
     * 요청에서 쿠키를 이름으로 조회합니다.
     * @param request HttpServletRequest
     * @param name 쿠키 이름
     * @return 쿠키 객체를 담은 Optional
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 응답에 쿠키를 추가합니다.
     * @param response HttpServletResponse
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 쿠키 유효 시간 (초)
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 요청과 응답에서 쿠키를 삭제합니다.
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     * @param name 삭제할 쿠키 이름
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 객체를 직렬화하여 Base64 문자열로 인코딩합니다.
     * @param object 직렬화할 객체
     * @return 인코딩된 문자열
     */
    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    /**
     * 쿠키 값을 역직렬화하여 객체로 변환합니다.
     * @param cookie 쿠키 객체
     * @param cls 변환할 클래스 타입
     * @return 역직렬화된 객체
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())
        ));
    }
}