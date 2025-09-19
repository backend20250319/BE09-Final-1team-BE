package com.newnormallist.userservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordResetRequest {
    @NotBlank
    private String token;
    @NotBlank
    @Size(min = 10, max = 20, message = "비밀번호는 10자 이상, 20자 이하로 입력해야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
