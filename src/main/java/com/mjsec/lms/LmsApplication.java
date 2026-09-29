package com.mjsec.lms;

import com.mjsec.lms.config.TimeZones;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LmsApplication {

	public static void main(String[] args) {
		applyDefaultTimeZone();
		SpringApplication.run(LmsApplication.class, args);
	}

	// 컨테이너 기본값(UTC)을 따르면 LocalDateTime.now(), 생성 시각이 9시간 어긋나서 서울로 고정함
	static void applyDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(TimeZones.SEOUL_ZONE));
	}

}
