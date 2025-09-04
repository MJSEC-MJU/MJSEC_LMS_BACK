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
    INVALID_MENTOR_STUDENT_NUMBER(HttpStatus.NOT_FOUND, "존재하지 않는 멘토 학번 입니다."),

    //스터디
    STUDY_NOT_FOUND(HttpStatus.NOT_FOUND, "스터디를 찾을 수 없습니다."),
    UNAUTHORIZED_MENTOR_ROLE(HttpStatus.UNAUTHORIZED, "멘토 권한인 유저만 가능합니다."),
    UNAUTHORIZED_MENTEE_ROLE(HttpStatus.UNAUTHORIZED, "멘티 권한인 유저만 가능합니다."),
    STUDY_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 스터디 멤버를 찾을 수 없습니다."),
    STUDY_GROUP_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "이미 존재하는 스터디 그룹 입니다."),

    //계획
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "계획을 찾을 수 없습니다."),

    //과제
    ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "과제가 없습니다."),
    UNAUTHORIZED_DOMAIN(HttpStatus.UNAUTHORIZED, "허용되지 않은 도메인입니다."),
    INVALID_URL_FORMAT(HttpStatus.BAD_REQUEST, "유효하지 않은 URL입니다."),
    WARNING_CONTENT(HttpStatus.BAD_REQUEST, "허용되지 않은 내용입니다."),
    DUPLICATE_SUBMISSION(HttpStatus.BAD_REQUEST, "중복된 과제 제출입니다."),
    ASSIGNMENT_COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "과제 댓글 내용이 없습니다"),

    //과제 피드백
    FEEDBACK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 피드백을 남겼습니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND,"피드백이 존재하지 않습니다."),
    FEEDBACK_CONTENT_REQUIRED(HttpStatus.NOT_FOUND, "과제 피드백 내용이 없습니다."),

    //과제 제출
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "과제 제출 내역을 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS_SUBMISSION(HttpStatus.UNAUTHORIZED, "과제 제출 조회 권한이 없습니다."),
    SUBMISSION_ASSIGNMENT_MISMATCH(HttpStatus.NOT_FOUND, "제출물이 해당 과제에 속하지 않습니다."),
    SUBMISSION_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "과제 제출 내용(링크, 비밀번호)이 없습니다."),

    //출석 체크
    DUPLICATE_ATTENDANCE_CHECK(HttpStatus.BAD_REQUEST, "중복된 출석체크입니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "끝일자가 시작일자보다 빠릅니다."),

    //공지사항
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    ANNOUNCEMENT_UNAUTHORIZED_ROLE(HttpStatus.FORBIDDEN, "관리자만 관리할 수 있습니다."),
    ANNOUNCEMENT_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "공지 타입은 필수입니다."),
    ANNOUNCEMENT_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "공지사항 제목은 필수입니다."),
    ANNOUNCEMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "공지사항 내용은 필수입니다."),
    ANNOUNCEMENT_FORBIDDEN(HttpStatus. FORBIDDEN,"본인이 작성한 공지사항만 수정/삭제할 수 있습니다."),

    //멘토
    MENTOR_ONLY_CAN_ADD_MEMBER(HttpStatus.BAD_REQUEST, "멘토만 스터디원을 추가할 수 있습니다."),
    MENTOR_ONLY_CAN_DELETE_MEMBER(HttpStatus.BAD_REQUEST, "멘토만 스터디원을 삭제할 수 있습니다.")
    ;
  
    private final HttpStatus status;
    private final String message;
    public HttpStatus getHttpStatus() {
        return status;
    }
    public String getDescription() {
        return message;
    }
}
