package com.mjsec.lms.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jwts.SIG;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtService {

    private SecretKey secretKey;

    @Autowired
    public JwtService(@Value("${spring.jwt.secret}") String secret){

        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIG.HS256.key().build().getAlgorithm());
    }

    public String getRole(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Long getStudentNumber(String token){

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("studentNumber", Long.class);
    }

    public Boolean isExpired(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String getTokenType(String token) {

        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("tokenType", String.class);
    }

    public Date getExpirationDate(String token) {

        Date expDate = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        log.info("Extracted Expiration Date from JWT: {}", expDate);

        return expDate;
    }

    /**
     * JWT 생성
     *
     * @param tokenType : 토큰 타입(ACCESS 토큰 / REFRESH 토큰) -> 현재는 ACCESS 토큰만 사용
     * @param studentNumber : 유저의 학번
     * @param role : 유저의 권한
     * @param expiredMs : 만료 시점
     * @return JWT (String)
     */
    public String createJwt(String tokenType, Long studentNumber, String role, Long expiredMs) {

        return Jwts.builder()
                .claim("tokenType", tokenType)
                .claim("studentNumber", studentNumber)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }
}
