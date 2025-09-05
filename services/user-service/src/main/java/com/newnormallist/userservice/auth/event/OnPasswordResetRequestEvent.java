package com.newnormallist.userservice.auth.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OnPasswordResetRequestEvent {
    private final String email;
    private final String token;
}
