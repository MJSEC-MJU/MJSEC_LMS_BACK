package com.mjsec.lms.studygroup.service;

import com.mjsec.lms.studygroup.domain.GroupMember;
import com.mjsec.lms.studygroup.domain.StudyGroup;
import com.mjsec.lms.user.domain.User;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.user.repository.UserRepository;
import com.mjsec.lms.studygroup.repository.StudyGroupRepository;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.assignment.repository.PlanCommentRepository;
import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.studygroup.repository.StudyActivityRepository;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.user.domain.type.UserRole;
import com.mjsec.lms.common.util.ValidationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mjsec.lms.media.service.FileService;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorService - 어드민 bypass 테스트")
class MentorServiceAdminBypassTest {

    @Mock private UserRepository userRepository;
    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private PlanCommentRepository planCommentRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private StudyActivityRepository studyActivityRepository;
    @Mock private ValidationUtils validationUtils;
    @Mock private FileService fileService;
    @Mock private PlanRepository planRepository;

    private MentorService mentorService;

    @BeforeEach
    void setUp() {
        mentorService = new MentorService(
                userRepository,
                studyGroupRepository,
                groupMemberRepository,
                submissionRepository,
                planCommentRepository,
                attendanceRepository,
                studyActivityRepository,
                validationUtils,
                fileService
        );
    }

    // ========== checkMentor() ==========

    @Nested
    @DisplayName("checkMentor() - 멘토(creator) 확인")
    class CheckMentorTest {

        @Test
        @DisplayName("어드민은 그룹 creator가 아니어도 StudyGroup을 반환한다")
        void admin_bypasses_creator_check() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));

            // when
            StudyGroup result = mentorService.checkMentor(adminStudentNumber, groupId);

            // then
            assertThat(result).isEqualTo(studyGroup);

            // creator 비교가 발생하지 않아야 함
            verify(studyGroup, never()).getCreator();
        }

        @Test
        @DisplayName("일반 유저가 그룹 creator이면 StudyGroup을 반환한다")
        void regular_user_passes_if_creator() {
            // given
            Long studentNumber = 20210001L;
            Long groupId = 10L;
            Long sharedUserId = 1L;

            User creator = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User creatorInGroup = mock(User.class);

            when(creator.getRole()).thenReturn(UserRole.ROLE_USER);
            when(creator.getUserId()).thenReturn(sharedUserId);
            when(creatorInGroup.getUserId()).thenReturn(sharedUserId); // 동일한 userId → creator 맞음
            when(studyGroup.getCreator()).thenReturn(creatorInGroup);

            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(creator));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));

            // when
            StudyGroup result = mentorService.checkMentor(studentNumber, groupId);

            // then
            assertThat(result).isEqualTo(studyGroup);
        }

        @Test
        @DisplayName("일반 유저가 그룹 creator가 아니면 MENTOR_ONLY_CAN_DELETE_MEMBER 예외가 발생한다")
        void regular_user_fails_if_not_creator() {
            // given
            Long studentNumber = 20210002L;
            Long groupId = 10L;

            User regularUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User realCreator = mock(User.class);

            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(regularUser.getUserId()).thenReturn(2L);
            when(realCreator.getUserId()).thenReturn(1L); // 다른 userId → creator 아님
            when(studyGroup.getCreator()).thenReturn(realCreator);

            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(regularUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));

            // when & then
            assertThatThrownBy(() -> mentorService.checkMentor(studentNumber, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.MENTOR_ONLY_CAN_DELETE_MEMBER));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 USER_NOT_FOUND 예외가 발생한다")
        void user_not_found_throws_exception() {
            // given
            Long studentNumber = 99998L;
            Long groupId = 10L;
            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mentorService.checkMentor(studentNumber, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 그룹이면 STUDY_NOT_FOUND 예외가 발생한다")
        void group_not_found_throws_exception() {
            // given
            Long studentNumber = 20210001L;
            Long groupId = 999L;

            User user = mock(User.class);
            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(user));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> mentorService.checkMentor(studentNumber, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STUDY_NOT_FOUND));
        }
    }

    // ========== warnMember() ==========

    @Nested
    @DisplayName("warnMember() - 멘티 경고 부여")
    class WarnMemberTest {

        @Test
        @DisplayName("어드민은 creator가 아니어도 멘티에게 경고를 부여할 수 있다")
        void admin_can_warn_member() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;
            Long menteeStudentNumber = 20210001L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User mentee = mock(User.class);
            GroupMember groupMember = mock(GroupMember.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findByStudentNumber(menteeStudentNumber)).thenReturn(Optional.of(mentee));
            when(groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)).thenReturn(Optional.of(groupMember));
            when(groupMember.getWarn()).thenReturn(1);

            // when & then
            assertThatCode(() -> mentorService.warnMember(adminStudentNumber, groupId, menteeStudentNumber))
                    .doesNotThrowAnyException();

            verify(groupMember).setWarn(2);
            verify(groupMemberRepository).save(groupMember);
        }

        @Test
        @DisplayName("경고 3회 누적 시 멤버가 자동 퇴출된다")
        void admin_warn_triggers_expulsion_at_3_warnings() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;
            Long menteeStudentNumber = 20210001L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User mentee = mock(User.class);
            GroupMember groupMember = mock(GroupMember.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findByStudentNumber(menteeStudentNumber)).thenReturn(Optional.of(mentee));
            when(groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)).thenReturn(Optional.of(groupMember));
            when(groupMember.getWarn()).thenReturn(3); // 경고 3회 누적

            // when
            mentorService.warnMember(adminStudentNumber, groupId, menteeStudentNumber);

            // then - 3회 누적이므로 자동 퇴출
            verify(groupMemberRepository).delete(groupMember);
        }
    }

    // ========== addMember() ==========

    @Nested
    @DisplayName("addMember() - 멤버 추가")
    class AddMemberTest {

        @Test
        @DisplayName("어드민은 creator가 아니어도 멤버를 추가할 수 있다")
        void admin_can_add_member() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;
            Long newMenteeStudentNumber = 20210002L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User newMentee = mock(User.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findByStudentNumber(newMenteeStudentNumber)).thenReturn(Optional.of(newMentee));
            when(groupMemberRepository.existsByUserAndStudyGroup(newMentee, studyGroup)).thenReturn(false);

            // when & then
            assertThatCode(() -> mentorService.addMember(adminStudentNumber, groupId, newMenteeStudentNumber))
                    .doesNotThrowAnyException();

            verify(groupMemberRepository).save(any(GroupMember.class));
        }

        @Test
        @DisplayName("이미 그룹에 있는 멤버를 추가하면 ALREADY_JOINED_GROUP 예외가 발생한다")
        void add_already_joined_member_throws_exception() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;
            Long existingMenteeStudentNumber = 20210003L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User existingMentee = mock(User.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findByStudentNumber(existingMenteeStudentNumber)).thenReturn(Optional.of(existingMentee));
            when(groupMemberRepository.existsByUserAndStudyGroup(existingMentee, studyGroup)).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> mentorService.addMember(adminStudentNumber, groupId, existingMenteeStudentNumber))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_JOINED_GROUP));
        }
    }

    // ========== deleteMember() ==========

    @Nested
    @DisplayName("deleteMember() - 멤버 삭제")
    class DeleteMemberTest {

        @Test
        @DisplayName("어드민은 creator가 아니어도 멤버를 삭제할 수 있다")
        void admin_can_delete_member() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;
            Long menteeStudentNumber = 20210004L;
            Long menteeUserId = 5L;
            Long studyGroupId = 10L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            User mentee = mock(User.class);
            GroupMember groupMember = mock(GroupMember.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(mentee.getUserId()).thenReturn(menteeUserId);
            when(studyGroup.getStudyId()).thenReturn(studyGroupId);

            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findByStudyId(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findByStudentNumber(menteeStudentNumber)).thenReturn(Optional.of(mentee));
            when(groupMemberRepository.findByUserAndStudyGroup(mentee, studyGroup)).thenReturn(Optional.of(groupMember));

            // 연관 데이터 없음 (cascade 삭제 대상 없음)
            when(submissionRepository.findIdsByUserIdAndStudyGroupId(menteeUserId, studyGroupId)).thenReturn(List.of());
            when(planCommentRepository.findIdsByUserIdAndStudyGroupId(menteeUserId, studyGroupId)).thenReturn(List.of());
            when(attendanceRepository.findIdsByUserIdAndStudyGroupId(menteeUserId, studyGroupId)).thenReturn(List.of());
            when(studyActivityRepository.findIdsByCreatorIdAndStudyGroupId(menteeUserId, studyGroupId)).thenReturn(List.of());

            // when & then
            assertThatCode(() -> mentorService.deleteMember(adminStudentNumber, groupId, menteeStudentNumber))
                    .doesNotThrowAnyException();

            verify(groupMemberRepository).delete(groupMember);
        }
    }
}
