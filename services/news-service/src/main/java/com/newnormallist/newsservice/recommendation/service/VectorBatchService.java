package com.newnormallist.newsservice.recommendation.service;

/* 
    벡터 저장/갱신 orchestrator 인터페이스.

    구현체(예: VectorBatchServiceImpl)는:

    스케줄러/로그인 트리거/운영 호출 등에서 VectorBuilder 호출 →
    UserPrefVectorRepository.upsertBatch(userId, 9행) 저장.

    stale 정책(최종 업데이트 10분 경과, 마지막 계산 이후 조회≥5 등) 관리.
*/
public interface VectorBatchService {
    void upsert(Long userId); // stale 판단/일괄 갱신은 구현에서
}