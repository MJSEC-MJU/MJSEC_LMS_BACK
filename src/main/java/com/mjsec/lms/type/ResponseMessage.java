package com.mjsec.lms.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

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
    DUPLICATE_EMAIL("사용 중인 이메일"),
    // Admin 관련
    GET_ALL_PENDING_USER_SUCCESS("회원가입 승인 대기자 목록 반환 성공"),
    APPROVE_REGISTER_SUCCESS("회원가입 승인 완료"),

    //User 관련
    USER_GET_PAGE_SUCCESS("유저 페이지 조회 성공"),

    // Assignment 관련
    ASSIGNMENT_CREATE_SUCCESS("과제 등록 성공"),
    ASSIGNMENT_DELETE_SUCCESS("과제 삭제 성공"),
    ASSIGNMENT_UPDATE_SUCCESS("과제 수정 성공"),
    ASSIGNMENT_SUCCESS("과제 조회 성공"),
    ASSIGNMENT_SUBMIT_SUCCESS("과제 제출 성공"),
    ASSIGNMENT_SUBMIT_CHECK_SUCCESS("과제 제출 확인 성공"),
    ASSIGNMENT_SUBMIT_UPDATE_SUCCESS("과제 제출 내용 수정 성공"),
    ASSIGNMENT_SUBMIT_DELETE_SUCCESS("제출한 과제 삭제 성공"),
    FEEDBACK_LEAVE_SUCCESS("과제 피드백 남기기 성공"),
    COMMENT_CREATE_SUCCESS("댓글 생성 성공"),
    FEEDBACK_UPDATE_SUCCESS("과제 피드백 수정 성공"),
    FEEDBACK_DELETE_SUCCESS("과제 피드백 삭제 성공"),

    //출석체크
    ATTENDANCE_GET_SUCCESS("출석체크 조회 성공"),
    ATTENDANCE_CREATE_SUCCESS("출석체크 성공"),

    // Announcement관련
    POST_ANNOUNCEMENT_SUCCESS("공지사항 등록 성공"),
    GET_ANNOUNCEMENT_SUCCESS("전체 공지사항 목록 반환 성공"),
    DETAIL_ANNOUNCEMENT_SUCCESS("공지사항 상제 조회 성공"),
    UPDATE_ANNOUNCEMENT_SUCCESS("공지사항 수정 성공"),
    DELETE_ANNOUNCEMENT_SUCCESS("공지사항 삭제 성공"),

    //테스트 관련
    TEST_SUCCESS("테스트 성공"),
    ;
    private final String message;
}
