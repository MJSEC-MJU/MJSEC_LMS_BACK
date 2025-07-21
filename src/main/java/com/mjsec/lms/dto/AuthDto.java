package com.mjsec.lms.dto;

import jakarta.validation.Valid;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LogIn {

        private String loginId;
        private String password;
    }

    @Data
    public static class Register {

        @Valid
        private UserDto userDto;
    }
}
