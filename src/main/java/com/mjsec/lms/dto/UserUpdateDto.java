package com.mjsec.lms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserUpdateDto {

    @Pattern(regexp = "^[가-힣]+$", message = "사용자 이름은 한글로만 이루어져야 합니다.")
    private String name;

    @Email(message = "유효한 이메일 주소 형식이어야 합니다.")
    private String email;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-xxxx-xxxx 형식이어야 합니다.")
    private String phoneNumber;
}
