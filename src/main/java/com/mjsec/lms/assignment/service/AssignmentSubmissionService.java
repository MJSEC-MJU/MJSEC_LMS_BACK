package com.mjsec.lms.assignment.service;

import com.mjsec.lms.assignment.domain.AssignmentSubmission;
import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.assignment.dto.SubmissionResponse;
import com.mjsec.lms.assignment.dto.SubmissionDto;
import com.mjsec.lms.assignment.dto.SubmissionFeedbackDto;
import com.mjsec.lms.assignment.dto.DetailSubmissionResponse;
import com.mjsec.lms.assignment.dto.SubmissionStatisticsResponse;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.studygroup.domain.type.GroupMemberRole;
import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import com.mjsec.lms.user.domain.type.UserRole;
import com.mjsec.lms.common.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssignmentSubmissionService {

    private final ValidationUtils validationUtils;
    private final SubmissionRepository submissionRepository;
    private final GroupMemberRepository groupMemberRepository;

    AssignmentSubmissionService(ValidationUtils validationUtils, SubmissionRepository submissionRepository, GroupMemberRepository groupMemberRepository){

        this.validationUtils = validationUtils;
        this.submissionRepository = submissionRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    // 과제 제출하기 (멘티만)
    @Transactional
    public SubmissionResponse submitAssignment(Long groupId, Long planId, Long currentUserStudentNumber, SubmissionDto dto, String ipAddress) {

        log.info("submitAssignment called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        Plan plan = validationUtils.validatePlan(planId);
        
        //과제 제출 가능한 계획인지 확인
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        // 과제 제출 내용 확인하기
        validationUtils.validateSubmissionContent(dto.getContent());

        //과제 기한 검사
       // validationUtils.validateAssignmentDeadline(plan);

        // 과제 제출 중복 여부 체크
        validationUtils.validateDuplicateSubmission(user.getUserId(), planId);

        AssignmentSubmission assignmentSubmission = AssignmentSubmission.builder()
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .password(dto.getPassword())
                .submitter(user)
                .plan(plan)
                .status(SubmissionStatus.SUBMITTED)
                .submitterIp(ipAddress)
                .build();

        submissionRepository.save(assignmentSubmission);
        log.info("Assignment submission saved successfully: {}", assignmentSubmission);

        return createSubmissionResponse(assignmentSubmission);
    }

    // 과제 제출 리스트 확인하기 (멘토/멘티)
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionList(Long groupId, Long planId, Long currentUserStudentNumber) {

        log.info("getSubmissionList called");

        validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        return submissionRepository.findAssignmentSubmissionsByPlanPlanId(plan.getPlanId())
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
    public DetailSubmissionResponse getDetailedSubmission(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber) {

        log.info("getDetailedSubmission called");

        User user = validationUtils.validateUser(currentUserStudentNumber);

        validationUtils.validateStudyGroup(groupId);
        validationUtils.validatePlan(planId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        GroupMemberRole role = (user.getRole() == UserRole.ROLE_ADMIN)
                ? GroupMemberRole.MENTOR
                : validationUtils.validateUserRole(user.getUserId(), groupId);

        validationUtils.validateSubmissionAccessByStatus(assignmentSubmission, user.getUserId(), role);

        return createDetailSubmissionResponse(assignmentSubmission);
    }

    // 과제 제출 수정하기
    @Transactional
    public DetailSubmissionResponse updateAssignmentSubmission(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionDto dto) {
        log.info("updateAssignmentSubmission called for user: {}, submission: {}", currentUserStudentNumber, submitId);

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);

        // 강화된 검증 로직
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        // 마감일 검증 추가 (수정도 마감일 이후 불가)
        Plan plan = validationUtils.validatePlan(planId);
        //validationUtils.validateAssignmentDeadline(plan);

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        //수정 가능한 상태인지 확인
        validationUtils.validateSubmissionStatusForUpdate(assignmentSubmission);

        // 내용 검증 강화
        if (dto.getContent() != null && !dto.getContent().trim().isEmpty()) {
            validationUtils.validateSubmissionContent(dto.getContent());
        }

        //수정 필요 상태에서 재제출 시 상태를 SUBMITTED로 변경
        if (assignmentSubmission.getStatus() == SubmissionStatus.REVISION_REQUIRED) {
            assignmentSubmission.setStatus(SubmissionStatus.SUBMITTED);
            log.info("Status changed from REVISION_REQUIRED to SUBMITTED for submission: {}", submitId);
        }

        return updateSubmitData(assignmentSubmission, dto);
    }

    // 과제 제출 삭제하기
    @Transactional
    public void deleteAssignmentSubmission(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteAssignmentSubmission called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);

        // 검증 로직
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        validationUtils.validateSubmissionStatusForDelete(assignmentSubmission);

        submissionRepository.delete(assignmentSubmission);
        log.info("Assignment submission deleted successfully: {}", assignmentSubmission);
    }

    // 멘토가 과제 피드백 남기기
    @Transactional
    public void leaveFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("leaveFeedback called for user: {}, submission: {}", currentUserStudentNumber, submitId);

        // 강화된 검증 로직
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        validationUtils.validateSubmissionStatus(dto.getStatus().toString());

        //피드백 내용 + 제출 상태 검증
        validationUtils.validateFeedbackContentAndStatus(dto.getFeedback(), dto.getStatus());

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackNotExists(assignmentSubmission);

        validationUtils.validateSubmissionStatusForFeedback(assignmentSubmission);

        leaveFeedbackData(assignmentSubmission, dto);
    }

    //과제 피드백 수정하기
    @Transactional
    public SubmissionFeedbackDto updateFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("updateFeedback called for user: {}, submission: {}", currentUserStudentNumber, submitId);

        // 강화된 검증 로직
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        validationUtils.validateSubmissionStatus(dto.getStatus().toString());

        validationUtils.validateFeedbackContentAndStatus(dto.getFeedback(), dto.getStatus());

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        validationUtils.validateStatusTransition(assignmentSubmission.getStatus(), dto.getStatus());

        return updateFeedbackData(assignmentSubmission, dto);
    }

    //과제 피드백 삭제하기
    @Transactional
    public void deleteFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        assignmentSubmission.setFeedback(null);
        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Feedback deleted successfully for submission: {}", assignmentSubmission.getSubmissionId());
    }

    // 상태별 과제 제출 조회 메서드
    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsByStatus(Long groupId, Long planId, SubmissionStatus status, Long currentUserStudentNumber) {

        log.info("getSubmissionsByStatus called for status: {}", status);

        User user = validationUtils.validateBasicAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        validationUtils.validateSubmissionStatus(status.toString());

        // 역할 반한
        GroupMemberRole role = validationUtils.validateUserRole(user.getUserId(), groupId);

        List<AssignmentSubmission> submissions;

        if (role == GroupMemberRole.MENTOR) {
            // 멘토는 해당 과제의 모든 특정 상태 제출물 조회 가능
            submissions = submissionRepository.findByPlanPlanIdAndStatus(planId, status);
            log.info("Mentor {} retrieved {} submissions with status: {}", user.getUserId(), submissions.size(), status);
        } else {
            // 멘티는 자신의 특정 상태 제출물만 조회 가능
            submissions = submissionRepository.findByPlanPlanIdAndSubmitterUserIdAndStatus(planId, user.getUserId(), status);
            log.info("Mentee {} retrieved {} own submissions with status: {}", user.getUserId(), submissions.size(), status);
        }

        return submissions.stream()
                .map(this::createSubmissionResponse)
                .collect(Collectors.toList());
    }

    // 과제 제출 통계 조회 메서드 (멘토 전용)
    @Transactional(readOnly = true)
    public SubmissionStatisticsResponse getSubmissionStatistics(Long groupId, Long planId, Long currentUserStudentNumber) {

        log.info("getSubmissionStatistics called");

        // 멘토 권한 검증
        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validatePlanBelongsToGroup(planId, groupId);
        Plan plan = validationUtils.validatePlan(planId);
        validationUtils.validateAssignmentSubmissionAllowed(planId);

        // 해당 스터디 그룹의 전체 멘티 수 조회
        int totalMentees = groupMemberRepository.findByStudyGroup_StudyIdAndRole(groupId, GroupMemberRole.MENTEE).size();

        // 상태별 제출 수 조회
        int submittedCount = submissionRepository.countByPlanPlanIdAndStatus(planId, SubmissionStatus.SUBMITTED);
        int completedCount = submissionRepository.countByPlanPlanIdAndStatus(planId, SubmissionStatus.COMPLETED);
        int revisionRequiredCount = submissionRepository.countByPlanPlanIdAndStatus(planId, SubmissionStatus.REVISION_REQUIRED);

        // 미제출자 수 계산
        int submittedMentees = submissionRepository.countDistinctSubmittersByPlanId(planId);
        int notSubmittedCount = totalMentees - submittedMentees;

        return SubmissionStatisticsResponse.builder()
                .totalMentees(totalMentees)
                .submittedCount(submittedCount)
                .completedCount(completedCount)
                .revisionRequiredCount(revisionRequiredCount)
                .notSubmittedCount(notSubmittedCount)
                .build();
    }

    /**
    DTO 반환용 Method들
     */

    // 과제 피드백 수정해서 dto 반환하기
    private SubmissionFeedbackDto updateFeedbackData(AssignmentSubmission submission, SubmissionFeedbackDto dto) {

        if(dto.getFeedback() != null && !dto.getFeedback().trim().isEmpty()){
            submission.setFeedback(dto.getFeedback());
            submission.setUpdatedAt(LocalDateTime.now());
        }
        else {
            throw new RestApiException(ErrorCode.FEEDBACK_CONTENT_REQUIRED);
        }

        if (dto.getStatus() != null) {
            submission.setStatus(dto.getStatus());
        }

        submissionRepository.save(submission);

        log.info("Feedback updated successfully for submission: {}", submission.getSubmissionId());

        return SubmissionFeedbackDto.builder()
                .feedback(submission.getFeedback())
                .status(submission.getStatus())
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

        submission.setStatus(dto.getStatus());
        submission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        log.info("Feedback saved successfully for submission: {}", submission.getSubmissionId());
    }

    // 과제 제출 반환용 Dto로 변환하기
    private SubmissionResponse createSubmissionResponse(AssignmentSubmission assignmentSubmission) {

        return SubmissionResponse.builder()
                .submissionId(assignmentSubmission.getSubmissionId())
                .content(assignmentSubmission.getContent())
                .creatorName(assignmentSubmission.getSubmitter().getName())
                .status(assignmentSubmission.getStatus())
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
                .status(assignmentSubmission.getStatus())
                .build();
    }

    private DetailSubmissionResponse updateSubmitData(AssignmentSubmission assignmentSubmission, SubmissionDto dto) {

        // 수정할 데이터가 없으면 예외
        if((dto.getContent() == null || dto.getContent().trim().isEmpty()) &&
                (dto.getPassword() == null || dto.getPassword().trim().isEmpty())) {
            throw new RestApiException(ErrorCode.SUBMISSION_CONTENT_REQUIRED);
        }

        SubmissionStatus originalStatus = assignmentSubmission.getStatus();

        if(dto.getContent() != null && !dto.getContent().trim().isEmpty()){
            validationUtils.validateSubmissionContent(dto.getContent());
            assignmentSubmission.setContent(dto.getContent());

            if (originalStatus == SubmissionStatus.REVISION_REQUIRED) {
                assignmentSubmission.setStatus(SubmissionStatus.SUBMITTED);
                log.info("Status changed -> SUBMITTED for submission: {}",
                        assignmentSubmission.getSubmissionId());
            }
        }

        if(dto.getPassword() != null && !dto.getPassword().trim().isEmpty()){
            assignmentSubmission.setPassword(dto.getPassword());
        }

        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Assignment submission updated successfully: {}", assignmentSubmission.getSubmissionId());

        return createDetailSubmissionResponse(assignmentSubmission);
    }
}
