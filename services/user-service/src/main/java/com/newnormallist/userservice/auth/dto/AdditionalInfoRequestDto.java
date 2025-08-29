package com.newnormallist.userservice.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AdditionalInfoRequestDto {
    @NotNull(message = "출생연도는 필수입니다.")
    private Integer birthYear;

    @NotNull(message = "성별은 필수입니다.")
    private String gender; // "MALE" or "FEMALE"

    private List<String> hobbies;
}
