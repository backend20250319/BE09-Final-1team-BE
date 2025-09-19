package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.KeywordSubscriptionDto;
import com.newnormallist.newsservice.news.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Keyword Subscriptions", description = "키워드 구독 관리")
@RestController
@RequestMapping("/api/keywords")
@CrossOrigin(origins = "*")
public class KeywordSubscriptionController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 키워드 구독
     */
    @Operation(
        summary = "키워드 구독",
        description = "사용자가 특정 키워드를 구독합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "키워드 구독 성공"),
        @ApiResponse(responseCode = "400", description = "구독 실패")
    })
    @PostMapping("/subscribe")
    public ResponseEntity<KeywordSubscriptionDto> subscribeKeyword(
            @Parameter(name = "userId", description = "사용자 ID", example = "123") 
            @RequestParam Long userId,
            @Parameter(name = "keyword", description = "구독할 키워드", example = "인공지능") 
            @RequestParam String keyword) {
        try {
            KeywordSubscriptionDto subscription = newsService.subscribeKeyword(userId, keyword);
            return ResponseEntity.ok(subscription);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 키워드 구독 해제
     */
    @Operation(
        summary = "키워드 구독 해제",
        description = "사용자의 키워드 구독을 해제합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "키워드 구독 해제 성공"),
        @ApiResponse(responseCode = "400", description = "구독 해제 실패")
    })
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeKeyword(
            @Parameter(name = "userId", description = "사용자 ID", example = "123") 
            @RequestParam Long userId,
            @Parameter(name = "keyword", description = "해제할 키워드", example = "인공지능") 
            @RequestParam String keyword) {
        try {
            newsService.unsubscribeKeyword(userId, keyword);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 사용자의 키워드 구독 목록 조회
     */
    @Operation(
        summary = "사용자 키워드 구독 목록",
        description = "특정 사용자의 키워드 구독 목록을 조회합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "키워드 구독 목록 조회 성공")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<KeywordSubscriptionDto>> getUserKeywordSubscriptions(
            @Parameter(name = "userId", description = "사용자 ID", example = "123") 
            @PathVariable Long userId) {
        List<KeywordSubscriptionDto> subscriptions = newsService.getUserKeywordSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }
}
