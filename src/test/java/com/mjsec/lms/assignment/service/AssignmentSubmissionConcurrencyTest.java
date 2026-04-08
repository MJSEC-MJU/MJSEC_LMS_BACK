package com.mjsec.lms.assignment.service;

import com.mjsec.lms.assignment.domain.AssignmentSubmission;
import com.mjsec.lms.assignment.domain.Plan;
import com.mjsec.lms.assignment.dto.SubmissionDto;
import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.common.exception.RestApiException;
import com.mjsec.lms.common.type.ErrorCode;
import com.mjsec.lms.common.util.ValidationUtils;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import com.mjsec.lms.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 과제 동시 제출 Race Condition 방지 테스트
 *
 * 비관적 락의 실제 블로킹 효과는 DB 트랜잭션 수준에서 동작하므로
 * 순수 단위 테스트로는 검증할 수 없다.
 *
 * 이 테스트는 다음 두 가지를 검증한다:
 *   1. 서비스가 모든 동시 요청에서 락 획득 → 중복 체크 → 저장 순서를 유지하는가
 *   2. 중복 체크 로직(AtomicBoolean으로 DB의 원자적 동작을 시뮬레이션)이
 *      동시 요청 환경에서 정확히 1건만 통과시키는가
 *
 * Mockito mock은 thread-safe하지 않으므로, 스레드 실행 전에 모든 stubbing을
 * 완료하고 실행 중에는 새로운 stubbing을 추가하지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssignmentSubmissionService - 동시 제출 Race Condition 테스트")
class AssignmentSubmissionConcurrencyTest {

    @Mock private ValidationUtils validationUtils;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private PlanRepository planRepository;

    private AssignmentSubmissionService service;

    private static final Long GROUP_ID       = 1L;
    private static final Long PLAN_ID        = 10L;
    private static final Long USER_ID        = 100L;
    private static final Long STUDENT_NUMBER = 20210001L;
    private static final String IP           = "127.0.0.1";

    private User user;
    private Plan plan;

    @BeforeEach
    void setUp() {
        service = new AssignmentSubmissionService(
                validationUtils, submissionRepository, groupMemberRepository, planRepository
        );

        // 스레드 실행 전에 모든 stubbing을 완료한다.
        // Mockito mock은 thread-safe하지 않으므로 실행 도중 stubbing 호출을 피한다.
        user = mock(User.class);
        plan = mock(Plan.class);
        when(user.getUserId()).thenReturn(USER_ID);
        when(user.getName()).thenReturn("홍길동");
        when(plan.getPlanId()).thenReturn(PLAN_ID);

        when(validationUtils.validateMenteeAccess(GROUP_ID, STUDENT_NUMBER)).thenReturn(user);
        when(validationUtils.validatePlan(PLAN_ID)).thenReturn(plan);
        when(planRepository.findByIdWithPessimisticLock(PLAN_ID)).thenReturn(Optional.of(plan));
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("동일 사용자가 동시에 N번 제출해도 정확히 1건만 성공한다")
    void only_one_submission_succeeds_under_concurrent_requests() throws Exception {
        final int threadCount = 5;

        // 비관적 락이 보장하는 효과를 시뮬레이션:
        // 실제 DB에서는 락 덕분에 트랜잭션이 직렬화되어 두 번째 스레드가
        // 중복 체크를 수행할 때 이미 첫 번째 커밋 결과가 반영된다.
        // AtomicBoolean의 compareAndSet이 동일한 원자성을 흉내낸다.
        AtomicBoolean submitted = new AtomicBoolean(false);
        doAnswer(invocation -> {
            if (!submitted.compareAndSet(false, true)) {
                throw new RestApiException(ErrorCode.DUPLICATE_SUBMISSION);
            }
            return null;
        }).when(validationUtils).validateDuplicateSubmission(USER_ID, PLAN_ID);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount    = new AtomicInteger(0);
        CountDownLatch startLatch  = new CountDownLatch(1);
        SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 출발하도록 동기화
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP);
                    successCount.incrementAndGet();
                } catch (RestApiException e) {
                    if (e.getErrorCode() == ErrorCode.DUPLICATE_SUBMISSION) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startLatch.countDown(); // 전체 스레드 동시 출발
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(successCount.get())
                .as("정확히 1건만 제출에 성공해야 한다")
                .isEqualTo(1);
        assertThat(failCount.get())
                .as("나머지 %d건은 DUPLICATE_SUBMISSION으로 차단되어야 한다", threadCount - 1)
                .isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("동시 제출 시 비관적 락은 모든 요청에서 각각 호출된다")
    void pessimistic_lock_is_called_for_every_concurrent_request() throws Exception {
        final int threadCount = 3;

        AtomicBoolean submitted = new AtomicBoolean(false);
        doAnswer(invocation -> {
            if (!submitted.compareAndSet(false, true)) {
                throw new RestApiException(ErrorCode.DUPLICATE_SUBMISSION);
            }
            return null;
        }).when(validationUtils).validateDuplicateSubmission(anyLong(), anyLong());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(threadCount);
        SubmissionDto dto = SubmissionDto.builder().content("https://blog.example.com").build();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    service.submitAssignment(GROUP_ID, PLAN_ID, STUDENT_NUMBER, dto, IP);
                } catch (RestApiException | InterruptedException ignored) {
                    if (Thread.interrupted()) Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(); // 모든 스레드가 끝날 때까지 대기
        executor.shutdown();

        // 락 획득 시도는 요청 수만큼 반드시 발생해야 한다
        verify(planRepository, times(threadCount)).findByIdWithPessimisticLock(PLAN_ID);
    }
}
