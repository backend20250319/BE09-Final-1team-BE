package com.newnormallist.userservice.auth.token;

import com.newnormallist.userservice.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expireDate;

    @Builder
    public PasswordResetToken(String token, User user, LocalDateTime expireDate) {
        this.token = token;
        this.user = user;
        this.expireDate = expireDate;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireDate);
    }





}
