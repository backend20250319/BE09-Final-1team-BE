package com.newnormallist.newsservice.summarizer.exception;


import com.newnormallist.newsservice.summarizer.controller.NewsSummaryController;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 요약( Summarizer ) 엔드포인트에만 적용되는 예외 핸들러.
 * - 글로벌로 퍼지지 않도록 assignableTypes 로 대상 컨트롤러를 한정한다.
 * - 응답은 ApiError JSON 으로 통일.
 */
@RestControllerAdvice(assignableTypes = {
        NewsSummaryController.class   // ← 요약 컨트롤러(들)를 나열. 여러 개면 콤마로 추가
})
public class SummarizerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SummarizerExceptionHandler.class);

    /* 404: 서비스 내부에서 명시적으로 던진 NotFound */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e, req, false);
    }

    /* 400: 바디 파싱/검증/파라미터 타입 오류 */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> badRequest(Exception e, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e, req, false);
    }

    /* 502: Flask 호출/응답 문제 등 요약 파이프라인 실패 */
    @ExceptionHandler(SummarizerException.class)
    public ResponseEntity<ApiError> summarizer(SummarizerException e, HttpServletRequest req) {
        return build(HttpStatus.BAD_GATEWAY, "SUMMARIZER_ERROR", e, req, true);
    }

    /* 500: 그 외 요약 컨트롤러 내 일반 오류 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> generic(Exception e, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e, req, true);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, Exception e,
                                           HttpServletRequest req, boolean logStack) {
        // MDC에는 getOrDefault가 없습니다. 직접 기본값 처리하세요.
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) traceId = "-";

        String path = req.getRequestURI();

        if (logStack) {
            log.error("[{}] {} {} - {}", traceId, status.value(), code, e.getMessage(), e);
        } else {
            log.warn ("[{}] {} {} - {}", traceId, status.value(), code, e.toString());
        }

        ApiError body = ApiError.of(status.value(), code, e.getMessage(), path, traceId);
        return ResponseEntity.status(status).body(body);
    }

}
