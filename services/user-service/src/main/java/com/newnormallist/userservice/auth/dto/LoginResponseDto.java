    package com.newnormallist.userservice.auth.dto;

import com.newnormallist.userservice.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserInfoDto user;

    @Getter
    @AllArgsConstructor
    public static class UserInfoDto {
        private Long id;
        private String email;
        private String name;
        private UserRole role;
    }
}
