package com.mjsec.lms.common.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.mjsec.lms.announcement.domain.Announcement;
import com.mjsec.lms.user.domain.User;

@Getter
@AllArgsConstructor
public enum ResponseMessage {

    // Auth 관련
    REGISTER_SUCCESS("회원가입 요청 성공"),
    LOGIN_SUCCESS("로그인 성공"),
    LOGOUT_SUCCESS("로그아웃 성공"),
    STUDENT_NUMBER_CHECK_SUCCESS("사용 가능한 학번"),
    DUPLICATE_STUDENT_NUMBER("사용 중인 학번"),
    EMAIL_CHECK_SUCCESS("사용 가능한 이메일"),
    DUPLICATE_EMAIL("사용 중인 이메일"),

    // Admin 관련
    GET_ALL_PENDING_USER_SUCCESS("회원가입 승인 대기자 목록 반환 성공"),
    APPROVE_REGISTER_SUCCESS("회원가입 승인 완료"),
    CREATE_GROUP_SUCCESS("스터디 그룹 생성 성공"),
    UPDATE_GROUP_SUCCESS("스터디 그룹 수정 성공"),
    GET_ALL_USER_INFO_SUCCESS("전체 사용자 정보 조회 성공"),
    DELETE_USER_SUCCESS("유저 삭제 성공"),
    REFUSE_REGISTER_SUCCESS("회원가입 반려 완료"),

    // User 관련
    USER_GET_PAGE_SUCCESS("유저 페이지 조회 성공"),
    SEND_CODE_SUCCESS("인증 코드 전송 성공"),
    EMAIL_VERIFICATION_SUCCESS("이메일 인증 성공"),
    UPDATE_PASSWORD_SUCCESS("비밀번호 변경 성공"),
    UPDATE_USER_SUCCESS("유저 정보 수정 성공"),

    // 과제
    ASSIGNMENT_SUBMIT_SUCCESS("과제 제출 성공"),
    ASSIGNMENT_SUBMIT_CHECK_SUCCESS("과제 제출 확인 성공"),
    ASSIGNMENT_SUBMIT_UPDATE_SUCCESS("과제 제출 내용 수정 성공"),
    ASSIGNMENT_SUBMIT_DELETE_SUCCESS("제출한 과제 삭제 성공"),
    FEEDBACK_LEAVE_SUCCESS("과제 피드백 남기기 성공"),
    COMMENT_CREATE_SUCCESS("댓글 생성 성공"),
    COMMENT_UPDATE_SUCCESS("댓글 수정 성공"),
    COMMENT_DELETE_SUCCESS("댓글 삭제 성공"),
    FEEDBACK_UPDATE_SUCCESS("과제 피드백 수정 성공"),
    FEEDBACK_DELETE_SUCCESS("과제 피드백 삭제 성공"),
    PENDING_FEEDBACK_SUBMISSIONS_SUCCESS("피드백 대기 중인 과제 조회 성공"),
    REVISION_REQUIRED_SUBMISSIONS_SUCCESS("수정 필요한 과제 조회 성공"),
    SUBMISSION_STATISTICS_SUCCESS("과제 제출 통계 조회 성공"),

    // 계획
    PLAN_CREATE_SUCCESS("계획 등록 성공"),
    PLAN_DELETE_SUCCESS("계획 삭제 성공"),
    PLAN_UPDATE_SUCCESS("계획 수정 성공"),
    PLAN_SUCCESS("계획 조회 성공"),

    // 출석체크 관련
    ATTENDANCE_GET_SUCCESS("출석체크 조회 성공"),
    ATTENDANCE_CREATE_SUCCESS("출석체크 성공"),
    WEEKLY_ATTENDANCE_GET_SUCCESS("주차별 출석체크 조회 성공"),
    ALL_WEEKS_ATTENDANCE_GET_SUCCESS("전체 주차별 출석체크 조회 성공"),

    // Announcement 관련
    POST_ANNOUNCEMENT_SUCCESS("공지사항 등록 성공"),
    GET_ANNOUNCEMENT_SUCCESS("전체 공지사항 목록 반환 성공"),
    DETAIL_ANNOUNCEMENT_SUCCESS("공지사항 상제 조회 성공"),
    UPDATE_ANNOUNCEMENT_SUCCESS("공지사항 수정 성공"),
    DELETE_ANNOUNCEMENT_SUCCESS("공지사항 삭제 성공"),

    // 테스트 관련
    TEST_SUCCESS("테스트 성공"),

    // Mentor 관련
    ADD_MEMBER_SUCCESS("스터디원 추가 성공"),
    DELETE_MEMBER_SUCCESS("스터디원 삭제 성공"),
    WARN_MEMBER_SUCCESS("수동 경고 부여 성공"),

    // 활동 글 관련
    STUDY_ACTIVITY_SUCCESS("활동 글 생성 성공"),
    STUDY_ACTIVITY_GET_SUCCESS("활동 글 조회 성공"),
    STUDY_ACTIVITY_DELETE_SUCCESS("활동 글 삭제 성공"),
    STUDY_ACTIVITY_UPDATE_SUCCESS("활동 글 수정 성공"),

    // 스터디그룹 관련
    STUDY_MEMBER_GET_SUCCESS("스터디 전체 멤버 반환 성공"),
    STUDY_MENTEE_GET_SUCCESS("스터디 멘티 멤버 반환 성공"),
    GET_ALL_GROUPS_SUCCESS("전체 스터디 그룹 정보 조회 성공"),
    GET_ALL_WARN_SUCCESS("스터디 멘티 멤버들 경고 조회 성공"),
    UPDATE_GROUP_STATUS_SUCCESS("스터디 그룹 상태 변경 성공"),
    GROUP_NAME_CHECK_SUCCESS("사용 가능한 스터디 이름"),
    DUPLICATE_GROUP_NAME("사용 중인 스터디 이름"),
    STUDY_GROUP_DETAIL_GET_SUCCESS("스터디 그룹 상세 정보 조회 성공"),
    DELETE_GROUP_SUCCESS("스터디 그룹 삭제 성공"),
    ;
    private final String message;
}
