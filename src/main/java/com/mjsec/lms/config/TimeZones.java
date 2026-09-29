package com.mjsec.lms.config;

import java.time.ZoneId;

// 서비스 기준 시간대, 시각을 다루는 코드는 JVM 기본값 대신 이 값을 씀
// application.yml의 hibernate.jdbc.time_zone과 같아야 LocalDateTime이 저장될 때 밀리지 않음
public final class TimeZones {

    // @Scheduled(zone = ...)처럼 컴파일 타임 상수가 필요한 곳에서 씀
    public static final String SEOUL = "Asia/Seoul";

    public static final ZoneId SEOUL_ZONE = ZoneId.of(SEOUL);

    private TimeZones() {
        throw new IllegalStateException("Utility class");
    }
}
