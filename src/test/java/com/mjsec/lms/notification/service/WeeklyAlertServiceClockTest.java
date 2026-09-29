package com.mjsec.lms.notification.service;

import com.mjsec.lms.assignment.repository.PlanRepository;
import com.mjsec.lms.assignment.repository.SubmissionRepository;
import com.mjsec.lms.notification.external.EmailService;
import com.mjsec.lms.studygroup.repository.GroupMemberRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("주간 리포트 조회 기준 시각 테스트")
class WeeklyAlertServiceClockTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private PlanRepository planRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private EmailService emailService;

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        // main()의 기본 시간대 고정이 빠진 상황을 만든다
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("JVM 기본 시간대가 UTC여도 서울 시각 기준 지난 1주를 조회한다")
    void weeklyReportUsesSeoulNow() {
        when(planRepository.findAssignmentsExpiredBetween(any(), any())).thenReturn(List.of());
        WeeklyAlertService service = new WeeklyAlertService(
                planRepository, submissionRepository, groupMemberRepository, emailService);

        service.checkAndSendWeeklyAssignmentReport();

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(planRepository).findAssignmentsExpiredBetween(start.capture(), end.capture());
        assertThat(Duration.between(LocalDateTime.now(SEOUL), end.getValue()).abs())
                .isLessThan(Duration.ofMinutes(1));
        assertThat(start.getValue()).isEqualTo(end.getValue().minusWeeks(1));
    }
}
