package com.mjsec.lms;

import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@DisplayName("서버 기본 시간대 설정 테스트")
class LmsApplicationTimeZoneTest {

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
        // 운영 컨테이너처럼 JVM 기본 시간대가 UTC인 상황을 만든다
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("JVM 기본 시간대가 UTC여도 Asia/Seoul로 맞춘다")
    void setsDefaultTimeZoneToSeoul() {
        LmsApplication.applyDefaultTimeZone();

        assertThat(TimeZone.getDefault().getID()).isEqualTo("Asia/Seoul");
    }

    @Test
    @DisplayName("main()은 스프링을 띄우기 전에 시간대를 서울로 맞춘다")
    void mainAppliesSeoulBeforeSpringStarts() {
        AtomicReference<String> zoneAtRun = new AtomicReference<>();

        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(eq(LmsApplication.class), any(String[].class)))
                    .thenAnswer(invocation -> {
                        zoneAtRun.set(TimeZone.getDefault().getID());
                        return null;
                    });

            LmsApplication.main(new String[0]);
        }

        assertThat(zoneAtRun.get()).isEqualTo("Asia/Seoul");
    }
}
