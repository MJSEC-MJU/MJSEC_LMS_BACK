package com.mjsec.lms.service;

import com.mjsec.lms.domain.Assignment;
import com.mjsec.lms.domain.AssignmentSubmission;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.*;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;

    public AssignmentService(AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             StudyGroupRepository studyGroupRepository,
                             GroupMemberRepository groupMemberRepository,
                             SubmissionRepository submissionRepository) {

        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
    }

    // 과제 등록하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse createAssignment(Long groupId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);

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

        log.info("Assignment created successfully: {}", assignment);
        assignmentRepository.save(assignment);

        return createDetailAssignmentResponse(assignment);
    }

    // 전체 과제 조회하기
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignment(Long groupId, Long currentUserStudentNumber) {

        log.info("getAssignment called");

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);

        List<Assignment> assignmentList = assignmentRepository.findAllByStudyGroup_StudyId(groupId);
        log.info("Found {} assignments in StudyGroup: {}", assignmentList.size(), studyGroup.getName());

        List<AssignmentResponse> assignmentResponses = new ArrayList<>();
        for (Assignment assignment : assignmentList) {
            assignmentResponses.add(createResponse(assignment));
        }

        log.info("get Assignment Successfully!");
        return assignmentResponses;
    }

    // 과제 상세 조회하기
    @Transactional(readOnly = true)
    public DetailAssignmentResponse getDetailAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("getDetailAssignment called");

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);
        return createDetailAssignmentResponse(assignment);
    }

    //과제 수정하기 (멘토만 가능함.)
    @Transactional
    public DetailAssignmentResponse updateAssignment(Long groupId, Long assignmentId, AssignmentDto dto, Long currentUserStudentNumber) {

        log.info("updateAssignment called");

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        updateAssignmentData(assignment, dto);
        return createDetailAssignmentResponse(assignment);
    }

    // 과제 삭제하기 (멘토만 가능함.)
    @Transactional
    public void deleteAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {

        log.info("deleteAssignment called");

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        assignmentRepository.delete(assignment);
        log.info("Assignment deleted successfully: {}", assignment);
    }

    //과제 제출하기
    @Transactional
    public SubmissionResponse submitAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber, SubmissionDto dto) {

        log.info("submitAssignment called");

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        validateMenteeRole(user.getUserId(), groupId);
        Assignment assignment = validateAssignment(assignmentId);

        AssignmentSubmission assignmentSubmission = AssignmentSubmission.builder()
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .submitter(user)
                .assignment(assignment)
                .build();

        submissionRepository.save(assignmentSubmission);
        log.info("Assignment submission saved successfully: {}", assignmentSubmission);

        return createSubmissionResponse(assignmentSubmission);
    }

    /**
     * === 검증 메서드들 ===
     */

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

    private void validateMenteeRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if(userRole != GroupMemberRole.MENTEE) {
            log.warn("User {} does not have MENTEE role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTEE_ROLE);
        }
    }

    //사용자가 해당 스터디 그룹의 멘토인지 확인
    private void validateMentorRole(Long userId, Long groupId) {

        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if (userRole != GroupMemberRole.MENTO) {
            log.warn("User {} does not have MENTO role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_MENTO_ROLE);
        }
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

    /**
     * === 데이터 처리 메서드들 ===
     */

    //Assignment 데이터를 업데이트
    private void updateAssignmentData(Assignment assignment, AssignmentDto dto) {

        /*
        null이 아닌 경우만 update 하도록 변경.
        title과 content는 빈 문자열인지 아닌지까지 검사하기
         */
        
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

    /**
     * === DTO 변환 메서드들 ===
     */

    //Assignment를 DetailAssignmentResponse로 변환
    private DetailAssignmentResponse createDetailAssignmentResponse(Assignment assignment) {

        return DetailAssignmentResponse.builder()
                .assignmentId(assignment.getAssignId())
                .title(assignment.getTitle())
                .content(assignment.getContent())
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .creatorName(assignment.getCreator().getName())
                .createdAt(assignment.getCreatedAt())
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

    private SubmissionResponse createSubmissionResponse(AssignmentSubmission assignmentSubmission) {

        return SubmissionResponse.builder()
                .submissionId(assignmentSubmission.getSubmissionId())
                .content(assignmentSubmission.getContent())
                .creatorName(assignmentSubmission.getSubmitter().getName())
                .createdAt(assignmentSubmission.getCreatedAt())
                .build();
    }
}