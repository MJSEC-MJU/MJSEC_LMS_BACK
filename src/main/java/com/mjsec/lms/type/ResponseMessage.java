package com.mjsec.lms.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    // Auth 관련
    REGISTER_SUCCESS("회원가입 요청 성공"),
    LOGIN_SUCCESS("로그인 성공"),
    LOGOUT_SUCCESS("로그아웃 성공"),
    STUDENT_NUMBER_CHECK_SUCCESS("사용 가능한 학번"),
    DUPLICATE_STUDENT_NUMBER("사용 중인 학번"),
    EMAIL_CHECL_SUCCESS("사용 가능한 이메일"),
    DUPLICATE_EMAIL("사용 중인 이메일")
    ;

    private final String message;
}
