package com.mjsec.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordUpdateDto {

    @Email(message = "유효한 이메일 주소 형식이어야 합니다.")
    @NotBlank
    private String email;

    @Pattern(
            regexp = "^(?=(?:.*[a-z]))(?=(?:.*[A-Z]))(?=(?:.*\\\\d){5,})(?=(?:.*[\\\\W_]){2,}).{8,}$\n",
            message = "비밀번호는 총 8자 이상이며 대소문자, 숫자 5개 이상, 특수문자 2개 이상을 포함해야 합니다."
    )
    @NotBlank
    private String password;
}
