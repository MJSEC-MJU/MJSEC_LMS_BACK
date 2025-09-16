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

    private static final List<String> PUBLIC_API = List.of(
        "/api/v1/auth/**",
        "/api/v1/user/password/**",
        "/api/v1/image/**"
    );

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String requestURI = request.getRequestURI(); // 예: /lms/api/v1/image/abc.png
        String ctx = request.getContextPath();       // 보통 "/lms" 또는 ""
        String matchURI = (ctx != null && !ctx.isEmpty() && requestURI.startsWith(ctx))
                ? requestURI.substring(ctx.length()) // 예: /api/v1/image/abc.png
                : requestURI;

        for (String pattern : PUBLIC_API) {
            if (pathMatcher.match(pattern, matchURI)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.info("Starting JWTFilter for request: {}", requestURI);

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header missing or malformed");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization header missing or malformed");
            return;
        }

        String accessToken = authorizationHeader.substring(7);
        try {
            if (jwtService.isExpired(accessToken)) {
                log.warn("Access token expired");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token expired");
                return;
            }

            String tokenType = jwtService.getTokenType(accessToken);
            if (!"accessToken".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
                return;
            }

            Long studentNumber = jwtService.getStudentNumber(accessToken);
            String role = jwtService.getRole(accessToken);

            log.info("Authenticated user: {}, Role: {}", studentNumber, role);

            Authentication authToken = new UsernamePasswordAuthenticationToken(
                studentNumber, null, Collections.singletonList(new SimpleGrantedAuthority(role))
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);
            log.info("Completed JWTFilter for request: {}", requestURI);
        } catch (Exception ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }
}
