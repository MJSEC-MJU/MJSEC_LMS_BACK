package com.mjsec.lms.service;

import com.mjsec.lms.domain.PendingUser;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.PendingUserDto;
import com.mjsec.lms.dto.StudyGroupDto.StudyGroupRequestDto;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.PendingUserRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.UserRole;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminService {

    private final PendingUserRepository pendingUserRepository;
    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;


    public AdminService(PendingUserRepository pendingUserRepository, UserRepository userRepository,
                        StudyGroupRepository studyGroupRepository) {

        this.pendingUserRepository = pendingUserRepository;
        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
    }

    /**
     * 회원가입 승인 대기자 목록을 반환하는 메소드
     * @return List<PendingUserDto> (각 PendingUserDto는 학번, 이름, 이메일, 전화번호 정보를 담음)
     */
    public List<PendingUserDto> getAllPendingUser() {

        log.info("Getting all pending users");

        List<PendingUser> allPendingUsers = pendingUserRepository.findAll();

        return allPendingUsers.stream()
                .map(user -> PendingUserDto.builder()
                        .studentNumber(user.getStudentNumber())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .toList();
    }

    /**
     * 회원가입 승인을 위한 메소드
     * @param studentNumber 학번
     */
    @Transactional
    public void approveRegister(Long studentNumber){

        log.info("Approve registration for {}", studentNumber);

        PendingUser pendingUser = pendingUserRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        if (userRepository.existsByStudentNumber(pendingUser.getStudentNumber()) ||
                userRepository.existsByEmail(pendingUser.getEmail())) {
            log.warn("Already existing user: {}", studentNumber);
            throw new RestApiException(ErrorCode.ALREADY_REGISTERED_USER);
        }

        User user = User.builder()
                .studentNumber(pendingUser.getStudentNumber())
                .password(pendingUser.getPassword())
                .name(pendingUser.getName())
                .email(pendingUser.getEmail())
                .phoneNumber(pendingUser.getPhoneNumber())
                .role(UserRole.ROLE_USER)
                .build();

        userRepository.save(user);
        pendingUserRepository.delete(pendingUser);
        log.info("Moved pending user to approved user: {}", user.getStudentNumber());
    }

    /**
     * 스터디 그룹을 생성하는 메소드
     * @param requestDto 스터디 그룹명, 스터디 소개, 스터디 타입, 멘토 학번
     */
    public void createGroup(StudyGroupRequestDto requestDto) {

        User mentor = userRepository.findByStudentNumber(requestDto.getMentorStudentNumber())
                .orElseThrow(() -> new RestApiException(ErrorCode.INVALID_MENTOR_STUDENT_NUMBER));

        if(studyGroupRepository.existsByName(requestDto.getName())){
            throw new RestApiException(ErrorCode.STUDY_GROUP_ALREADY_EXIST);
        }

        StudyGroup studyGroup = StudyGroup.builder()
                .name(requestDto.getName())
                .category(requestDto.getCategory().name())
                .content(requestDto.getContent())
                .creator(mentor)
                .build();

        studyGroupRepository.save(studyGroup);
    }
}
