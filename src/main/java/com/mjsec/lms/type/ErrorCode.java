package com.mjsec.lms.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    //General Errors
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "잘못된 요청 포맷입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다."),

    // 학번 관련 에러
    DUPLICATE_STUDENT_NUMBER(HttpStatus.CONFLICT, "사용할 수 없는 학번 입니다."),
    INVALID_STUDENT_NUMBER(HttpStatus.BAD_REQUEST, "학번은 8자리 숫자 입니다."),

    // 이메일 관련 에러
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "이미 사용 중인 이메일입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "이메일 형식에 맞지 않습니다."),
    UNAUTHORIZED_EMAIL(HttpStatus.BAD_REQUEST, "정해진 이메일 도메인이 아닙니다."),
    EMAIL_VERIFICATION_PENDING(HttpStatus.UNAUTHORIZED, "이메일 인증이 완료되지 않았습니다. 인증을 진행해주세요."),
    FAILED_VERIFICATION(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
    AUTH_ATTEMPT_EXCEEDED(HttpStatus.BAD_REQUEST, "인증 횟수를 초과했습니다. 다시 시도해주세요."),
    EMPTY_EMAIL(HttpStatus.BAD_REQUEST, "이메일을 입력해주세요."),

    // 비밀번호 관련 에러
    EMPTY_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호를 입력해주세요."),
    INVALID_PASSWORD_LENGTH_MIN(HttpStatus.BAD_REQUEST, "비밀번호는 최소 8자 이상이어야 합니다."),
    INVALID_PASSWORD_LENGTH_MAX(HttpStatus.BAD_REQUEST, "비밀번호는 32자 이하로 입력해주세요."),
    INVALID_PASSWORD_WHITESPACE(HttpStatus.BAD_REQUEST, "비밀번호에 공백을 포함할 수 없습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호는 소문자, 대문자, 숫자 및 특수문자(!@#$%^&*)를 모두 포함해야 합니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호를 잘못 입력하셨습니다. 다시 입력해주세요."),

    //유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다."),
    ALREADY_REGISTERED_USER(HttpStatus.BAD_REQUEST, "이미 회원가입이 완료된 유저입니다."),

    //스터디
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다."),
    STUDY_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "스터디에 해당 사용자가 없습니다."),
    UNAUTHORIZED_ROLE(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다."),

    //공지사항
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    ANNOUNCEMENT_UNAUTHORIZED_ROLE(HttpStatus.FORBIDDEN, "공지사항에 접근할 권한이 없습니다."),
    ANNOUNCEMENT_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "공지 타입은 필수입니다.");


    private final HttpStatus status;
    private final String message;
    public HttpStatus getHttpStatus() {
        return status;
    }
    public String getDescription() {
        return message;
    }
}
