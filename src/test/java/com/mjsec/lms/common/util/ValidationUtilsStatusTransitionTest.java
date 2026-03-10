package com.mjsec.lms.common.util;

import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mjsec.lms.user.repository.UserRepository;
import com.mjsec.lms.studygroup.repository.StudyGroupRepository;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.assignment.repository.PlanCommentRepository;
import com.mjsec.lms.studygroup.repository.StudyActivityRepository;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("과제 피드백 상태 전이 테스트")
class ValidationUtilsStatusTransitionTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudyGroupRepository studyGroupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private PlanCommentRepository planCommentRepository;
    @Mock
    private StudyActivityRepository studyActivityRepository;

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

    @Nested
    @DisplayName("멘티가 과제를 제출한 상태 (SUBMITTED)")
    class FromSubmittedStatus {

        @Test
        @DisplayName("멘토가 '완료' 처리할 수 있다")
        void submitted_to_completed_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.SUBMITTED;
            SubmissionStatus newStatus = SubmissionStatus.COMPLETED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("멘토가 '수정필요' 처리할 수 있다")
        void submitted_to_revision_required_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.SUBMITTED;
            SubmissionStatus newStatus = SubmissionStatus.REVISION_REQUIRED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 제출된 상태를 다시 '제출됨'으로 바꿀 수 없다")
        void submitted_to_submitted_should_fail() {
            // given
            SubmissionStatus current = SubmissionStatus.SUBMITTED;
            SubmissionStatus newStatus = SubmissionStatus.SUBMITTED;

            // when & then
            assertThatThrownBy(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(exception -> {
                        RestApiException restApiException = (RestApiException) exception;
                        assertThat(restApiException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });
        }
    }

    @Nested
    @DisplayName("멘토가 수정을 요청한 상태 (REVISION_REQUIRED)")
    class FromRevisionRequiredStatus {

        @Test
        @DisplayName("멘티가 재제출하면 '제출됨' 상태로 바뀐다")
        void revision_required_to_submitted_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.REVISION_REQUIRED;
            SubmissionStatus newStatus = SubmissionStatus.SUBMITTED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("멘토가 재제출 없이 바로 '완료' 처리할 수 있다")
        void revision_required_to_completed_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.REVISION_REQUIRED;
            SubmissionStatus newStatus = SubmissionStatus.COMPLETED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 수정필요 상태를 다시 '수정필요'로 바꿀 수 없다")
        void revision_required_to_revision_required_should_fail() {
            // given
            SubmissionStatus current = SubmissionStatus.REVISION_REQUIRED;
            SubmissionStatus newStatus = SubmissionStatus.REVISION_REQUIRED;

            // when & then
            assertThatThrownBy(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(exception -> {
                        RestApiException restApiException = (RestApiException) exception;
                        assertThat(restApiException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });
        }
    }

    @Nested
    @DisplayName("과제가 완료된 상태 (COMPLETED)")
    class FromCompletedStatus {

        @Test
        @DisplayName("멘토가 다시 '수정필요' 처리할 수 있다")
        void completed_to_revision_required_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.COMPLETED;
            SubmissionStatus newStatus = SubmissionStatus.REVISION_REQUIRED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("멘토가 피드백 내용만 수정할 수 있다 (상태 유지)")
        void completed_to_completed_should_succeed() {
            // given
            SubmissionStatus current = SubmissionStatus.COMPLETED;
            SubmissionStatus newStatus = SubmissionStatus.COMPLETED;

            // when & then
            assertThatCode(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("완료된 과제를 '제출됨' 상태로 되돌릴 수 없다")
        void completed_to_submitted_should_fail() {
            // given
            SubmissionStatus current = SubmissionStatus.COMPLETED;
            SubmissionStatus newStatus = SubmissionStatus.SUBMITTED;

            // when & then
            assertThatThrownBy(() -> validationUtils.validateStatusTransition(current, newStatus))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(exception -> {
                        RestApiException restApiException = (RestApiException) exception;
                        assertThat(restApiException.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });
        }
    }

    @Nested
    @DisplayName("전체 상태 전이 규칙 검증")
    class StatusTransitionMatrix {

        @Test
        @DisplayName("허용된 모든 상태 변경이 정상 동작한다")
        void all_valid_transitions_should_succeed() {
            Object[][] validTransitions = {
                    {SubmissionStatus.SUBMITTED, SubmissionStatus.COMPLETED, "제출됨 → 완료"},
                    {SubmissionStatus.SUBMITTED, SubmissionStatus.REVISION_REQUIRED, "제출됨 → 수정필요"},
                    {SubmissionStatus.REVISION_REQUIRED, SubmissionStatus.SUBMITTED, "수정필요 → 제출됨 (재제출)"},
                    {SubmissionStatus.REVISION_REQUIRED, SubmissionStatus.COMPLETED, "수정필요 → 완료 (멘토 직접 완료)"},
                    {SubmissionStatus.COMPLETED, SubmissionStatus.REVISION_REQUIRED, "완료 → 수정필요"},
                    {SubmissionStatus.COMPLETED, SubmissionStatus.COMPLETED, "완료 → 완료 (피드백만 수정)"}
            };

            for (Object[] transition : validTransitions) {
                SubmissionStatus from = (SubmissionStatus) transition[0];
                SubmissionStatus to = (SubmissionStatus) transition[1];
                String description = (String) transition[2];

                assertThatCode(() -> validationUtils.validateStatusTransition(from, to))
                        .as("[%s] 전이가 허용되어야 합니다", description)
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("허용되지 않은 상태 변경은 에러가 발생한다")
        void all_invalid_transitions_should_fail() {
            Object[][] invalidTransitions = {
                    {SubmissionStatus.SUBMITTED, SubmissionStatus.SUBMITTED, "제출됨 → 제출됨"},
                    {SubmissionStatus.REVISION_REQUIRED, SubmissionStatus.REVISION_REQUIRED, "수정필요 → 수정필요"},
                    {SubmissionStatus.COMPLETED, SubmissionStatus.SUBMITTED, "완료 → 제출됨"}
            };

            for (Object[] transition : invalidTransitions) {
                SubmissionStatus from = (SubmissionStatus) transition[0];
                SubmissionStatus to = (SubmissionStatus) transition[1];
                String description = (String) transition[2];

                assertThatThrownBy(() -> validationUtils.validateStatusTransition(from, to))
                        .as("[%s] 변경은 차단되어야 합니다", description)
                        .isInstanceOf(RestApiException.class);
            }
        }
    }
}
