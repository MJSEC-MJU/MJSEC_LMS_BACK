package com.mjsec.lms.service;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.AttendanceRepository;
import com.mjsec.lms.repository.GroupMemberRepository;
import com.mjsec.lms.repository.PlanCommentRepository;
import com.mjsec.lms.repository.PlanRepository;
import com.mjsec.lms.repository.StudyActivityRepository;
import com.mjsec.lms.repository.StudyGroupRepository;
import com.mjsec.lms.repository.SubmissionRepository;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.type.ErrorCode;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MentorService {

    private final UserRepository userRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final PlanCommentRepository planCommentRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudyActivityRepository studyActivityRepository;
    private final PlanRepository planRepository;

    public MentorService(UserRepository userRepository, StudyGroupRepository studyGroupRepository,
                         GroupMemberRepository groupMemberRepository, SubmissionRepository submissionRepository,
                         PlanCommentRepository planCommentRepository, AttendanceRepository attendanceRepository,
                         StudyActivityRepository studyActivityRepository, PlanRepository planRepository) {

        this.userRepository = userRepository;
        this.studyGroupRepository = studyGroupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
        this.planCommentRepository = planCommentRepository;
        this.attendanceRepository = attendanceRepository;
        this.studyActivityRepository = studyActivityRepository;
        this.planRepository = planRepository;
    }

    /**
     * 요청을 보낸 사용자가 해당 스터디그룹의 멘토라면 StudyGroup 엔티티를 반환하는 메소드
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @return StudyGroup 스터디 그룹 엔티티
     */
    public StudyGroup checkMentor(Long currentStudentNumber, Long groupId) {

        User user = userRepository.findByStudentNumber(currentStudentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        StudyGroup studyGroup = studyGroupRepository.findByStudyId(groupId)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_NOT_FOUND));

        if(!Objects.equals(user.getUserId(), studyGroup.getCreator().getUserId())) {
            throw new RestApiException(ErrorCode.MENTOR_ONLY_CAN_DELETE_MEMBER);
        }

        return studyGroup;
    }

    /**
     * 스터디 그룹에 멘티를 추가하는 메소드
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @param studentNumber 스터디 그룹 멘티의 학번
     */
    public void addMember(Long currentStudentNumber, Long groupId, Long studentNumber) {

        StudyGroup studyGroup = checkMentor(currentStudentNumber, groupId);

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        if(groupMemberRepository.existsByUserAndStudyGroup(mentee, studyGroup)){
            throw new RestApiException(ErrorCode.ALREADY_JOINED_GROUP);
        }

        GroupMember groupMember = GroupMember.builder()
                .user(mentee)
                .studyGroup(studyGroup)
                .build();

        groupMemberRepository.save(groupMember);
    }

    /**
     * 스터디 그룹에서 특정 멘티를 삭제시키는 메소드 (그룹 멤버 엔티티와 연관이 있는 모든 엔티티 Soft Delete 처리)
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @param studentNumber 스터디 그룹 멘티의 학번
     */
    public void deleteMember(Long currentStudentNumber, Long groupId, Long studentNumber) {

        StudyGroup studyGroup = checkMentor(currentStudentNumber, groupId);

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        GroupMember groupMember = groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        Long userId = mentee.getUserId();
        Long studyId = studyGroup.getStudyId();

        List<Long> submissionIds = submissionRepository.findIdsByUserIdAndStudyGroupId(userId, studyId);
        if (!submissionIds.isEmpty()) {
            submissionRepository.deleteAllById(submissionIds);
        }

        List<Long> commentIds = planCommentRepository.findIdsByUserIdAndStudyGroupId(userId, studyId);
        if (!commentIds.isEmpty()) {
            planCommentRepository.deleteAllById(commentIds);
        }

        List<Long> attendanceIds = attendanceRepository.findIdsByUserIdAndStudyGroupId(userId, studyId);
        if (!attendanceIds.isEmpty()) {
            attendanceRepository.deleteAllById(attendanceIds);
        }

        List<Long> activityIds = studyActivityRepository.findIdsByCreatorIdAndStudyGroupId(userId, studyId);
        if (!activityIds.isEmpty()) {
            studyActivityRepository.deleteAllById(activityIds);
        }

        List<Long> planIds = planRepository.findIdsByCreatorIdAndStudyGroupId(userId, studyId);
        if (!planIds.isEmpty()) {
            planRepository.deleteAllById(planIds);
        }

        groupMemberRepository.delete(groupMember);
    }

    /**
     * 멘티에게 수동으로 경고를 부여하는 메소드 (경고 3번 누적 시 스터디 자동 퇴출)
     * @param currentStudentNumber 요청을 보낸 사용자의 학번
     * @param groupId 스터디 그룹의 Id
     * @param studentNumber 스터디 그룹 멘티의 학번
     */
    public void warnMember(Long currentStudentNumber, Long groupId, Long studentNumber) {

        StudyGroup studyGroup = checkMentor(currentStudentNumber, groupId);

        User mentee = userRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new RestApiException(ErrorCode.USER_NOT_FOUND));

        GroupMember groupMember = groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)
                .orElseThrow(() -> new RestApiException(ErrorCode.STUDY_USER_NOT_FOUND));

        groupMember.setWarn(groupMember.getWarn() + 1);
        groupMemberRepository.save(groupMember);

        if(groupMember.getWarn() == 3) {
            groupMemberRepository.delete(groupMember);
        }
    }
}
