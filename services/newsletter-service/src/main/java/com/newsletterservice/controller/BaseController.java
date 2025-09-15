package com.newsletterservice.controller;

import com.newsletterservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

@RequiredArgsConstructor
public abstract class BaseController {

    @Autowired
    protected JwtUtil jwtUtil;
    
    // protected logger for subclasses
    protected final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

    /**
     * JWT 토큰에서 사용자 ID 추출 (개선된 버전)
     */
    protected Long extractUserIdFromToken(HttpServletRequest request) {
        try {
            // Authorization 헤더에서 토큰 확인
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.debug("Authorization 헤더에서 토큰 발견, 길이: {}", token.length());
                String userId = jwtUtil.extractUserIdSafely(token);
                if (userId != null) {
                    return Long.valueOf(userId);
                }
            } else {
                log.debug("Authorization 헤더가 없거나 Bearer 형식이 아닙니다: {}", authHeader);
            }
            
            // 쿠키에서도 토큰 확인
            if (request.getCookies() != null) {
                for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                    if ("access-token".equals(cookie.getName())) {
                        log.debug("쿠키에서 토큰 발견, 길이: {}", cookie.getValue().length());
                        String userId = jwtUtil.extractUserIdSafely(cookie.getValue());
                        if (userId != null) {
                            return Long.valueOf(userId);
                        }
                    }
                }
            } else {
                log.debug("쿠키가 없습니다.");
            }
            
            log.warn("유효한 토큰을 찾을 수 없습니다. Authorization 헤더: {}, 쿠키 개수: {}", 
                    authHeader != null ? "있음" : "없음", 
                    request.getCookies() != null ? request.getCookies().length : 0);
            return 1L; // 기본값 (개발용)
            
        } catch (Exception e) {
            log.error("사용자 ID 추출 중 오류 발생", e);
            return 1L; // 기본값 (개발용)
        }
    }

    /**
     * ID 형식 검증 및 파싱
     */
    protected Long validateAndParseId(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            return null;
        }
        
        if (idString.contains("{") || idString.contains("}")) {
            log.warn("템플릿 문자열이 ID로 전달됨: {}", idString);
            return null;
        }
        
        try {
            return Long.parseLong(idString.trim());
        } catch (NumberFormatException e) {
            log.warn("잘못된 ID 형식: {}", idString);
            return null;
        }
    }

    /**
     * 에러 HTML 생성
     */
    protected String generateErrorHtml(String title, String message, String suggestion) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                           max-width: 600px; margin: 50px auto; padding: 20px; 
                           text-align: center; background-color: #f5f5f5; }
                    .error-container { background: white; padding: 40px; border-radius: 10px; 
                                      box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .error-icon { font-size: 48px; color: #e74c3c; margin-bottom: 20px; }
                    .error-title { color: #e74c3c; font-size: 24px; margin-bottom: 10px; }
                    .error-message { color: #666; margin-bottom: 20px; line-height: 1.6; }
                    .suggestion { background: #e3f2fd; padding: 15px; border-radius: 5px; 
                                 color: #1976d2; margin-bottom: 20px; }
                    .back-button { display: inline-block; background: #2196f3; color: white; 
                                  padding: 10px 20px; text-decoration: none; border-radius: 5px; 
                                  margin-top: 10px; }
                    .back-button:hover { background: #1976d2; }
                </style>
            </head>
            <body>
                <div class="error-container">
                    <div class="error-icon">⚠️</div>
                    <h1 class="error-title">%s</h1>
                    <p class="error-message">%s</p>
                    <div class="suggestion">💡 %s</div>
                    <a href="javascript:history.back()" class="back-button">뒤로 가기</a>
                </div>
            </body>
            </html>
            """, title, title, message, suggestion);
    }
}
