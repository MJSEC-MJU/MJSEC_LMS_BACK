package com.mjsec.lms.dto.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LogIn {

        @NotNull(message = "학번은 필수입니다.")
        @Min(value = 10000000, message = "학번은 8자리여야 합니다.")
        @Max(value = 99999999, message = "학번은 8자리여야 합니다.")
        private Long studentNumber;

        @Pattern(
                regexp = "^(?=(?:.*[a-z]))(?=(?:.*[A-Z]))(?=(?:.*\\\\d){5,})(?=(?:.*[\\\\W_]){2,}).{8,}$\n",
                message = "비밀번호는 총 8자 이상이며 대소문자, 숫자 5개 이상, 특수문자 2개 이상을 포함해야 합니다."
        )
        private String password;
    }

    @Data
    public static class Register {

        @Valid
        private UserDto userDto;
    }
}
