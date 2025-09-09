package com.mjsec.lms.service.user;

import com.mjsec.lms.domain.study.GroupMember;
import com.mjsec.lms.domain.user.User;
import com.mjsec.lms.dto.study.StudyGroupSummaryDto;
import com.mjsec.lms.dto.user.UserResponse;
import com.mjsec.lms.repository.study.GroupMemberRepository;
import com.mjsec.lms.repository.user.UserRepository;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ValidationUtils validationUtils;

    public UserService(UserRepository userRepository,
                       GroupMemberRepository groupMemberRepository,
                       ValidationUtils validationUtils) {

        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.validationUtils = validationUtils;
    }

    // 유저 페이지 조회
    public UserResponse getUserPage(Long studentNumber) {

        log.info("get user page called with studentNumber: {}", studentNumber);

        User user = validationUtils.validateUser(studentNumber);

        // 사용자가 속한 스터디 그룹들 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByUserIdWithStudyGroup(user.getUserId());

        // StudyGroupSummaryDto 리스트로 변환
        List<StudyGroupSummaryDto> studyGroups = groupMembers.stream()
                .map(this::createStudyGroupSummary)
                .collect(Collectors.toList());

        return createUserResponse(user, studyGroups);
    }

    // UserResponse 반환하기 (스터디 그룹 정보 포함)
    private UserResponse createUserResponse(User user, List<StudyGroupSummaryDto> studyGroups) {

        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .studentNumber(user.getStudentNumber())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImage(user.getProfileImage())
                .createdAt(user.getCreatedAt())
                .studyGroups(studyGroups)
                .build();
    }

    // StudyGroupSummary 생성 메소드
    private StudyGroupSummaryDto createStudyGroupSummary(GroupMember groupMember) {

        return StudyGroupSummaryDto.builder()
                .studyGroupId(groupMember.getStudyGroup().getStudyId())
                .name(groupMember.getStudyGroup().getName())
                .category(groupMember.getStudyGroup().getCategory())
                .studyImage(groupMember.getStudyGroup().getStudyImage())
                .build();
    }
}