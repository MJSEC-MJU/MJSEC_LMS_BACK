package com.mjsec.lms.service;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MentorService {

    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public MentorService(UserRepository userRepository, StudyGroupRepository studyGroupRepository,
                         GroupMemberRepository groupMemberRepository) {

        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public void addMember(Long currentStudentNumber, Long groupId, Long studentNumber) {

        User user = userRepository.findByStudentNumber(currentStudentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        StudyGroup studyGroup = studyGroupRepository.findByStudyId(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        if(user != studyGroup.getCreator()) {
            throw new RestApiException(ErrorCode.MENTOR_ONLY_CAN_ADD_MEMBER);
        }

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        GroupMember groupMember = GroupMember.builder()
                .user(mentee)
                .studyGroup(studyGroup)
                .build();

        groupMemberRepository.save(groupMember);
    }
}
