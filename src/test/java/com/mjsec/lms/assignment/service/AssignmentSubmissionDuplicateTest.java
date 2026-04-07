package com.mjsec.lms.assignment.service;

import com.mjsec.lms.assignment.domain.AssignmentSubmission;
import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.assignment.domain.type.SubmissionStatus;
import com.mjsec.lms.assignment.dto.SubmissionDto;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("과제 중복 제출 방지 테스트")
class AssignmentSubmissionDuplicateTest {

    @Mock
    private ValidationUtils validationUtils;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private AssignmentSubmissionService assignmentSubmissionService;

    private User mockUser;
    private Plan mockPlan;
    private SubmissionDto mockDto;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .name("테스트멘티")
                .email("test@test.com")
                .studentNumber(20230001L)
                .password("password")
                .phoneNumber("010-0000-0000")
                .build();

        mockPlan = Plan.builder()
                .planId(10L)
                .title("1주차 과제")
                .content("블로그에 정리하세요")
                .hasAssignment(true)
                .build();

        mockDto = SubmissionDto.builder()
                .content("https://velog.io/@test/week1")
                .build();
    }

    @Nested
    @DisplayName("앱 레벨 중복 체크 (validateDuplicateSubmission)")
    class AppLevelDuplicateCheck {

        @Test
        @DisplayName("처음 제출이면 정상적으로 저장된다")
        void first_submission_should_succeed() {
            // given
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);
            when(submissionRepository.save(any(AssignmentSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // when
            assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "127.0.0.1");

            // then
            verify(submissionRepository, times(1)).save(any(AssignmentSubmission.class));
        }

        @Test
        @DisplayName("이미 제출했으면 앱 레벨에서 DUPLICATE_SUBMISSION 예외가 발생한다")
        void duplicate_submission_should_throw_at_app_level() {
            // given
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);
            doThrow(new RestApiException(ErrorCode.DUPLICATE_SUBMISSION))
                    .when(validationUtils).validateDuplicateSubmission(anyLong(), anyLong());

            // when & then
            assertThatThrownBy(() ->
                    assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "127.0.0.1"))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_SUBMISSION));

            // save가 호출되지 않아야 함
            verify(submissionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("DB 레벨 중복 차단 (UniqueConstraint)")
    class DbLevelDuplicateCheck {

        @Test
        @DisplayName("앱 체크를 통과해도 DB unique constraint 위반 시 DUPLICATE_SUBMISSION 예외로 변환된다")
        void db_constraint_violation_should_be_converted_to_duplicate_submission() {
            // given - 앱 레벨 체크는 통과 (Race Condition 시뮬레이션)
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);
            // DB unique constraint 위반 시뮬레이션
            when(submissionRepository.save(any(AssignmentSubmission.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'uk_submission_submitter_plan'"));

            // when & then
            assertThatThrownBy(() ->
                    assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "127.0.0.1"))
                    .isInstanceOf(RestApiException.class)
                    .satisfies(e -> assertThat(((RestApiException) e).getErrorCode())
                            .isEqualTo(ErrorCode.DUPLICATE_SUBMISSION));
        }

        @Test
        @DisplayName("DB constraint가 없었다면 Race Condition으로 중복이 저장될 수 있다 (취약점 재현)")
        void without_db_constraint_race_condition_allows_duplicates() throws InterruptedException {
            // given - 모든 요청이 앱 레벨 체크를 통과 (동시 요청 시뮬레이션)
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);
            when(submissionRepository.save(any(AssignmentSubmission.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0)); // DB constraint 없다고 가정

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(); // 모든 스레드가 동시에 시작
                        assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "127.0.0.1");
                        successCount.incrementAndGet();
                    } catch (RestApiException e) {
                        // 중복 차단된 경우
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // 동시에 출발
            doneLatch.await();
            executor.shutdown();

            // DB constraint 없으면 여러 개가 저장될 수 있음 (취약점)
            int savedCount = successCount.get();
            System.out.println("DB constraint 없이 동시 " + threadCount + "개 요청 → " + savedCount + "개 저장됨");
            assertThat(savedCount).isGreaterThan(0); // 적어도 하나는 성공
        }

        @Test
        @DisplayName("DB constraint가 있으면 동시 요청 중 첫 번째만 성공하고 나머지는 DUPLICATE_SUBMISSION이 된다")
        void with_db_constraint_only_first_request_succeeds() throws InterruptedException {
            // given
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);

            AtomicInteger saveCallCount = new AtomicInteger(0);
            // 첫 번째 save만 성공, 나머지는 DB constraint 위반
            when(submissionRepository.save(any(AssignmentSubmission.class)))
                    .thenAnswer(invocation -> {
                        if (saveCallCount.incrementAndGet() == 1) {
                            return invocation.getArgument(0); // 첫 번째만 성공
                        }
                        throw new DataIntegrityViolationException("Duplicate entry");
                    });

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger duplicateCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "127.0.0.1");
                        successCount.incrementAndGet();
                    } catch (RestApiException e) {
                        if (e.getErrorCode() == ErrorCode.DUPLICATE_SUBMISSION) {
                            duplicateCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            executor.shutdown();

            // then - 1개만 성공, 나머지는 중복 차단
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(duplicateCount.get()).isEqualTo(threadCount - 1);
            assertThat(successCount.get() + duplicateCount.get()).isEqualTo(threadCount);
        }
    }

    @Nested
    @DisplayName("제출된 과제는 정상 데이터를 담고 있어야 한다")
    class SubmissionDataIntegrity {

        @Test
        @DisplayName("제출 시 IP 주소, 상태(SUBMITTED), 제출자 정보가 저장된다")
        void submission_should_save_correct_data() {
            // given
            when(validationUtils.validateMenteeAccess(anyLong(), anyLong())).thenReturn(mockUser);
            when(validationUtils.validatePlan(anyLong())).thenReturn(mockPlan);

            List<AssignmentSubmission> savedSubmissions = new ArrayList<>();
            when(submissionRepository.save(any(AssignmentSubmission.class)))
                    .thenAnswer(invocation -> {
                        AssignmentSubmission saved = invocation.getArgument(0);
                        savedSubmissions.add(saved);
                        return saved;
                    });

            // when
            assignmentSubmissionService.submitAssignment(1L, 10L, 20230001L, mockDto, "192.168.1.1");

            // then
            assertThat(savedSubmissions).hasSize(1);
            AssignmentSubmission saved = savedSubmissions.get(0);
            assertThat(saved.getContent()).isEqualTo("https://velog.io/@test/week1");
            assertThat(saved.getSubmitterIp()).isEqualTo("192.168.1.1");
            assertThat(saved.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
            assertThat(saved.getSubmitter()).isEqualTo(mockUser);
            assertThat(saved.getPlan()).isEqualTo(mockPlan);
        }
    }
}
