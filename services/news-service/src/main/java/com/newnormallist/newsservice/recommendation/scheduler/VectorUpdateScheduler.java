// package com.newnormallist.newsservice.recommendation.scheduler;

// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// import com.newnormallist.newsservice.recommendation.service.VectorBatchService;
// import com.newnormallist.newsservice.recommendation.repository.UserRepository;
// import com.newnormallist.newsservice.recommendation.entity.User;

// import java.util.List;

// // 사용자 벡터 주기적 업데이트 스케줄러
// @Component
// @RequiredArgsConstructor
// @Slf4j
// public class VectorUpdateScheduler {

//     private final VectorBatchService vectorBatchService;
//     private final UserRepository userRepository;

//     // 매일 새벽 2시에 모든 활성 사용자의 벡터 업데이트
//     @Scheduled(cron = "0 0 2 * * ?")
//     public void updateAllUserVectors() {
//         log.info("Starting daily vector update for all active users");
        
//         try {
//             List<User> activeUsers = userRepository.findAll(); // TODO: ACTIVE 상태 필터링 추가
            
//             int updatedCount = 0;
//             for (User user : activeUsers) {
//                 try {
//                     vectorBatchService.upsert(user.getId());
//                     updatedCount++;
//                 } catch (Exception e) {
//                     log.error("Failed to update vector for user {}: {}", user.getId(), e.getMessage());
//                 }
//             }
            
//             log.info("Daily vector update completed. Updated {} users out of {}", updatedCount, activeUsers.size());
            
//         } catch (Exception e) {
//             log.error("Daily vector update failed: {}", e.getMessage(), e);
//         }
//     }

//     // 매시간 정각에 오래된 벡터 업데이트 (10분 이상 경과)
//     @Scheduled(cron = "0 0 * * * ?")
//     public void updateStaleVectors() {
//         log.info("Starting hourly stale vector update");
        
//         try {
//             List<User> activeUsers = userRepository.findAll(); // TODO: ACTIVE 상태 필터링 추가
            
//             int updatedCount = 0;
//             for (User user : activeUsers) {
//                 try {
//                     vectorBatchService.upsert(user.getId());
//                     updatedCount++;
//                 } catch (Exception e) {
//                     log.error("Failed to update stale vector for user {}: {}", user.getId(), e.getMessage());
//                 }
//             }
            
//             log.info("Hourly stale vector update completed. Updated {} users out of {}", updatedCount, activeUsers.size());
            
//         } catch (Exception e) {
//             log.error("Hourly stale vector update failed: {}", e.getMessage(), e);
//         }
//     }
// }
