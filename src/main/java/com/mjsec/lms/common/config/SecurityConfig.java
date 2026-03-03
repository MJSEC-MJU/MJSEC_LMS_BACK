package com.mjsec.lms.common.config;

import com.mjsec.lms.user.repository.UserRepository;
import com.mjsec.lms.common.security.CustomLoginFilter;
import com.mjsec.lms.common.security.CustomLogoutFilter;
import com.mjsec.lms.common.security.JwtFilter;
import com.mjsec.lms.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

// 추가 (프리플라이트 허용용)
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {

                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000"));
                        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Set-Cookie", "X-Requested-With", "Accept"));
                        configuration.setMaxAge(3600L);
                        configuration.setExposedHeaders(Arrays.asList("Authorization", "access", "X-Custom-Header"));

                        //환경변수 기반 허용 도메인 덧붙이기
                        String corsAllowed = System.getenv("CORS_ALLOWED_ORIGINS"); // 콤마구분 목록
                        String domain = System.getenv("CORS_DOMAIN");
                        if (domain == null || domain.isBlank()) {
                            domain = System.getenv("DOMAIN");
                        }

                        if (corsAllowed != null && !corsAllowed.isBlank()) {
                            for (String s : corsAllowed.split("\\s*,\\s*")) {
                                if (!s.isBlank()) {
                                    configuration.addAllowedOriginPattern(s.trim());
                                }
                            }
                        } else if (domain != null && !domain.isBlank()) {
                            configuration.addAllowedOriginPattern("https://" + domain);
                            configuration.addAllowedOriginPattern("https://*." + domain);
                        }
                        configuration.addAllowedOriginPattern("http://localhost:5173");
                        configuration.addAllowedOriginPattern("http://localhost:8080");
                        configuration.addAllowedHeader("Origin");

                        return configuration;
                    }
                }));

        http
                .csrf((auth) -> auth.disable());

        http
                .formLogin((auth) -> auth.disable());

        http
                .httpBasic((auth) -> auth.disable());

        http
                .addFilterBefore(new CustomLoginFilter(userRepository, jwtService, passwordEncoder), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new JwtFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CustomLogoutFilter(), LogoutFilter.class);

        // 경로별 인가 작업
        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll() // 업로드된 파일 접근 허용
                        .requestMatchers("/swagger-ui/*", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/group/**").permitAll()
                        .requestMatchers("/api/v1/image/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/user/create-announcement").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/user/announcements/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/user/password/**").permitAll()
                        .requestMatchers("/api/v1/user/user-page").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/test/**").permitAll() //임시로 허용
                        .requestMatchers("/api/v1/mentor/**").hasAnyRole("ADMIN", "USER")
                );

        return http.build();
    }
}