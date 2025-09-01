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

    /**
     * 스터디 그룹에 멘티를 추가하는 메소드
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @param studentNumber 스터디 그룹 멘티의 학번
     */
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

    /**
     * 스터디 그룹에서 특정 멘티를 삭제시키는 메소드
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @param studentNumber 스터디 그룹 멘티의 학번
     */
    public void deleteMember(Long currentStudentNumber, Long groupId, Long studentNumber) {

        User user = userRepository.findByStudentNumber(currentStudentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        StudyGroup studyGroup = studyGroupRepository.findByStudyId(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        if(user != studyGroup.getCreator()) {
            throw new RestApiException(ErrorCode.MENTOR_ONLY_CAN_DELETE_MEMBER);
        }

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        GroupMember groupMember = groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        groupMemberRepository.delete(groupMember);
    }
}
