package com.mjsec.lms.security;

import com.mjsec.lms.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/**",
            "/api/v1/user/password/**"
    );

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("Starting JWTFilter for request: {}", requestURI);

        // 컨텍스트패스 제거한 URI로 매칭 (로그용 requestURI는 그대로 유지)
        final String matchURI;
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && requestURI.startsWith(ctx)) {
            matchURI = requestURI.substring(ctx.length()); // "/api/..." 형태
        } else {
            matchURI = requestURI;
        }

        // CORS 프리플라이트는 바로 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Public Endpoint 인지 확인 (컨텍스트패스 제거된 URI 사용)
        boolean isPublic = PUBLIC_ENDPOINTS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, matchURI));

        if (isPublic) {
            log.info("Skipping JWT filter for public endpoint: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or malformed");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header missing or malformed");
            return;
        }

        String accessToken = authorizationHeader.substring(7);

        if (jwtService.isExpired(accessToken)) {
            log.warn("Access token expired");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access token expired");
            return;
        }

        String tokenType = jwtService.getTokenType(accessToken);
        if (!"accessToken".equals(tokenType)) {
            log.warn("Invalid token type: {}", tokenType);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid access token");
            return;
        }

        Long studentNumber = jwtService.getStudentNumber(accessToken);
        String role = jwtService.getRole(accessToken);

        log.info("Authenticated user: {}, Role: {}", studentNumber, role);

        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        Authentication authToken = new UsernamePasswordAuthenticationToken(studentNumber, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
        log.info("Completed JWTFilter for request: {}", requestURI);
    }
}
