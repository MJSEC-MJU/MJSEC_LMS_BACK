package com.mjsec.lms.service;

import com.mjsec.lms.domain.Assignment;
import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AssignmentDTO;
import com.mjsec.lms.dto.AssignmentResponse;
import com.mjsec.lms.dto.DetailAssignmentResponse;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.AssignmentRepository;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
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

    public AssignmentService(AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             StudyGroupRepository studyGroupRepository,
                             GroupMemberRepository groupMemberRepository) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    // 과제 등록하기 (멘토만 가능함.)
    public DetailAssignmentResponse createAssignment(Long groupId, AssignmentDTO dto, Long currentUserStudentNumber) {
        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);

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

        log.info("Assignment created successfully: {}", assignment);
        assignmentRepository.save(assignment);

        return createDetailAssignmentResponse(assignment);
    }

    @Transactional(readOnly = true)
    // 전체 과제 조회하기
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

    @Transactional(readOnly = true)
    // 과제 상세 조회하기
    public DetailAssignmentResponse getDetailAssignment(Long groupId, Long assignmentId, Long currentUserStudentNumber) {
        log.info("getDetailAssignment called");

        User user = validateUser(currentUserStudentNumber);
        StudyGroup studyGroup = validateStudyGroup(groupId);
        validateGroupMembership(user, studyGroup);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);
        return createDetailAssignmentResponse(assignment);
    }

    @Transactional
    //과제 수정하기 (멘토만 가능함.)
    public DetailAssignmentResponse updateAssignment(Long groupId, Long assignmentId, AssignmentDTO dto, Long currentUserStudentNumber) {
        log.info("updateAssignment called");

        User user = validateUser(currentUserStudentNumber);
        validateStudyGroup(groupId);
        validateMentorRole(user.getUserId(), groupId);
        Assignment assignment = validateAssignment(assignmentId);

        log.info("Found assignment: {}", assignment);

        updateAssignmentData(assignment, dto);
        return createDetailAssignmentResponse(assignment);
    }

    @Transactional
    // 과제 삭제하기 (멘토만 가능함.)
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

    //사용자가 해당 스터디 그룹의 멘토인지 확인
    private void validateMentorRole(Long userId, Long groupId) {
        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        if (userRole != GroupMemberRole.MENTO) {
            log.warn("User {} does not have MENTOR role in StudyGroup {}. Current role: {}", userId, groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_ROLE);
        }
    }

    //사용자가 해당 스터디 그룹의 멤버인지 확인
    private void validateGroupMembership(User user, StudyGroup studyGroup) {
        groupMemberRepository.findByUserAndStudyGroup(user, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));
    }

    //Assignment 존재 여부를 확인하고 Assignment 객체를 반환
    private Assignment validateAssignment(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RestApiException(ErrorCode.ASSIGNMENT_NOT_FOUND));
    }

    /**
     * === 데이터 처리 메서드들 ===
     */

    //Assignment 데이터를 업데이트
    private void updateAssignmentData(Assignment assignment, AssignmentDTO dto) {
        assignment.setTitle(dto.getTitle());
        assignment.setContent(dto.getContent());
        assignment.setStartDate(dto.getStartDate());
        assignment.setEndDate(dto.getEndDate());
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
}