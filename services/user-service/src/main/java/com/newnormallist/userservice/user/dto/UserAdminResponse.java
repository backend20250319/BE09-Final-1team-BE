package com.newnormallist.userservice.user.dto;

import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.entity.UserRole;
import com.newnormallist.userservice.user.entity.UserStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserAdminResponse {
    private final Long id;
    private final String email;
    private final String name;
    private final UserStatus status;
    private final UserRole role;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public UserAdminResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.status = user.getStatus();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }
}
