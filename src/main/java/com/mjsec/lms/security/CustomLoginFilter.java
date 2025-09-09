package com.mjsec.lms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mjsec.lms.domain.User;
import com.mjsec.lms.dto.AuthDto;
import com.mjsec.lms.repository.UserRepository;
import com.mjsec.lms.service.JwtService;
import com.mjsec.lms.type.ErrorCode;
import com.mjsec.lms.type.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
public class CustomLoginFilter extends GenericFilterBean {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON 변환용

    public CustomLoginFilter(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        doFilter((HttpServletRequest) request, (HttpServletResponse) response, filterChain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        // 로그인 요청(POST)이 아닐 경우, 다음 필터로 넘김
        if (!request.getServletPath().equalsIgnoreCase("/api/v1/auth/login") ||
                !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 본문에서 JSON 데이터 추출
        AuthDto.LogIn loginRequest;
        try {
            loginRequest = objectMapper.readValue(request.getInputStream(), AuthDto.LogIn.class);
        } catch (IOException e) {
            log.error("Invalid login request format: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, ErrorCode.INVALID_REQUEST_FORMAT);
            return;
        }

        // 학번 검증 (학번이 존재하지 않는 경우)
        User user = userRepository.findByStudentNumber(loginRequest.getStudentNumber()).orElse(null);
        if (user == null) {
            log.warn("Invalid login attempt: nonexistent student number {}", loginRequest.getStudentNumber());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.INVALID_STUDENT_NUMBER);
            return;
        }

        // 비밀번호 검증 (비밀번호가 틀린 경우)
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Invalid login attempt: wrong password for student number {}", loginRequest.getStudentNumber());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.INVALID_PASSWORD);
            return;
        }

        // AccessToken 발급
        final long ACCESS_TOKEN_EXPIRY = 43_200_000L; // 12시간

        // User 권한 정보 획득
        UserRole role = user.getRole();

        // AccessToken 생성
        String accessToken = jwtService.createJwt(
                "accessToken",
                user.getStudentNumber(),
                String.valueOf(role),
                ACCESS_TOKEN_EXPIRY
        );

        // AccessToken 헤더에 추가
        response.setHeader("Authorization", "Bearer " + accessToken);

        // 성공 응답 (테스트 단계에서만 사용)
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> message = new HashMap<>();
        message.put("message", "로그인 성공!");
        message.put("accessToken", accessToken);

        objectMapper.writeValue(response.getWriter(), message);

        log.info("User '{}' logged in successfully", user.getStudentNumber());
    }

    private void sendErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", errorCode.name());
        errorResponse.put("message", errorCode.getDescription());

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
