package com.newnormallist.newsservice.recommendation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


// 사용자 마스터 (생년월일과 성별로 AgeBucket 유추 -> D(c)에 쓰임)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "letter_ok", nullable = false)
    @Builder.Default
    private Boolean letterOk = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
}