package com.mjsec.lms.config;

import com.mjsec.lms.domain.user.User;
import com.mjsec.lms.repository.user.UserRepository;
import com.mjsec.lms.type.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

// Admin 계정 생성을 위한 Configuration Class (테스트 단계에서만 사용)
@Configuration
public class AdminUserInitializer {

    @Value("${admin.number}")
    private Long adminNumber;

    @Value("${admin.password}")
    private String adminPassword;

    @Bean
    public CommandLineRunner createAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {

        return args -> {
            if (!userRepository.existsByStudentNumber(adminNumber)) {
                User admin = User.builder()
                        .studentNumber(adminNumber)
                        .password(passwordEncoder.encode(adminPassword))
                        .name("관리자")
                        .email("admin@example.com")
                        .phoneNumber("010-3727-3727")
                        .role(UserRole.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);

                System.out.println("관리자 계정 생성 완료: " + adminNumber);
            }
        };
    }
}
