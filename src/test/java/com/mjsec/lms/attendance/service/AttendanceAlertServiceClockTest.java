package com.mjsec.lms.attendance.service;

import com.mjsec.lms.attendance.repository.AttendanceRepository;
import com.mjsec.lms.notification.external.EmailService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("결석 알림 조회 기준 날짜 테스트")
class AttendanceAlertServiceClockTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Mock
    private AttendanceRepository attendanceRepository;
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
    @DisplayName("JVM 기본 시간대가 UTC여도 서울 날짜 기준 10일 전을 조회한다")
    void absenceAlertUsesSeoulDate() {
        when(attendanceRepository.findByAttendanceDate(any())).thenReturn(List.of());
        AttendanceAlertService service = new AttendanceAlertService(attendanceRepository, emailService);

        service.checkAndSendAbsenceAlert();

        // 서울과 UTC 날짜가 다른 시간대(00~09시 KST)에만 수정 전 코드와 구별된다
        verify(attendanceRepository).findByAttendanceDate(LocalDate.now(SEOUL).minusDays(10));
    }
}
