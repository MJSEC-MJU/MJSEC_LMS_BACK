package com.mjsec.lms.service;

import com.mjsec.lms.domain.*;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentCommentRepository assignmentCommentRepository;

    private static final String[] ALLOWED_DOMAINS =
            {"velog.io", "tistory.com", "blog.naver.com"};

    private static final String[] XSS_PATTERNS = {
            "<script", "javascript:", "onload=", "onerror=", "onclick=",
            "onmouseover=", "eval(", "alert(", "document.cookie"
    };

    private static final String[] SQL_INJECTION_PATTERNS = {
            "union select", "drop table", "delete from", "insert into",
            "update set", "' or '1'='1", "-- ", "/*"
    };

    private static final Pattern URL_PATTERN = Pattern.compile("^https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)$");

    public AssignmentService(AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             StudyGroupRepository studyGroupRepository,
                             GroupMemberRepository groupMemberRepository,
                             SubmissionRepository submissionRepository,
                             AssignmentCommentRepository assignmentCommentRepository) {

        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentCommentRepository = assignmentCommentRepository;
    }

    // 과제 등록하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse createAssignment(Long groupId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        User user = validateMentoAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);

        log.info("User {} is confirmed as MENTO of StudyGroup: {}", user.getUserId(), studyGroup.getName());

        Assignment assignment = Assignment.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .creator(user)
                .studyGroup(studyGroup)
                .createdAt(LocalDateTime.now())
                .build();

        assignmentRepository.save(assignment);
        log.info("Assignment created successfully: {}", assignment);

        return createDetailAssignmentResponse(assignment);
    }

    // 전체 과제 조회하기
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignment(Long groupId, Long currentUserStudentNumber) {

        log.info("getAssignment called");

        validateBasicAccess(groupId, currentUserStudentNumber);

        List<Assignment> assignmentList = assignmentRepository.findAllByStudyGroup_StudyId(groupId);
        log.info("Found {} assignments in StudyGroup", assignmentList.size());

        List<AssignmentResponse> assignmentResponses = assignmentList.stream()
                .map(this::createResponse)
                .collect(Collectors.toList());

        log.info("get Assignment Successfully!");
        return assignmentResponses;
    }

    // 과제 상세 조회하기
    @Transactional(readOnly = true)
    public DetailAssignmentResponse getDetailAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getDetailAssignment called");

        validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);
        return createDetailAssignmentResponse(assignment);
    }

    //과제 수정하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse updateAssignment(Long groupId, Long assignmentId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("updateAssignment called");

        validateMentoAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        updateAssignmentData(assignment, dto);
        return createDetailAssignmentResponse(assignment);
    }

    // 과제 삭제하기 (멘토만 가능함.)
    @Transactional
    public void deleteAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("deleteAssignment called");

        validateMentoAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        assignmentRepository.delete(assignment);
        log.info("Assignment deleted successfully: {}", assignment);
    }

    //과제 제출하기 (멘티만)
    @Transactional
    public SubmissionResponse submitAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber, SubmissionDto dto, String ipAddress) {

        log.info("submitAssignment called");

        User user = validateMenteeAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);

        //과제 내용 확인하기
        validateSubmissionContent(dto.getContent());

        //과제 제출 중복 여부 체크
        validateDuplicateSubmission(user.getUserId(), assignmentId);

        AssignmentSubmission assignmentSubmission = AssignmentSubmission.builder()
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .password(dto.getPassword())
                .submitter(user)
                .assignment(assignment)
                .submitterIp(ipAddress)
                .build();

        submissionRepository.save(assignmentSubmission);
        log.info("Assignment submission saved successfully: {}", assignmentSubmission);

        return createSubmissionResponse(assignmentSubmission);
    }

    //과제 제출 리스트 확인하기 (멘토/멘티)
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionList(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getSubmissionList called");

        validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);

        return submissionRepository.findAssignmentSubmissionsByAssignmentAssignId(assignment.getAssignId())
                .stream()
                .map(this::createSubmissionResponse)
                .collect(Collectors.toList());
    }

    /**
     * 유저별 과제 제출 확인하기 (멘토/멘티 둘 다 가능)
     * 멘티 (자기 자신 과제만 조회 가능)
     * 멘토 (나머지도 다 가능)
     */
    @Transactional(readOnly = true)
    public DetailSubmissionResponse getDetailedSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("getDetailedSubmission called");

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateAssignment(assignmentId);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        GroupMemberRole role = validateUserRole(user.getUserId(), groupId);

        if(role == GroupMemberRole.MENTEE) {
            if (!assignmentSubmission.getSubmitter().getUserId().equals(user.getUserId())) {
                log.warn("User {} attempted to access submission {} which is not theirs",
                        user.getUserId(), submitId);
                throw new RestApiException(ErrorCode.UNAUTHORIZED_ACCESS_SUBMISSION);
            }
        }
        else if(role == GroupMemberRole.MENTO) {
            log.info("Mento {} accessing submission {}", user.getUserId(), submitId);
        }

        return createDetailSubmissionResponse(assignmentSubmission);
    }

    //과제 제출 수정하기
    @Transactional
    public DetailSubmissionResponse updateAssignmentSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionDto dto) {

        log.info("updateAssignmentSubmission called");

        User user = validateMenteeAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        return updateSubmitData(assignmentSubmission, dto);
    }

    //과제 제출 삭제하기
    @Transactional
    public void deleteAssignmentSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteAssignmentSubmission called");

        User user = validateMenteeAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        submissionRepository.delete(assignmentSubmission);
        log.info("Assignment submission deleted successfully: {}", assignmentSubmission);
    }

    //과제 댓글 생성
    @Transactional
    public AssignmentCommentResponse createAssignmentComment(Long groupId, Long assignmentId, Long currentUserStudentNumber, AssignmentCommentDto dto) {

        log.info("createAssignmentComment called");

        User user = validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validateAssignment(assignmentId);
        validateComment(dto.getContent());

        AssignmentComment assignmentComment = AssignmentComment.builder()
                .assignment(assignment)
                .author(user)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        assignmentCommentRepository.save(assignmentComment);

        return createAssignmentCommentResponse(assignmentComment);
    }

    //멘토가 과제 피드백 남기기
    @Transactional
    public void leaveFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("leaveFeedback called");

        validateMentoAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        validateFeedbackNotExists(assignmentSubmission);

        leaveFeedbackData(assignmentSubmission, dto);
    }

    @Transactional
    public SubmissionFeedbackDto updateFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("updateFeedback called");

        validateMentoAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        validateFeedbackExists(assignmentSubmission);

        return updateFeedbackData(assignmentSubmission, dto);
    }

    @Transactional
    public void deleteFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteFeedback called");

        validateMentoAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validateSubmissionAccess(assignmentId, submitId);
        validateFeedbackExists(assignmentSubmission);

        assignmentSubmission.setFeedback(null);
        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Feedback deleted successfully for submission: {}", assignmentSubmission.getSubmissionId());
    }

    /**
     * === 공통 검증 메서드들 ===
     */

    // 기본 접근 검증 (사용자, 스터디 그룹 존재, 멤버십)
    private User validateBasicAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        return user;
    }

    // 멘토 접근 검증
    private User validateMentoAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateMentoRole(user.getUserId(), groupId);
        return user;
    }

    // 멘티 접근 검증
    private User validateMenteeAccess(Long groupId, Long currentUserStudentNumber) {

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        validateMenteeRole(user.getUserId(), groupId);
        return user;
    }

    // 제출물 접근 검증 (과제와 제출물 존재, 관계 검증)
    private AssignmentSubmission validateSubmissionAccess(Long assignmentId, Long submitId) {

        validateAssignment(assignmentId);
        AssignmentSubmission submission = validateSubmission(submitId);
        validateAssignmentIdInAssignmentSubmission(assignmentId, submission);
        return submission;
    }

    private void validateComment(String content) {

        if(content == null || content.trim().isEmpty()){
            throw new RestApiException(ErrorCode.ASSIGNMENT_COMMENT_REQUIRED);
        }
    }

    // 제출물이 해당 과제에 속하는지 검증
    private void validateAssignmentIdInAssignmentSubmission(Long assignmentId, AssignmentSubmission assignmentSubmission) {

        if(!assignmentSubmission.getAssignment().getAssignId().equals(assignmentId)) {
            log.warn("Assignment id mismatch between assignment and assignment submission");
            throw new RestApiException(ErrorCode.SUBMISSION_ASSIGNMENT_MISMATCH);
        }
    }

    //제출된 과제와 제출한 유저가 동일인물인지 검증
    private void validateSubmissionOwnership(AssignmentSubmission assignmentSubmission, Long userId){

        if(!assignmentSubmission.getSubmitter().getUserId().equals(userId)) {
            log.warn("User {} attempted to update submission {} which is not theirs",
                    userId, assignmentSubmission.getSubmitter().getUserId());
            throw new RestApiException(ErrorCode.UNAUTHORIZED_ACCESS_SUBMISSION);
        }
    }

    //과제 제출 내용에 문제가 없는지 검증
    private void validateSubmissionContent(String content) {

        if(content == null || content.trim().isEmpty()) {
            throw new RestApiException(ErrorCode.SUBMISSION_CONTENT_REQUIRED);
        }

        List<String> urls = extractUrls(content);
        for(String url : urls) {
            validateUrlSecurity(url);
        }

        validateMaliciousContent(content);
    }

    //허용된 도메인인지 확인하기
    private void validateUrlSecurity(String urlString){

        try{
            URL url = new URL(urlString);
            String urlHost = url.getHost();

            if (urlHost == null) {
                throw new RestApiException(ErrorCode.INVALID_URL_FORMAT);
            }

            urlHost = urlHost.toLowerCase();

            String finalUrlHost = urlHost;

            boolean isAllowed = Arrays.stream(ALLOWED_DOMAINS)
                    .anyMatch(domain -> finalUrlHost.endsWith("."+domain) || finalUrlHost.equals(domain));

            if(!isAllowed){
                log.warn("URL {} is not allowed. Allowed domains: {}", urlString, Arrays.toString(ALLOWED_DOMAINS));
                throw new RestApiException(ErrorCode.UNAUTHORIZED_DOMAIN);
            }
        } catch(MalformedURLException e){
            log.warn("URL {} is invalid", urlString);
            throw new RestApiException(ErrorCode.INVALID_URL_FORMAT);
        }
    }

    //XSS, SQL INJECTION 검사
    private void validateMaliciousContent(String content) {

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

    //URL 뽑아내기
    private List<String> extractUrls(String content) {

        List<String> urls = new ArrayList<>();
        String[] splittedContent = content.split("\\s+");

        for(String word : splittedContent) {
            if(isValidUrl(word)){
                urls.add(word);
            }
        }
        return urls;
    }

    //유효한 URL인지 확인
    private boolean isValidUrl(String urlString) {

        //URL 패턴에 맞는지 먼저 검사하기
        if(!URL_PATTERN.matcher(urlString).matches()) {
            return false;
        }

        //실제로 있는 URL인지 검사하기
        try{
            new URL(urlString);
            return true;
        } catch (MalformedURLException e){
            return false;
        }
    }

    //사용자 존재 여부를 확인하고 User 객체를 반환
    private User validateUser(Long studentNumber) {

        return userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));
    }

    //스터디 그룹 존재 여부를 확인하고 StudyGroup 객체를 반환
    private StudyGroup validateStudyGroup(Long groupId) {

        return studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));
    }

    //사용자가 해당 스터디 그룹의 멘티인지 확인
    private void validateMenteeRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if(userRole != GroupMemberRole.MENTEE) {
            log.warn("User {} does not have MENTEE role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTEE_ROLE);
        }
    }

    //사용자가 해당 스터디 그룹의 멘토인지 확인
    private void validateMentoRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if (userRole != GroupMemberRole.MENTO) {
            log.warn("User {} does not have MENTO role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTO_ROLE);
        }
    }

    //사용자가 멘토인지 멘티인지 확인하기
    private GroupMemberRole validateUserRole(Long userId, Long groupId) {

        return groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));
    }

    //사용자가 해당 스터디 그룹의 멤버인지 확인
    private void validateGroupMembership(User user, StudyGroup studyGroup) {

        groupMemberRepository.findByUserAndStudyGroup(user, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));
    }

    //Assignment(과제) 존재 여부를 확인하고 Assignment 객체를 반환
    private Assignment validateAssignment(Long assignmentId) {

        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    //과제 제출 중복 여부 확인하기
    private void validateDuplicateSubmission(Long userId, Long assignmentId) {

        boolean alreadySubmitted = submissionRepository
                .existsBySubmitterUserIdAndAssignmentAssignId(userId, assignmentId);

        if (alreadySubmitted) {
            log.warn("User {} attempted duplicate submission for assignment {}", userId, assignmentId);
            throw new RestApiException(ErrorCode.DUPLICATE_SUBMISSION);
        }
    }

    //제출한 과제가 존재하는지 검증
    private AssignmentSubmission validateSubmission(Long submitId) {

        return submissionRepository.findById(submitId)
                .orElseThrow(() -> new RestApiException(ErrorCode.SUBMISSION_NOT_FOUND));
    }

    //과제 피드백이 이미 존재하는지 검증
    private void validateFeedbackNotExists(AssignmentSubmission submission) {

        if (submission.getFeedback() != null && !submission.getFeedback().trim().isEmpty()) {
            log.warn("Feedback already exists for submission {}", submission.getSubmissionId());
            throw new RestApiException(ErrorCode.FEEDBACK_ALREADY_EXISTS);
        }
    }

    private void validateFeedbackExists(AssignmentSubmission submission) {

        if (submission.getFeedback() == null || submission.getFeedback().trim().isEmpty()) {
            log.warn("No feedback exists for submission {}", submission.getSubmissionId());
            throw new RestApiException(ErrorCode.FEEDBACK_NOT_FOUND);
        }
    }

    /**
     * === 데이터 처리 메서드들 ===
     */

    //Assignment 데이터를 업데이트
    private void updateAssignmentData(Assignment assignment, AssignmentDto dto) {

        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            assignment.setTitle(dto.getTitle());
        }

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            assignment.setContent(dto.getContent());
        }

        if (dto.getStartDate() != null) {
            assignment.setStartDate(dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            assignment.setEndDate(dto.getEndDate());
        }

        assignment.setUpdatedAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
        log.info("Assignment updated successfully: {}", assignment);
    }

    //제출한 과제 내용을 수정
    private DetailSubmissionResponse updateSubmitData(AssignmentSubmission assignmentSubmission, SubmissionDto dto) {

        // 수정할 데이터가 없으면 예외
        if((dto.getContent() == null || dto.getContent().trim().isEmpty()) &&
                (dto.getPassword() == null || dto.getPassword().trim().isEmpty())) {
            throw new RestApiException(ErrorCode.SUBMISSION_CONTENT_REQUIRED);
        }

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            validateSubmissionContent(dto.getContent());
            assignmentSubmission.setContent(dto.getContent());
        }

        if(dto.getPassword() != null && !dto.getPassword().trim().isEmpty()){
            assignmentSubmission.setPassword(dto.getPassword());
        }

        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Assignment submission updated successfully: {}", assignmentSubmission.getSubmissionId());

        return createDetailSubmissionResponse(assignmentSubmission);
    }

    /**
     * === DTO 변환 메서드들 ===
     */

    //Assignment를 DetailAssignmentResponse로 변환
    private DetailAssignmentResponse createDetailAssignmentResponse(Assignment assignment) {

        List<AssignmentComment> assignmentCommentList = assignmentCommentRepository.findAllByAssignmentAssignId(assignment.getAssignId());
        List<AssignmentCommentResponse> commentList = assignmentCommentList.stream()
                .map(this::createAssignmentCommentResponse)
                .collect(Collectors.toList());

        return DetailAssignmentResponse.builder()
                .assignmentId(assignment.getAssignId())
                .title(assignment.getTitle())
                .content(assignment.getContent())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .creatorName(assignment.getCreator().getName())
                .createdAt(assignment.getCreatedAt())
                .commentCount(assignmentCommentList.size())
                .commentList(commentList)
                .updatedAt(assignment.getUpdatedAt())
                .build();
    }

    //Assignment를 AssignmentResponse로 변환
    private AssignmentResponse createResponse(Assignment assignment) {

        return AssignmentResponse.builder()
                .assignmentId(assignment.getAssignId())
                .title(assignment.getTitle())
                .content(assignment.getContent())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .createdAt(assignment.getCreatedAt())
                .build();
    }

    //과제 제출 반환용 Dto로 변환하기
    private SubmissionResponse createSubmissionResponse(AssignmentSubmission assignmentSubmission) {

        return SubmissionResponse.builder()
                .submissionId(assignmentSubmission.getSubmissionId())
                .content(assignmentSubmission.getContent())
                .creatorName(assignmentSubmission.getSubmitter().getName())
                .createdAt(assignmentSubmission.getCreatedAt())
                .build();
    }

    //과제 제출 (디테일) 반환용
    private DetailSubmissionResponse createDetailSubmissionResponse(AssignmentSubmission assignmentSubmission) {

        return DetailSubmissionResponse.builder()
                .submissionId(assignmentSubmission.getSubmissionId())
                .content(assignmentSubmission.getContent())
                .creatorName(assignmentSubmission.getSubmitter().getName())
                .createdAt(assignmentSubmission.getCreatedAt())
                .updatedAt(assignmentSubmission.getUpdatedAt())
                .password(assignmentSubmission.getPassword())
                .feedback(assignmentSubmission.getFeedback())
                .build();
    }

    //과제 피드백 남기기
    private void leaveFeedbackData(AssignmentSubmission submission, SubmissionFeedbackDto dto) {

        if(dto.getFeedback() != null && !dto.getFeedback().trim().isEmpty()){
            submission.setFeedback(dto.getFeedback());
        }
        else{
            throw new RestApiException(ErrorCode.FEEDBACK_CONTENT_REQUIRED);
        }

        submission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        log.info("Feedback saved successfully for submission: {}", submission.getSubmissionId());
    }

    //과제 댓글 Response 생성해서 반환하기
    private AssignmentCommentResponse createAssignmentCommentResponse(AssignmentComment assignmentComment) {

        return AssignmentCommentResponse.builder()
                .commentId(assignmentComment.getCommentId())
                .content(assignmentComment.getContent())
                .assignmentId(assignmentComment.getAssignment().getAssignId())
                .creatorName(assignmentComment.getAuthor().getName())
                .createdAt(assignmentComment.getCreatedAt())
                .build();
    }

    //과제 피드백 수정해서 dto 반환하기
    private SubmissionFeedbackDto updateFeedbackData(AssignmentSubmission submission, SubmissionFeedbackDto dto) {

        if(dto.getFeedback() != null && !dto.getFeedback().trim().isEmpty()){
            submission.setFeedback(dto.getFeedback());
            submission.setUpdatedAt(LocalDateTime.now());
        }
        else {
            throw new RestApiException(ErrorCode.FEEDBACK_CONTENT_REQUIRED);
        }

        submissionRepository.save(submission);

        log.info("Feedback updated successfully for submission: {}", submission.getSubmissionId());

        return SubmissionFeedbackDto.builder()
                .feedback(submission.getFeedback())
                .build();
    }
}