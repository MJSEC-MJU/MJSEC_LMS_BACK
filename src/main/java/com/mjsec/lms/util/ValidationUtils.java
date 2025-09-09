package com.mjsec.lms.util;

import com.mjsec.lms.domain.mentor.Plan;
import com.mjsec.lms.domain.mentor.PlanComment;
import com.mjsec.lms.domain.study.*;
import com.mjsec.lms.domain.user.User;
import com.mjsec.lms.repository.mentor.AttendanceRepository;
import com.mjsec.lms.repository.mentor.PlanCommentRepository;
import com.mjsec.lms.repository.mentor.PlanRepository;
import com.mjsec.lms.repository.user.UserRepository;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.study.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import com.mjsec.lms.type.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ValidationUtils {

    // 보안 관련 상수들
    private static final String[] ALLOWED_DOMAINS = {
            "velog.io",
            "tistory.com",
            "blog.naver.com"
    };

    private static final String[] XSS_PATTERNS = {
            "<script", "javascript:", "onload=", "onerror=", "onclick=",
            "onmouseover=", "eval(", "alert(", "document.cookie"
    };

    private static final String[] SQL_INJECTION_PATTERNS = {
            "union select", "drop table", "delete from", "insert into",
            "update set", "' or '1'='1", "-- ", "/*"
    };

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$"
    );

    // Repository 의존성들
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AttendanceRepository attendanceRepository;
    private final PlanRepository planRepository;
    private final SubmissionRepository submissionRepository;
    private final PlanCommentRepository planCommentRepository;
    private final StudyActivityRepository studyActivityRepository;

    public ValidationUtils(UserRepository userRepository,
                           StudyGroupRepository studyGroupRepository,
                           GroupMemberRepository groupMemberRepository,
                           AttendanceRepository attendanceRepository,
                           PlanRepository planRepository,
                           SubmissionRepository submissionRepository,
                           PlanCommentRepository planCommentRepository,
                           StudyActivityRepository studyActivityRepository) {

        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.attendanceRepository = attendanceRepository;
        this.planRepository = planRepository;
        this.submissionRepository = submissionRepository;
        this.planCommentRepository = planCommentRepository;
        this.studyActivityRepository = studyActivityRepository;
    }

    // ========== 기본 엔티티 검증 ==========

    // 학번으로 사용자 존재 여부를 확인하고 User 객체를 반환
    public User validateUser(Long studentNumber) {

        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }

    // 스터디 그룹 존재 여부를 확인하고 StudyGroup 객체를 반환
    public StudyGroup validateStudyGroup(Long groupId) {

        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));
    }

    // 계획 존재 여부를 확인하고 Plan 객체를 반환
    public Plan validatePlan(Long planId) {

        return planRepository.findById(planId)
                .orElseThrow(() -> new RestApiException(ErrorCode.PLAN_NOT_FOUND));
    }

    // 제출물 존재 여부를 확인하고 AssignmentSubmission 객체를 반환
    public AssignmentSubmission validateSubmission(Long submitId) {

        return submissionRepository.findById(submitId)
                .orElseThrow(() -> new RestApiException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    //스터디 활동 글의 존재 여부를 확인하고 StudyActivity 객체를 반환
    public StudyActivity validateStudyActivity(Long activityId){

        return studyActivityRepository.findById(activityId)
                .orElseThrow(()-> new RestApiException(ErrorCode.STUDY_ACTIVITY_NOT_FOUND));
    }

    // ========== 멤버십 및 역할 검증 ==========

    // 사용자가 해당 스터디 그룹의 멤버인지 확인
    public void validateGroupMembership(User user, StudyGroup studyGroup) {

        groupMemberRepository.findByUserAndStudyGroup(user, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));
    }

    // 사용자가 해당 스터디 그룹의 멘토인지 확인
    public void validateMentorRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if (userRole != GroupMemberRole.MENTOR) {
            log.warn("User {} does not have MENTOR role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTOR_ROLE);
        }
    }

    // 사용자가 해당 스터디 그룹의 멘티인지 확인
    public void validateMenteeRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if (userRole != GroupMemberRole.MENTEE) {
            log.warn("User {} does not have MENTEE role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTEE_ROLE);
        }
    }

    // 사용자가 해당 스터디 그룹에서 멘토인지 멘티인지 확인하여 역할 반환
    public GroupMemberRole validateUserRole(Long userId, Long groupId) {

        return groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));
    }

    // 로그인한 사용자가 ADMIN인지 확인하기
    public void validateAdminRole(Long currentStudentNumber) {

        User user = validateUser(currentStudentNumber);

        if(user.getRole() != UserRole.ROLE_ADMIN){
            throw new RestApiException(ErrorCode.UNAUTHORIZED);
        }
    }

    //댓글 작성자 본인인지 확인하기
    public PlanComment validateCommentAccess(Long commentId, Long userId){

        return planCommentRepository.findByCommentIdAndAuthor_UserId(commentId,userId).orElseThrow(()-> new RestApiException(ErrorCode.PLAN_COMMENT_NOT_FOUND));
    }

    // ========== 복합 접근 검증 ==========

    // 기본 접근 검증 (사용자, 스터디 그룹 존재, 멤버십)
    public User validateBasicAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        return user;
    }

    // 멘토 접근 검증 (사용자 존재, 스터디 그룹 존재, 멘토 권한)
    public User validateMentorAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);
        return user;
    }

    // 멘티 접근 검증 (사용자 존재, 스터디 그룹 존재, 멘티 권한)
    public User validateMenteeAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        validateMenteeRole(user.getUserId(), groupId);
        return user;
    }

    // ========== 출석 관련 검증 ==========

    // 중복 출석 체크 방지 (같은 날짜에 이미 출석 체크가 있는지 확인)
    public void validateDuplicateAttendance(User user, StudyGroup studyGroup, LocalDate attendanceDate) {

        boolean exists = attendanceRepository.existsByUserAndStudyGroupAndAttendanceDate(
                user, studyGroup, attendanceDate);

        if (exists) {
            throw new RestApiException(ErrorCode.DUPLICATE_ATTENDANCE_CHECK);
        }
    }

    // ========== 과제 관련 검증 ==========

    //계획 중 과제가 포함인지 확인
    public void validateAssignmentSubmissionAllowed(Long planId){

        log.info("validate Assignment Submission Allowed");

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RestApiException(ErrorCode.PLAN_NOT_FOUND));

        if(!plan.isHasAssignment()){
            throw new RestApiException(ErrorCode.ASSIGNMENT_NOT_FOUND);
        }
    }

    // 중복된 과제 제출인지 확인
    public void validateDuplicateSubmission(Long userId, Long assignmentId) {

        boolean alreadySubmitted = submissionRepository
                .existsBySubmitterUserIdAndPlanPlanId(userId, assignmentId);

        if (alreadySubmitted) {
            log.warn("User {} attempted duplicate submission for assignment {}", userId, assignmentId);
            throw new RestApiException(ErrorCode.DUPLICATE_SUBMISSION);
        }
    }

    // 제출물 접근 검증 (과제와 제출물 존재, 관계 검증)
    public AssignmentSubmission validateSubmissionAccess(Long assignmentId, Long submitId) {

        validatePlan(assignmentId);
        AssignmentSubmission submission = validateSubmission(submitId);
        validateAssignmentIdInAssignmentSubmission(assignmentId, submission);
        return submission;
    }

    // 제출물이 해당 과제에 속하는지 검증
    public void validateAssignmentIdInAssignmentSubmission(Long assignmentId, AssignmentSubmission assignmentSubmission) {

        if (!assignmentSubmission.getPlan().getPlanId().equals(assignmentId)) {
            log.warn("Assignment id mismatch between assignment and assignment submission");
            throw new RestApiException(ErrorCode.SUBMISSION_ASSIGNMENT_MISMATCH);
        }
    }

    // 제출된 과제와 제출한 유저가 동일인물인지 검증
    public void validateSubmissionOwnership(AssignmentSubmission assignmentSubmission, Long userId) {

        if (!assignmentSubmission.getSubmitter().getUserId().equals(userId)) {
            log.warn("User {} attempted to update submission {} which is not theirs",
                    userId, assignmentSubmission.getSubmitter().getUserId());
            throw new RestApiException(ErrorCode.UNAUTHORIZED_ACCESS_SUBMISSION);
        }
    }

    // 과제 피드백이 이미 존재하는지 검증
    public void validateFeedbackNotExists(AssignmentSubmission submission) {

        if (submission.getFeedback() != null && !submission.getFeedback().trim().isEmpty()) {
            log.warn("Feedback already exists for submission {}", submission.getSubmissionId());
            throw new RestApiException(ErrorCode.FEEDBACK_ALREADY_EXISTS);
        }
    }

    // 과제 피드백이 존재하는지 검증
    public void validateFeedbackExists(AssignmentSubmission submission) {

        if (submission.getFeedback() == null || submission.getFeedback().trim().isEmpty()) {
            log.warn("No feedback exists for submission {}", submission.getSubmissionId());
            throw new RestApiException(ErrorCode.FEEDBACK_NOT_FOUND);
        }
    }

    // ========== 내용 검증 ==========

    // 댓글 내용이 비어있지 않은지 확인
    public void validateComment(String content) {

        if (content == null || content.trim().isEmpty()) {
            throw new RestApiException(ErrorCode.PLAN_COMMENT_REQUIRED);
        }
    }

    // 과제 제출 내용에 문제가 없는지 검증 (내용, URL 보안, 악성 코드)
    public void validateSubmissionContent(String content) {

        if (content == null || content.trim().isEmpty()) {
            throw new RestApiException(ErrorCode.SUBMISSION_CONTENT_REQUIRED);
        }

        List<String> urls = extractUrls(content);
        for (String url : urls) {
            validateUrlSecurity(url);
        }

        validateMaliciousContent(content);
    }

    // 피드백 내용이 비어있지 않은지 확인
    public void validateFeedbackContent(String feedback) {

        if (feedback == null || feedback.trim().isEmpty()) {
            throw new RestApiException(ErrorCode.FEEDBACK_CONTENT_REQUIRED);
        }
    }

    //시작일자가 끝일자보다 늦은 경우 에러 처리
    public void validateDateRange(LocalDate startDate, LocalDate endDate){

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {  // startDate가 endDate보다 늦으면 에러
                throw new RestApiException(ErrorCode.INVALID_DATE_RANGE);
            }
        }
    }

    // ========== 보안 검증 ==========

    // 허용된 도메인인지 확인하기 (URL 보안 검증)
    public void validateUrlSecurity(String urlString) {

        try {
            URL url = new URL(urlString);
            String urlHost = url.getHost();

            if (urlHost == null) {
                throw new RestApiException(ErrorCode.INVALID_URL_FORMAT);
            }

            urlHost = urlHost.toLowerCase();

            String finalUrlHost = urlHost;

            boolean isAllowed = Arrays.stream(ALLOWED_DOMAINS)
                    .anyMatch(domain -> finalUrlHost.endsWith("." + domain) || finalUrlHost.equals(domain));

            if (!isAllowed) {
                log.warn("URL {} is not allowed. Allowed domains: {}", urlString, Arrays.toString(ALLOWED_DOMAINS));
                throw new RestApiException(ErrorCode.UNAUTHORIZED_DOMAIN);
            }
        } catch (MalformedURLException e) {
            log.warn("URL {} is invalid", urlString);
            throw new RestApiException(ErrorCode.INVALID_URL_FORMAT);
        }
    }

    // XSS, SQL INJECTION 검사
    public void validateMaliciousContent(String content) {

        String lowerContent = content.toLowerCase();

        // XSS 패턴 검증
        for (String pattern : XSS_PATTERNS) {
            if (lowerContent.contains(pattern)) {
                log.warn("Malicious XSS pattern detected: {}", pattern);
                throw new RestApiException(ErrorCode.WARNING_CONTENT);
            }
        }

        // SQL Injection 패턴 검증
        for (String pattern : SQL_INJECTION_PATTERNS) {
            if (lowerContent.contains(pattern)) {
                log.warn("SQL injection pattern detected: {}", pattern);
                throw new RestApiException(ErrorCode.WARNING_CONTENT);
            }
        }
    }

    // ========== 활동 글 관련 검증 ==========

    // 중복 주차 생성 방지 검증 (생성시)
    public void validateDuplicateWeekForCreate(StudyGroup studyGroup, String week) {

        if (week == null || week.trim().isEmpty()) {
            return; // 주차가 없으면 검증하지 않음
        }

        boolean exists = studyActivityRepository.existsByStudyGroupAndWeek(studyGroup, week);

        if (exists) {
            log.warn("Duplicate week creation attempt: StudyGroup ID {}, Week {}",
                    studyGroup.getStudyId(), week);
            throw new RestApiException(ErrorCode.DUPLICATE_WEEK);
        }
    }

    // 중복 주차 수정 방지 검증 (수정시)
    public void validateDuplicateWeekForUpdate(StudyGroup studyGroup, String week, Long currentActivityId) {

        if (week == null || week.trim().isEmpty()) {
            return; // 주차가 없으면 검증하지 않음
        }

        boolean exists = studyActivityRepository.existsByStudyGroupAndWeekAndActivityIdNot(
                studyGroup, week, currentActivityId);

        if (exists) {
            log.warn("Duplicate week update attempt: StudyGroup ID {}, Week {}, Current Activity ID {}",
                    studyGroup.getStudyId(), week, currentActivityId);
            throw new RestApiException(ErrorCode.DUPLICATE_WEEK);
        }
    }

    // ========== 유틸리티 메서드들 ==========

    // 문자열에서 URL 추출하기
    private List<String> extractUrls(String content) {

        List<String> urls = new ArrayList<>();
        String[] splittedContent = content.split("\\s+");

        for (String word : splittedContent) {
            if (isValidUrl(word)) {
                urls.add(word);
            }
        }
        return urls;
    }

    // 유효한 URL인지 확인
    private boolean isValidUrl(String urlString) {

        // URL 패턴에 맞는지 먼저 검사하기
        if (!URL_PATTERN.matcher(urlString).matches()) {
            return false;
        }

        // 실제로 있는 URL인지 검사하기
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}