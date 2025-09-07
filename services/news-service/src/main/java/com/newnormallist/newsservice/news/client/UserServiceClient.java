package com.newnormallist.newsservice.news.client;

import com.newnormallist.newsservice.news.client.dto.UserInterestResponse;
import com.newnormallist.newsservice.news.client.dto.UserBehaviorAnalysis;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User Service를 위한 Feign Client
 * 뉴스 서비스에서 User Service의 API를 호출할 때 사용
 */
@FeignClient(name = "user-service", url = "${feign.client.user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    /**
     * 사용자 관심사 분석 조회
     * @param userId 사용자 ID
     * @return UserInterestResponse 사용자 관심사 정보
     */
    @GetMapping("/api/users/{userId}/interests")
    UserInterestResponse getUserInterests(@PathVariable("userId") Long userId);

    /**
     * 사용자 행동 분석 조회
     * @param userId 사용자 ID
     * @return UserBehaviorAnalysis 사용자 행동 분석 결과
     */
    @GetMapping("/api/users/{userId}/behavior-analysis")
    UserBehaviorAnalysis getUserBehaviorAnalysis(@PathVariable("userId") Long userId);

    /**
     * 사용자 카테고리별 읽기 기록 조회
     * @param userId 사용자 ID
     * @return Map<String, Long> 카테고리별 읽기 횟수
     */
    @GetMapping("/api/users/{userId}/category-reading-history")
    Map<String, Long> getCategoryReadingHistory(@PathVariable("userId") Long userId);

    /**
     * 사용자 읽기 시간 선호도 조회
     * @param userId 사용자 ID
     * @return String 읽기 시간 선호도 (MORNING, AFTERNOON, EVENING, NIGHT)
     */
    @GetMapping("/api/users/{userId}/reading-time-preference")
    String getReadingTimePreference(@PathVariable("userId") Long userId);

    /**
     * 사용자 디바이스 선호도 조회
     * @param userId 사용자 ID
     * @return String 디바이스 선호도 (MOBILE, DESKTOP, TABLET)
     */
    @GetMapping("/api/users/{userId}/device-preference")
    String getDevicePreference(@PathVariable("userId") Long userId);

    /**
     * 사용자 콘텐츠 길이 선호도 조회
     * @param userId 사용자 ID
     * @return String 콘텐츠 길이 선호도 (SHORT, MEDIUM, LONG)
     */
    @GetMapping("/api/users/{userId}/content-length-preference")
    String getContentLengthPreference(@PathVariable("userId") Long userId);

    /**
     * 사용자 카카오 토큰 조회
     * @param userId 사용자 ID
     * @return String 카카오 토큰
     */
    @GetMapping("/api/users/{userId}/kakao-token")
    String getKakaoToken(@PathVariable("userId") String userId);
}
