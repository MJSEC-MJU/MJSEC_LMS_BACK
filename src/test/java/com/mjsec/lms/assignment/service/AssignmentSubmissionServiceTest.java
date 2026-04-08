package com.mjsec.lms.assignment.service;

import com.mjsec.lms.assignment.domain.AssignmentSubmission;
import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.assignment.dto.SubmissionDto;
import com.mjsec.lms.assignment.dto.SubmissionResponse;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.common.util.ValidationUtils;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// mockUser() 헬퍼가 getName()/getUserId()를 미리 stub하는데,
// 예외 경로 테스트에서는 일부가 실제로 호출되지 않아 STRICT_STUBS에서 실패한다.
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentSubmissionService - 과제 제출 단위 테스트")
class AssignmentSubmissionServiceTest {

    @Mock private ValidationUtils validationUtils;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private PlanRepository planRepository;

    private AssignmentSubmissionService service;

    private static final Long GROUP_ID       = 1L;
    private static final Long PLAN_ID        = 10L;
    private static final Long USER_ID        = 100L;
    private static final Long STUDENT_NUMBER = 20210001L;
    private static final String IP_ADDRESS   = "127.0.0.1";

    @BeforeEach
    void setUp() {
        service = new AssignmentSubmissionService(
                validationUtils, submissionRepository, groupMemberRepository, planRepository
        );
    }

    // =====================================================================
    // 정상 제출 흐름
    // =====================================================================

    @Nested
    @DisplayName("정상 제출")
    class NormalSubmission {

        @Test
        @DisplayName("모든 검증을 통과하면 과제가 저장되고 응답이 반환된다")
        void submit_success() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
            when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

            // when
            SubmissionResponse response = service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCreatorName()).isEqualTo("홍길동");
            verify(submissionRepository).save(any(AssignmentSubmission.class));
        }

        @Test
        @DisplayName("비관적 락 획득이 반드시 중복 체크보다 먼저 수행된다 (순서 보장)")
        void lock_acquired_before_duplicate_check() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
            when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

            // when
            service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS);

            // then — 락 획득 → 중복 체크 순서가 반드시 지켜져야 한다
            InOrder inOrder = inOrder(planRepository, validationUtils);
            inOrder.verify(planRepository).findByIdWithPessimisticLock(PLAN_ID);
            inOrder.verify(validationUtils).validateDuplicateSubmission(USER_ID, PLAN_ID);
        }

        @Test
        @DisplayName("비관적 락은 매 제출 요청마다 정확히 1회 호출된다")
        void lock_is_called_exactly_once_per_request() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
            when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));

            // when
            service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS);

            // then
            verify(planRepository, times(1)).findByIdWithPessimisticLock(PLAN_ID);
        }
    }

    // =====================================================================
    // 비관적 락 획득 단계 예외
    // =====================================================================

    @Nested
    @DisplayName("비관적 락 획득 단계 예외")
    class PessimisticLockFailure {

        @Test
        @DisplayName("락 획득 시 plan이 존재하지 않으면 PLAN_NOT_FOUND 예외가 발생한다")
        void plan_not_found_during_lock_throws_exception() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.PLAN_NOT_FOUND));
        }

        @Test
        @DisplayName("락 획득 실패 시 중복 체크와 저장이 수행되지 않는다")
        void no_duplicate_check_or_save_when_lock_fails() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.empty());

            // when
            assertThatThrownBy(() ->
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS))
                    .isInstanceOf(RestApiException.class);

            // then — 락 실패 이후 단계는 실행되지 않아야 한다
            verify(validationUtils, never()).validateDuplicateSubmission(anyLong(), anyLong());
            verify(submissionRepository, never()).save(any());
        }
    }

    // =====================================================================
    // 중복 제출 차단
    // =====================================================================

    @Nested
    @DisplayName("중복 제출 차단")
    class DuplicateSubmission {

        @Test
        @DisplayName("이미 제출한 사용자가 다시 제출하면 DUPLICATE_SUBMISSION 예외가 발생한다")
        void duplicate_submission_throws_exception() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
            doThrow(new RestApiException(ErrorCode.DUPLICATE_SUBMISSION))
                    .when(validationUtils).validateDuplicateSubmission(USER_ID, PLAN_ID);

            // when & then
            assertThatThrownBy(() ->
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_SUBMISSION));
        }

        @Test
        @DisplayName("중복 제출이 차단되면 save가 호출되지 않는다")
        void no_save_when_duplicate_detected() {
            // given
            User user = mockUser(USER_ID, "홍길동");
            Plan plan = mockPlan(PLAN_ID);
            SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

            when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
            when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
            when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
            doThrow(new RestApiException(ErrorCode.DUPLICATE_SUBMISSION))
                    .when(validationUtils).validateDuplicateSubmission(anyLong(), anyLong());

            // when
            assertThatThrownBy(() ->
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP_ADDRESS))
                    .isInstanceOf(RestApiException.class);

            // then
            verify(submissionRepository, never()).save(any());
        }
    }

    // =====================================================================
    // 헬퍼
    // =====================================================================

    private User mockUser(Long userId, String name) {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(userId);
        when(user.getName()).thenReturn(name);
        return user;
    }

    private Plan mockPlan(Long planId) {
        Plan plan = mock(Plan.class);
        when(plan.getPlanId()).thenReturn(planId);
        return plan;
    }
}
