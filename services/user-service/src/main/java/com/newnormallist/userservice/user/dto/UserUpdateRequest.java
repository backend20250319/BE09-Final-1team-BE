package com.newnormallist.userservice.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String currentPassword;

    @Size(min = 10, max = 20, message = "비밀번호는 10자 이상, 20자 이하로 입력해야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String newPassword;

    private String confirmPassword;

    @NotNull(message = "뉴스레터 수신 동의 여부는 필수입니다.")
    private Boolean letterOk;

    private Set<String> hobbies;
}
