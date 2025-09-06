package com.newnormallist.userservice.user.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 10, max = 20, message = "비밀번호는 10자 이상, 20자 이하로 입력해야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 모두 포함해야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "출생연도는 필수입니다.")
    private Integer birthYear;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "^(MALE|FEMALE)$", message = "성별은 'MALE' 또는 'FEMALE'로만 입력 가능합니다.")
    private String gender;

    @NotEmpty(message = "관심사는 최소 하나 이상 선택해야 합니다.")
    private Set<NewsCategory> hobbies;
}
