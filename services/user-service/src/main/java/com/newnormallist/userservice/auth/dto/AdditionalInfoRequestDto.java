package com.newnormallist.userservice.auth.dto;

import com.newnormallist.userservice.user.entity.NewsCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Getter
@NoArgsConstructor
public class AdditionalInfoRequestDto {
    @NotNull(message = "출생연도는 필수입니다.")
    private Integer birthYear;

    @NotNull(message = "성별은 필수입니다.")
    private String gender; // "MALE" or "FEMALE"

    private List<String> hobbies;

    @NotBlank(message = "기기 식별자는 필수입니다.")
    private String deviceId;
}
