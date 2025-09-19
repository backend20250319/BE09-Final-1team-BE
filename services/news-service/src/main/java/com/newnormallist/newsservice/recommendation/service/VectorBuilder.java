package com.newnormallist.newsservice.recommendation.service;

import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.UserEntity;
import com.newnormallist.newsservice.recommendation.entity.UserPrefVector;

/* 사용자 벡터 계산기 인터페이스.

    구현체(예: VectorBuilderImpl)는 한 유저에 대해:
    D(c): DemoBaseProvider에서 (연령/성별별) 베이스 분포 수집
    P(c): UserCategoryRepository → 선택된 카테고리 균등분포(1/k)
    R(c): UserReadHistoryRepository → 최근 7일 감쇠 가중합으로 비율화
    S(c): NewsScrapRepository → 최근 30일 감쇠 가중합으로 비율화
    WeightSelector.choose(readCount, scrapCount)로 케이스별 가중치 선택
    최종식 Score(c)=Norm(wD·D + wP·P + wR·R + wS·S)로 9개 값 계산
    UserPrefVector 9행으로 만들어 반환 

*/
public interface VectorBuilder {
    List<UserPrefVector> recomputeForUser(UserEntity userEntity);
}
