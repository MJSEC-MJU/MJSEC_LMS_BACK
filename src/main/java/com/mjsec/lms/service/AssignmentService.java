package com.mjsec.lms.service;

import com.mjsec.lms.domain.Assignment;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AssignmentDTO;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.AssignmentRepository;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
                             GroupMemberRepository groupMemberRepository
    ) {

        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    //과제 등록하기 (멘토만 가능함.)
    public void createAssignment(Long groupId, AssignmentDTO dto , Long currentUserStudentNumber) {

        log.info("createAssignment called with groupId: {}, dto: {}, currentUserStudentNumber: {}", groupId, dto, currentUserStudentNumber);

        //현재 과제를 생성하려는 유저 확인하기
        User user = userRepository.findByStudentNumber(currentUserStudentNumber).orElseThrow(()-> new RestApiException(ErrorCode.USER_NOT_FOUND));

        //스터디가 존재하는 지 확인하기
        StudyGroup studyGroup = studyGroupRepository.findById(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        //스터디에 등록되어 있는지 (멘토, 멘티 역할이 있는가?)
        GroupMemberRole userRole = groupMemberRepository.findRoleByUserIdAndStudyId(user.getUserId(), groupId).orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        // 멘토 권한 확인
        if (userRole != GroupMemberRole.MENTO) {
            log.warn("User {} does not have MENTOR role in StudyGroup {}. Current role: {}",
                    user.getUserId(), groupId, userRole);
            throw new RestApiException(ErrorCode.UNAUTHORIZED_ROLE);
        }

        log.info("User {} is confirmed as MENTOR of StudyGroup: {}", user.getUserId(), studyGroup.getName());

        //데이터 넣기
        Assignment assignment = Assignment.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .creator(user)
                .studyGroup(studyGroup)
                .build();

        log.info("Assignment created successfully: {}", assignment);

        assignmentRepository.save(assignment);
    }
}
