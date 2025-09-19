package com.newnormallist.userservice.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshTokenRequestDto {
    private String refreshToken;
}
