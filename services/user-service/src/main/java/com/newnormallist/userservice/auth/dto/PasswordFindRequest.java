package com.newnormallist.userservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordFindRequest {
    @NotBlank
    private String name;
    @NotBlank
    @Email
    private String email;
}
