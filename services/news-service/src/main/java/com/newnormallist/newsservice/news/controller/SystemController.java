package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "System", description = "시스템 상태/버전")
@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 헬스 체크 API
     */
    @Operation(
        summary = "헬스 체크",
        description = "뉴스 서비스의 상태를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "서비스 정상 동작")
    })
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("News Service is running");
    }

    /**
     * 데이터베이스 연결 테스트 API
     */
    @Operation(
        summary = "데이터베이스 연결 테스트",
        description = "데이터베이스 연결 상태와 뉴스 개수를 확인합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "데이터베이스 연결 성공"),
        @ApiResponse(responseCode = "500", description = "데이터베이스 연결 실패")
    })
    @GetMapping("/test-db")
    public ResponseEntity<String> databaseTest() {
        try {
            long count = newsService.getNewsCount();
            return ResponseEntity.ok("데이터베이스 연결 성공. 뉴스 개수: " + count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("데이터베이스 연결 실패: " + e.getMessage());
        }
    }
}
