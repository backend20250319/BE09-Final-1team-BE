package com.newnormallist.userservice.client;

import com.newnormallist.userservice.history.dto.CategoryPreferenceResponse;
import com.newnormallist.userservice.history.dto.UserBehaviorAnalysis;
import com.newnormallist.userservice.history.dto.UserInterestResponse;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User Service를 위한 Feign Client
 * 다른 마이크로서비스에서 User Service의 API를 호출할 때 사용
 */
@FeignClient(name = "user-service", url = "${feign.client.user-service.url:http://localhost:8081}")
public interface UserServiceClient {

    /**
     * 사용자 정보 조회
     * @param userId 사용자 ID
     * @return MyPageResponse 사용자 정보
     */
    @GetMapping("/api/users/{userId}")
    MyPageResponse getUserInfo(@PathVariable("userId") Long userId);

    /**
     * 사용자 행동 분석 조회
     * @param userId 사용자 ID
     * @return UserBehaviorAnalysis 사용자 행동 분석 결과
     */
    @GetMapping("/api/users/{userId}/behavior-analysis")
    UserBehaviorAnalysis getUserBehaviorAnalysis(@PathVariable("userId") Long userId);

    /**
     * 사용자 카테고리 선호도 조회
     * @param userId 사용자 ID
     * @return CategoryPreferenceResponse 카테고리 선호도 정보
     */
    @GetMapping("/api/users/{userId}/category-preferences")
    CategoryPreferenceResponse getCategoryPreferences(@PathVariable("userId") Long userId);

    /**
     * 사용자 관심사 분석 조회
     * @param userId 사용자 ID
     * @return UserInterestResponse 사용자 관심사 정보
     */
    @GetMapping("/api/users/{userId}/interests")
    UserInterestResponse getUserInterests(@PathVariable("userId") Long userId);

    /**
     * 사용자 관심사 점수 맵 조회 (뉴스레터 개인화용)
     * @param userId 사용자 ID
     * @return Map<카테고리, 선호도점수> 관심사 점수 맵
     */
    @GetMapping("/api/users/{userId}/interests/score-map")
    Map<String, Double> getUserInterestScoreMap(@PathVariable("userId") Long userId);

    /**
     * 사용자의 카카오 토큰 조회
     * @param userId 사용자 ID
     * @return 카카오 액세스 토큰
     */
    @GetMapping("/api/auth/tokens/kakao/{userId}")
    String getKakaoToken(@PathVariable("userId") String userId);
    @GetMapping("/api/users/{userId}/interest-scores")
    Map<String, Double> getInterestScores(@PathVariable("userId") Long userId);

    /**
     * 사용자 상위 관심사 목록 조회 (뉴스레터 개인화용)
     * @param userId 사용자 ID
     * @return List<String> 상위 관심사 카테고리 목록
     */
    @GetMapping("/api/users/{userId}/top-interests")
    java.util.List<String> getTopInterests(@PathVariable("userId") Long userId);
}
