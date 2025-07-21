package com.mjsec.lms.dto;

import com.mjsec.lms.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    @NotNull(message = "학번은 필수입니다.")
    @Min(value = 10000000, message = "학번은 8자리여야 합니다.")
    @Max(value = 99999999, message = "학번은 8자리여야 합니다.")
    private Long studentNumber;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d{5,})(?=.*[\\W_]{2,}).*$",
            message = "비밀번호는 대소문자, 숫자 5개 이상, 특수문자 2개 이상을 포함해야 합니다."
    )
    @NotBlank
    private String password;

    @Pattern(regexp = "^[ㄱ-ㅎ|가-힣]*$", message = "사용자 이름은 한글로만 이루어져야 합니다.")
    @NotBlank
    private String name;

    @Email(message = "유효한 이메일 주소 형식이어야 합니다.")
    @NotBlank
    private String email;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-xxxx-xxxx 형식이어야 합니다.")
    @NotBlank
    private String phoneNumber;
}
