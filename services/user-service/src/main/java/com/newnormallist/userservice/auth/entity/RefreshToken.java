package com.newnormallist.userservice.auth.entity;

import com.newnormallist.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 사용자와 1:1 관계 설정
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String tokenValue;

  // 생성자
  public RefreshToken(User user, String tokenValue) {
    this.user = user;
    this.tokenValue = tokenValue;
  }

  // 토큰 값 업데이트 메소드
  public void updateTokenValue(String newTokenValue) {
    this.tokenValue = newTokenValue;
  }
}