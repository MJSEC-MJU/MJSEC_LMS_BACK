package com.mjsec.lms.notification.service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import static org.assertj.core.api.Assertions.*;

@DisplayName("알림 스케줄 시간대 테스트")
class ScheduledTaskServiceZoneTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @ParameterizedTest
    @DisplayName("알림 cron은 JVM 기본 시간대가 아니라 서울 시각으로 해석된다")
    @CsvSource({
            // 메서드, 기준 시각(UTC), 다음 발송 시각(서울)
            // UTC로 해석하면 각각 10-05 09:00, 10-01 18:00(서울)에 발송됨
            "checkAssignmentNotSubmitted, 2026-09-30T01:00, 2026-10-05T00:00",
            "checkAbsenceAlert,           2026-09-30T01:00, 2026-10-01T09:00"
    })
    void cronUsesSeoulZone(String methodName, String baseUtc, String expectedNextSeoul) throws Exception {
        Method method = ScheduledTaskService.class.getMethod(methodName);
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");

        ZonedDateTime base = LocalDateTime.parse(baseUtc).atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.of(scheduled.zone()));
        ZonedDateTime next = CronExpression.parse(scheduled.cron()).next(base);

        assertThat(next.withZoneSameInstant(SEOUL).toLocalDateTime())
                .isEqualTo(LocalDateTime.parse(expectedNextSeoul));
    }
}
