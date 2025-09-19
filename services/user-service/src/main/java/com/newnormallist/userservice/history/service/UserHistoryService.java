package com.newnormallist.userservice.history.service;

import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.history.dto.ReadHistoryResponse;
import com.newnormallist.userservice.history.entity.UserReadHistory;
import com.newnormallist.userservice.history.repository.UserReadHistoryRepository;
import com.newnormallist.userservice.user.dto.NewsInfo;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.repository.NewsRepository;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserHistoryService {
    private final UserReadHistoryRepository userReadHistoryRepository;
    private final NewsRepository newsRepository;
    private final UserRepository userRepository;


    /**
     * 뉴스 읽음 기록 추가 로직
     * @param userId 사용자 ID
     * @param newsId 읽은 뉴스 ID
     * */
    @Transactional
    public void addReadHistory(Long userId, Long newsId) {
        // 1. 먼저 기존 기록이 있는지 조회
        Optional<UserReadHistory> existingHistory = userReadHistoryRepository.findByUser_IdAndNewsId(userId, newsId);

        if (existingHistory.isPresent()) {
            // 2. 기록이 있으면 시간만 업데이트
            existingHistory.get().updateReadTime();
            log.info("뉴스 읽음 기록 업데이트 완료 - 사용자 ID: {}, 뉴스 ID: {}", userId, newsId);
        } else {
            // 3. 기록이 없으면 새로 생성하기
            try {
                createReadHistory(userId, newsId);
                log.info("뉴스 읽음 기록 추가 완료 - 사용자 ID: {}, 뉴스 ID: {}", userId, newsId);
            } catch (DataIntegrityViolationException e) {
                // 4. 만약 다른 스레드가 그사이에 먼저 INSERT 해서 예외가 터지면,
                //    그냥 한 번 더 조회해서 업데이트
                log.warn("읽기 기록 추가 중 동시성 충돌 발생. 업데이트 로직으로 전환. userId={}, newsId={}", userId, newsId);
                userReadHistoryRepository.findByUser_IdAndNewsId(userId, newsId)
                        .ifPresent(UserReadHistory::updateReadTime);
            }
        }
    }
    /**
     * 사용자별 뉴스 읽음 기록 추가 로직
     * */
    private void createReadHistory(Long userId, Long newsId) {
        // 1. 사용자 조회
        User user = findByUserId(userId);
        // 2. 뉴스 정보 조회 (제목 및 카테고리)
        NewsInfo newsInfo = getNewsInfo(newsId);
        // 3. 읽음 기록 엔티티 생성 및 저장
        UserReadHistory history = UserReadHistory.builder()
                .user(user)
                .newsId(newsId)
                .newsTitle(newsInfo.getTitle())
                .categoryName(newsInfo.getCategoryName())
                .build();
        userReadHistoryRepository.save(history);
    }
    /**
     * 뉴스 정보 조회
     * */
    private NewsInfo getNewsInfo(Long newsId) {
        return newsRepository.findNewsInfoById(newsId)
                .orElseThrow(() -> new UserException(ErrorCode.NEWS_NOT_FOUND));
    }
    /**
     * 사용자 조회 공통 메서드
     * */
    private User findByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 사용자가 읽은 뉴스 기록 조회 로직 (updated_at 포함)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<ReadHistoryResponse> 읽은 뉴스 기록 목록 (updated_at 포함)
     */
    @Transactional(readOnly = true)
    public Page<ReadHistoryResponse> getReadHistory(Long userId, Pageable pageable) {
        // updated_at 기준 내림차순으로 정렬된 기록 조회 후 DTO로 변환
        return userReadHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ReadHistoryResponse::new);
    }
}
