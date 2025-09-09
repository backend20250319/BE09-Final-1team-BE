package com.newnormallist.userservice.user.repository;

import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // 관리자용 DELETE 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from User u where u.id = :id and u.status = 'DELETED'")
    int hardDeleteIFDeleted(@Param("id") Long id);

    // 배치 하드 삭제 쿼리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from User u where u.status = :status and u.updatedAt < :before")
    int deleteByStatusBefore(@Param("status") UserStatus status,
                             @Param("before") LocalDateTime before);
}
