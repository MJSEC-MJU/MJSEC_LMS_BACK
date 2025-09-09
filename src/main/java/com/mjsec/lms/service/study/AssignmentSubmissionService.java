package com.mjsec.lms.service.study;

import com.mjsec.lms.domain.study.AssignmentSubmission;
import com.mjsec.lms.domain.mentor.Plan;
import com.mjsec.lms.domain.user.User;
import com.mjsec.lms.dto.study.DetailSubmissionResponse;
import com.mjsec.lms.dto.study.SubmissionDto;
import com.mjsec.lms.dto.mentor.SubmissionFeedbackDto;
import com.mjsec.lms.dto.study.SubmissionResponse;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.study.SubmissionRepository;
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
public class AssignmentSubmissionService {

    private final ValidationUtils validationUtils;
    private final SubmissionRepository submissionRepository;

    AssignmentSubmissionService(ValidationUtils validationUtils, SubmissionRepository submissionRepository){

        this.validationUtils = validationUtils;
        this.submissionRepository = submissionRepository;
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

        // 과제 제출 중복 여부 체크
        validationUtils.validateDuplicateSubmission(user.getUserId(), planId);

        AssignmentSubmission assignmentSubmission = AssignmentSubmission.builder()
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .password(dto.getPassword())
                .submitter(user)
                .plan(plan)
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
    public DetailSubmissionResponse updateAssignmentSubmission(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionDto dto) {

        log.info("updateAssignmentSubmission called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        return updateSubmitData(assignmentSubmission, dto);
    }

    // 과제 제출 삭제하기
    @Transactional
    public void deleteAssignmentSubmission(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteAssignmentSubmission called");

        User user = validationUtils.validateMenteeAccess(groupId, currentUserStudentNumber);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateSubmissionOwnership(assignmentSubmission, user.getUserId());

        submissionRepository.delete(assignmentSubmission);
        log.info("Assignment submission deleted successfully: {}", assignmentSubmission);
    }

    // 멘토가 과제 피드백 남기기
    @Transactional
    public void leaveFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("leaveFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackNotExists(assignmentSubmission);

        leaveFeedbackData(assignmentSubmission, dto);
    }

    //과제 피드백 수정하기
    @Transactional
    public SubmissionFeedbackDto updateFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber, SubmissionFeedbackDto dto) {

        log.info("updateFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        return updateFeedbackData(assignmentSubmission, dto);
    }

    //과제 피드백 삭제하기
    @Transactional
    public void deleteFeedback(Long groupId, Long planId, Long submitId, Long currentUserStudentNumber) {

        log.info("deleteFeedback called");

        validationUtils.validateMentorAccess(groupId, currentUserStudentNumber);
        validationUtils.validateAssignmentSubmissionAllowed(planId);
        AssignmentSubmission assignmentSubmission = validationUtils.validateSubmissionAccess(planId, submitId);
        validationUtils.validateFeedbackExists(assignmentSubmission);

        assignmentSubmission.setFeedback(null);
        assignmentSubmission.setUpdatedAt(LocalDateTime.now());
        submissionRepository.save(assignmentSubmission);

        log.info("Feedback deleted successfully for submission: {}", assignmentSubmission.getSubmissionId());
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

        submissionRepository.save(submission);

        log.info("Feedback updated successfully for submission: {}", submission.getSubmissionId());

        return SubmissionFeedbackDto.builder()
                .feedback(submission.getFeedback())
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
}
