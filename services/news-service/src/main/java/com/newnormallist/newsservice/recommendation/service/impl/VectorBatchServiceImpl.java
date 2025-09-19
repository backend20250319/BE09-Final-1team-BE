package com.newnormallist.newsservice.recommendation.service.impl;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.VectorBatchService;
import com.newnormallist.newsservice.recommendation.service.VectorBuilder;
import com.newnormallist.newsservice.recommendation.entity.*;
import com.newnormallist.newsservice.recommendation.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/* 
    벡터 저장/갱신 orchestrator 구현체.

    스케줄러/로그인 트리거/운영 호출 등에서 VectorBuilder 호출 →
    UserPrefVectorRepository.upsertBatch(userId, 9행) 저장.

    stale 정책(최종 업데이트 10분 경과, 마지막 계산 이후 조회≥5 등) 관리.
*/
@Service
@RequiredArgsConstructor
public class VectorBatchServiceImpl implements VectorBatchService {

    private final VectorBuilder vectorBuilder;
    private final UserPrefVectorRepository userPrefVectorRepository;
    private final UserRepository userRepository;
    private final UserReadHistoryRepository userReadHistoryRepository;
    
    private static final int STALE_MINUTES = 10; // 10분 경과시 stale
    private static final int READ_THRESHOLD_FOR_UPDATE = 5; // 조회 5회 이상시 업데이트

    @Override
    public void upsert(Long userId) {
        
        // 1. 사용자 존재 확인
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return; // 사용자가 존재하지 않으면 무시
        }
        UserEntity userEntity = userOpt.get();
        
        // 2. 현재 벡터 조회
        List<UserPrefVector> currentVectors = userPrefVectorRepository
            .findAllByUserIdOrderByScoreDesc(userId);
        
        // 3. stale 판단
        if (!isStale(currentVectors, userId)) {
            return; // stale하지 않으면 업데이트하지 않음
        }
        
        // 4. 새로운 벡터 계산
        List<UserPrefVector> newVectors = vectorBuilder.recomputeForUser(userEntity);
        
        // 5. 기존 벡터 삭제 후 새로운 벡터 저장
        if (!currentVectors.isEmpty()) {
            userPrefVectorRepository.deleteAll(currentVectors);
        }
        userPrefVectorRepository.saveAll(newVectors);
    }
    
    private boolean isStale(List<UserPrefVector> currentVectors, Long userId) {
        
        // 벡터가 없으면 stale
        if (currentVectors.isEmpty()) {
            return true;
        }
        
        // 최신 벡터의 업데이트 시간 확인
        UserPrefVector latestVector = currentVectors.get(0);
        LocalDateTime lastUpdate = latestVector.getUpdatedAt();
        LocalDateTime staleThreshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        
        // 10분 경과시 stale
        if (lastUpdate.isBefore(staleThreshold)) {
            return true;
        }
        
        // 마지막 업데이트 이후 조회 기록 확인
        LocalDateTime readCountSince = lastUpdate;
        long readCount = userReadHistoryRepository.findByUserId(userId)
            .stream()
            .filter(history -> history.getCreatedAt().isAfter(readCountSince))
            .count();
        
        // 조회 5회 이상시 stale
        return readCount >= READ_THRESHOLD_FOR_UPDATE;
    }
}
