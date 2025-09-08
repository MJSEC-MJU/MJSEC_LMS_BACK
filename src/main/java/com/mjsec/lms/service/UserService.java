package com.mjsec.lms.service;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.StudyGroupSummaryDto;
import com.mjsec.lms.dto.UserResponse;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ValidationUtils validationUtils;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       GroupMemberRepository groupMemberRepository,
                       ValidationUtils validationUtils, PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.validationUtils = validationUtils;
        this.passwordEncoder = passwordEncoder;
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

    /**
     * 이메일 형식에 맞는 문자열인지 검사하는 메소드
     * @param email 이메일
     * @return true / false
     */
    public boolean isValidEmail(String email) {

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    /**
     * 존재하는 사용자의 이메일인지 검사하는 메소드
     * @param email 이메일
     * @return true / false
     */
    public boolean isEmailExists(String email) {

        return userRepository.existsByEmail(email);
    }

    /**
     * PasswordEncoder 를 사용하여 유저의 비밀번호를 업데이트하는 메소드
     * @param email 유저의 이메일
     * @param password 유저의 비밀번호
     */
    public void updatePassword(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RestApiException(ErrorCode.NOT_REGISTERED_EMAIL));

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}