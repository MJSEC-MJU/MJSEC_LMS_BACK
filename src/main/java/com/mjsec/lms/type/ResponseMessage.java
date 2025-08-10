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
    DUPLICATE_EMAIL("사용 중인 이메일"),
    // Admin 관련
    GET_ALL_PENDING_USER_SUCCESS("회원가입 승인 대기자 목록 반환 성공"),
    APPROVE_REGISTER_SUCCESS("회원가입 승인 완료"),

    // Assignment 관련
    ASSIGNMENT_CREATE_SUCCESS("과제 등록 성공"),
    ASSIGNMENT_DELETE_SUCCESS("과제 삭제 성공"),
    ASSIGNMENT_UPDATE_SUCCESS("과제 수정 성공"),
    ASSIGNMENT_SUCCESS("과제 조회 성공"),

    // Announcement관련
    POST_ANNOUNCEMENT_SUCCESS("공지사항 등록 성공"),
    GET_ANNOUNCEMENT_SUCCESS("전체 공지사항 목록 반환 성공"),
    DETAIL_ANNOUNCEMENT_SUCCESS("공지사항 상제 조회 성공"),
    UPDATE_ANNOUNCEMENT_SUCCESS("공지사항 수정 성공");
    private final String message;
}
