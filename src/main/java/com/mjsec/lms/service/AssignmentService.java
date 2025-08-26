package com.mjsec.lms.service;

import com.mjsec.lms.domain.*;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AssignmentCommentRepository assignmentCommentRepository;
    private final ValidationUtils validationUtils;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             SubmissionRepository submissionRepository,
                             AssignmentCommentRepository assignmentCommentRepository,
                             ValidationUtils validationUtils) {

        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.assignmentCommentRepository = assignmentCommentRepository;
        this.validationUtils = validationUtils;
    }

    // 과제 등록하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse createAssignment(Long groupId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        User user = validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        StudyGroup studyGroup = validationUtils.validateStudyGroup(groupId);

        log.info("User {} is confirmed as MENTOR of StudyGroup: {}", user.getUserId(), studyGroup.getName());

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
    public List<AssignmentResponse> getAssignment(Long groupId, Long currentUserStudentNumber) {

        log.info("getAssignment called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);

        List<Assignment> assignmentList = assignmentRepository.findAllByStudyGroup_StudyId(groupId);
        log.info("Found {} assignments in StudyGroup", assignmentList.size());

        List<AssignmentResponse> assignmentResponses = assignmentList.stream()
                .map(this::createResponse)
                .collect(Collectors.toList());

        log.info("get Assignment Successfully!");
        return assignmentResponses;
    }

    // 과제 상세 조회하기
    public DetailAssignmentResponse getDetailAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getDetailAssignment called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);
        return createDetailAssignmentResponse(assignment);
    }

    // 과제 수정하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse updateAssignment(Long groupId, Long assignmentId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("updateAssignment called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        updateAssignmentData(assignment, dto);
        return createDetailAssignmentResponse(assignment);
    }

    // 과제 삭제하기 (멘토만 가능함.)
    @Transactional
    public void deleteAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("deleteAssignment called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        assignmentRepository.delete(assignment);
        log.info("Assignment deleted successfully: {}", assignment);
    }

    // 과제 제출하기 (멘티만)
    @Transactional
    public SubmissionResponse submitAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber, SubmissionDto dto, String ipAddress) {

        log.info("submitAssignment called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);

        // 과제 내용 확인하기
        validationUtils.validateSubmissionContent(dto.getContent());

        // 과제 제출 중복 여부 체크
        validationUtils.validateDuplicateSubmission(user.getUserId(), assignmentId);

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

    // 과제 제출 리스트 확인하기 (멘토/멘티)
    public List<SubmissionResponse> getSubmissionList(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getSubmissionList called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);

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
    public DetailSubmissionResponse getDetailedSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("getDetailedSubmission called");

        User user = validationUtils.validateUser(currentUserStudentNumber);
        validationUtils.validateStudyGroup(groupId);
        validationUtils.validateAssignment(assignmentId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        GroupMemberRole role = validationUtils.validateUserRole(user.getUserId(), groupId);

        if(role == GroupMemberRole.MENTEE) {
            if (!assignmentSubmission.getSubmitter().getUserId().equals(user.getUserId())) {
                log.warn("User {} attempted to access submission {} which is not theirs",
                        user.getUserId(), submitId);
                throw new RestApiException(ErrorCode.UNAUTHORIZED_ACCESS_SUBMISSION);
            }
        }
        else if(role == GroupMemberRole.MENTOR) {
            log.info("Mentor {} accessing submission {}", user.getUserId(), submitId);
        }

        return createDetailSubmissionResponse(assignmentSubmission);
    }

    // 과제 제출 수정하기
    @Transactional
    public DetailSubmissionResponse updateAssignmentSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionDto dto) {

        log.info("updateAssignmentSubmission called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        return updateSubmitData(assignmentSubmission, dto);
    }

    // 과제 제출 삭제하기
    @Transactional
    public void deleteAssignmentSubmission(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteAssignmentSubmission called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        submissionRepository.delete(assignmentSubmission);
        log.info("Assignment submission deleted successfully: {}", assignmentSubmission);
    }

    // 과제 댓글 생성
    @Transactional
    public AssignmentCommentResponse createAssignmentComment(Long groupId, Long assignmentId, Long currentUserStudentNumber, AssignmentCommentDto dto) {

        log.info("createAssignmentComment called");

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        Assignment assignment = validationUtils.validateAssignment(assignmentId);
        validationUtils.validateComment(dto.getContent());

        AssignmentComment assignmentComment = AssignmentComment.builder()
                .assignment(assignment)
                .author(user)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();

        assignmentCommentRepository.save(assignmentComment);

        return createAssignmentCommentResponse(assignmentComment);
    }

    // 멘토가 과제 피드백 남기기
    @Transactional
    public void leaveFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("leaveFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        validationUtils.validateFeedbackNotExists(assignmentSubmission);

        leaveFeedbackData(assignmentSubmission, dto);
    }

    @Transactional
    public SubmissionFeedbackDto updateFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("updateFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        return updateFeedbackData(assignmentSubmission, dto);
    }

    @Transactional
    public void deleteFeedback(Long groupId, Long assignmentId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(assignmentId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        assignmentSubmission.setFeedback(null);
        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Feedback deleted successfully for submission: {}", assignmentSubmission.getSubmissionId());
    }

    /**
     * === 데이터 처리 메서드들 ===
     */

    // Assignment 데이터를 업데이트
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

    // 제출한 과제 내용을 수정
    private DetailSubmissionResponse updateSubmitData(AssignmentSubmission assignmentSubmission, SubmissionDto dto) {

        // 수정할 데이터가 없으면 예외
        if((dto.getContent() == null || dto.getContent().trim().isEmpty()) &&
                (dto.getPassword() == null || dto.getPassword().trim().isEmpty())) {
            throw new RestApiException(ErrorCode.SUBMISSION_CONTENT_REQUIRED);
        }

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            validationUtils.validateSubmissionContent(dto.getContent());
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

    // Assignment를 DetailAssignmentResponse로 변환
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

    // Assignment를 AssignmentResponse로 변환
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

    // 과제 제출 반환용 Dto로 변환하기
    private SubmissionResponse createSubmissionResponse(AssignmentSubmission assignmentSubmission) {

        return SubmissionResponse.builder()
                .submissionId(assignmentSubmission.getSubmissionId())
                .content(assignmentSubmission.getContent())
                .creatorName(assignmentSubmission.getSubmitter().getName())
                .createdAt(assignmentSubmission.getCreatedAt())
                .build();
    }

    // 과제 제출 (디테일) 반환용
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

    // 과제 피드백 남기기
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

    // 과제 댓글 Response 생성해서 반환하기
    private AssignmentCommentResponse createAssignmentCommentResponse(AssignmentComment assignmentComment) {

        return AssignmentCommentResponse.builder()
                .commentId(assignmentComment.getCommentId())
                .content(assignmentComment.getContent())
                .assignmentId(assignmentComment.getAssignment().getAssignId())
                .creatorName(assignmentComment.getAuthor().getName())
                .createdAt(assignmentComment.getCreatedAt())
                .build();
    }

    // 과제 피드백 수정해서 dto 반환하기
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