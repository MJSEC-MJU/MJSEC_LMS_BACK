package com.mjsec.lms.util;

import com.mjsec.lms.domain.GroupMember;
import com.mjsec.lms.domain.StudyGroup;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.repository.*;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.GroupMemberRole;
import com.mjsec.lms.type.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationUtils - 어드민 권한 bypass 테스트")
class ValidationUtilsAdminBypassTest {

    @Mock private UserRepository userRepository;
    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private PlanRepository planRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private PlanCommentRepository planCommentRepository;
    @Mock private StudyActivityRepository studyActivityRepository;

    private ValidationUtils validationUtils;

    @BeforeEach
    void setUp() {
        validationUtils = new ValidationUtils(
                userRepository,
                studyGroupRepository,
                groupMemberRepository,
                attendanceRepository,
                planRepository,
                submissionRepository,
                planCommentRepository,
                studyActivityRepository
        );
    }

    // ========== validateGroupMembership() ==========

    @Nested
    @DisplayName("validateGroupMembership() - 그룹 멤버십 검증")
    class ValidateGroupMembershipTest {

        @Test
        @DisplayName("어드민은 그룹 멤버가 아니어도 통과한다")
        void admin_bypasses_membership_check() {
            // given
            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);

            // when & then
            assertThatCode(() -> validationUtils.validateGroupMembership(adminUser, studyGroup))
                    .doesNotThrowAnyException();

            // DB 조회 자체가 발생하지 않아야 함
            verifyNoInteractions(groupMemberRepository);
        }

        @Test
        @DisplayName("일반 유저는 그룹 멤버이면 통과한다")
        void regular_user_passes_if_member() {
            // given
            User regularUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(groupMemberRepository.findByUserAndStudyGroup(regularUser, studyGroup))
                    .thenReturn(Optional.of(mock(GroupMember.class)));

            // when & then
            assertThatCode(() -> validationUtils.validateGroupMembership(regularUser, studyGroup))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 유저는 그룹 멤버가 아니면 STUDY_USER_NOT_FOUND 예외가 발생한다")
        void regular_user_fails_if_not_member() {
            // given
            User regularUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);
            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(groupMemberRepository.findByUserAndStudyGroup(regularUser, studyGroup))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validationUtils.validateGroupMembership(regularUser, studyGroup))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STUDY_USER_NOT_FOUND));
        }
    }

    // ========== validateBasicAccess() ==========

    @Nested
    @DisplayName("validateBasicAccess() - 기본 접근 검증 (복합 흐름)")
    class ValidateBasicAccessTest {

        @Test
        @DisplayName("어드민은 그룹 멤버가 아니어도 validateBasicAccess를 통과한다")
        void admin_bypasses_basic_access() {
            // given
            Long adminStudentNumber = 99999L;
            Long groupId = 10L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(studyGroup));

            // when & then
            assertThatCode(() -> validationUtils.validateBasicAccess(groupId, adminStudentNumber))
                    .doesNotThrowAnyException();

            // 멤버십 DB 조회가 발생하지 않아야 함
            verifyNoInteractions(groupMemberRepository);
        }

        @Test
        @DisplayName("일반 유저는 그룹 멤버이면 validateBasicAccess를 통과한다")
        void regular_user_passes_basic_access_if_member() {
            // given
            Long studentNumber = 20210001L;
            Long groupId = 10L;

            User regularUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(regularUser));
            when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(studyGroup));
            when(groupMemberRepository.findByUserAndStudyGroup(regularUser, studyGroup))
                    .thenReturn(Optional.of(mock(GroupMember.class)));

            // when & then
            assertThatCode(() -> validationUtils.validateBasicAccess(groupId, studentNumber))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 유저는 그룹 멤버가 아니면 validateBasicAccess에서 예외가 발생한다")
        void regular_user_fails_basic_access_if_not_member() {
            // given
            Long studentNumber = 20210002L;
            Long groupId = 10L;

            User regularUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(regularUser));
            when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(studyGroup));
            when(groupMemberRepository.findByUserAndStudyGroup(regularUser, studyGroup))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validationUtils.validateBasicAccess(groupId, studentNumber))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STUDY_USER_NOT_FOUND));
        }
    }

    // ========== validateMentorAccess() ==========

    @Nested
    @DisplayName("validateMentorAccess() - 멘토 접근 검증 (복합 흐름)")
    class ValidateMentorAccessTest {

        @Test
        @DisplayName("어드민은 그룹 멤버가 아니어도 validateMentorAccess를 통과한다")
        void admin_bypasses_mentor_access() {
            // given
            Long adminStudentNumber = 99999L;
            Long adminUserId = 1L;
            Long groupId = 10L;

            User adminUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(adminUser.getUserId()).thenReturn(adminUserId);
            when(userRepository.findByStudentNumber(adminStudentNumber)).thenReturn(Optional.of(adminUser));
            when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(studyGroup));
            // validateMentorRole 내부에서 findById 한 번 더 호출됨
            when(userRepository.findById(adminUserId)).thenReturn(Optional.of(adminUser));

            // when & then
            assertThatCode(() -> validationUtils.validateMentorAccess(groupId, adminStudentNumber))
                    .doesNotThrowAnyException();

            // 멤버 역할 조회가 발생하지 않아야 함
            verify(groupMemberRepository, never()).findRoleByUserIdAndStudyId(any(), any());
        }

        @Test
        @DisplayName("일반 유저가 MENTOR이면 validateMentorAccess를 통과한다")
        void regular_mentor_passes_mentor_access() {
            // given
            Long studentNumber = 20210001L;
            Long userId = 2L;
            Long groupId = 10L;

            User mentorUser = mock(User.class);
            StudyGroup studyGroup = mock(StudyGroup.class);

            when(mentorUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(mentorUser.getUserId()).thenReturn(userId);
            when(userRepository.findByStudentNumber(studentNumber)).thenReturn(Optional.of(mentorUser));
            when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(studyGroup));
            when(userRepository.findById(userId)).thenReturn(Optional.of(mentorUser));
            when(groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId))
                    .thenReturn(Optional.of(GroupMemberRole.MENTOR));

            // when & then
            assertThatCode(() -> validationUtils.validateMentorAccess(groupId, studentNumber))
                    .doesNotThrowAnyException();
        }
    }

    // ========== validateMenteeRole() 리그레션 ==========

    @Nested
    @DisplayName("validateMenteeRole() - 멘티 역할 검증 (리그레션: 어드민도 막혀야 함)")
    class ValidateMenteeRoleRegressionTest {

        @Test
        @DisplayName("어드민도 그룹 멤버가 아니면 멘티 역할 체크에서 STUDY_USER_NOT_FOUND 예외가 발생한다")
        void admin_is_blocked_by_mentee_role_check() {
            // given - 어드민은 group_member 테이블에 없으므로 empty 반환
            Long adminUserId = 1L;
            Long groupId = 10L;
            when(groupMemberRepository.findRoleByUserIdAndStudyId(adminUserId, groupId))
                    .thenReturn(Optional.empty());

            // when & then - 과제 제출/수정/삭제 같은 멘티 전용 작업은 어드민도 불가
            assertThatThrownBy(() -> validationUtils.validateMenteeRole(adminUserId, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STUDY_USER_NOT_FOUND));
        }
    }

    // ========== validateMentorRole() ==========

    @Nested
    @DisplayName("validateMentorRole() - 멘토 역할 검증")
    class ValidateMentorRoleTest {

        @Test
        @DisplayName("어드민은 그룹 멤버가 아니어도 멘토 검증을 통과한다")
        void admin_bypasses_mentor_role_check() {
            // given
            Long userId = 1L;
            Long groupId = 10L;
            User adminUser = mock(User.class);
            when(adminUser.getRole()).thenReturn(UserRole.ROLE_ADMIN);
            when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

            // when & then
            assertThatCode(() -> validationUtils.validateMentorRole(userId, groupId))
                    .doesNotThrowAnyException();

            // role 조회 DB 호출이 발생하지 않아야 함
            verify(groupMemberRepository, never()).findRoleByUserIdAndStudyId(any(), any());
        }

        @Test
        @DisplayName("일반 유저가 MENTOR 역할이면 통과한다")
        void regular_user_passes_if_mentor() {
            // given
            Long userId = 2L;
            Long groupId = 10L;
            User regularUser = mock(User.class);
            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId))
                    .thenReturn(Optional.of(GroupMemberRole.MENTOR));

            // when & then
            assertThatCode(() -> validationUtils.validateMentorRole(userId, groupId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("일반 유저가 MENTEE 역할이면 UNAUTHORIZED_MENTOR_ROLE 예외가 발생한다")
        void regular_user_fails_if_mentee() {
            // given
            Long userId = 3L;
            Long groupId = 10L;
            User regularUser = mock(User.class);
            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId))
                    .thenReturn(Optional.of(GroupMemberRole.MENTEE));

            // when & then
            assertThatThrownBy(() -> validationUtils.validateMentorRole(userId, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.UNAUTHORIZED_MENTOR_ROLE));
        }

        @Test
        @DisplayName("일반 유저가 그룹 멤버가 아니면 STUDY_USER_NOT_FOUND 예외가 발생한다")
        void regular_user_fails_if_not_in_group() {
            // given
            Long userId = 4L;
            Long groupId = 10L;
            User regularUser = mock(User.class);
            when(regularUser.getRole()).thenReturn(UserRole.ROLE_USER);
            when(userRepository.findById(userId)).thenReturn(Optional.of(regularUser));
            when(groupMemberRepository.findRoleByUserIdAndStudyId(userId, groupId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> validationUtils.validateMentorRole(userId, groupId))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.STUDY_USER_NOT_FOUND));
        }
    }
}
