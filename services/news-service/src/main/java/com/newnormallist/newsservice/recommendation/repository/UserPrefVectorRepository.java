package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.UserPrefVector;

// 유저 벡터 (9행) 조회/업서트
// findTop3ByUserId(score DESC LIMIT 3)로 피드 조립 시 사용.
public interface UserPrefVectorRepository extends JpaRepository<UserPrefVector, UserPrefVector.PK> {
    
    // 모든 벡터 조회 (점수 내림차순)
    @Query("select v from UserPrefVector v where v.userId = :uid order by v.score desc")
    List<UserPrefVector> findAllByUserIdOrderByScoreDesc(@Param("uid") Long userId);
    
    // 상위 N개 벡터 조회 (Pageable 사용)
    @Query("select v from UserPrefVector v where v.userId = :uid order by v.score desc")
    List<UserPrefVector> findTopByUserIdOrderByScoreDesc(@Param("uid") Long userId, Pageable pageable);
}
