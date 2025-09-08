package com.mjsec.lms.service;

import com.mjsec.lms.exception.RestApiException;
import com.mjsec.lms.type.ErrorCode;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthCodeService {

    private final StringRedisTemplate stringRedisTemplate;

    public AuthCodeService(StringRedisTemplate stringRedisTemplate) {

        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 인증 코드 생성 후 레디스에 저장하는 메소드
     * @param email 수신자
     * @return code 인증 코드
     */
    public String generateAndStoreCode(String email) {

        String code = generateNumericCode(6);

        stringRedisTemplate.opsForValue().set("authCode:" + email, code, 5, TimeUnit.MINUTES);
        log.info("인증 코드 생성 및 저장 : {} -> {}", email, code);

        return code;
    }

    /**
     * 사용자가 입력한 인증 코드가 맞는지 검증하는 메소드
     * @param email 수신자
     * @param code 인증 코드
     * @return true / false
     */
    public boolean verifyCode(String email, String code) {

        String key = "authCode:" + email;
        String storedCode = stringRedisTemplate.opsForValue().get(key);
        String attemptKey = "attempts:" + email;

        Integer attempts= Optional.ofNullable(stringRedisTemplate.opsForValue().get(attemptKey))
                .map(Integer::valueOf)
                .orElse(0);

        if(attempts >= 5) {
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(attemptKey);
            log.warn("인증 시도 횟수 초과: {}", email);
            throw new RestApiException(ErrorCode.AUTH_ATTEMPT_EXCEEDED);
        }

        if(storedCode != null && storedCode.equals(code)) {
            stringRedisTemplate.opsForValue().set("verified:" + email, "true", 30, TimeUnit.MINUTES);
            stringRedisTemplate.delete(key);
            stringRedisTemplate.delete(attemptKey);
            log.info("이메일 인증 성공: {}", email);
            return true;
        }
        else {
            stringRedisTemplate.opsForValue().increment(attemptKey);
            stringRedisTemplate.expire(attemptKey, 5, TimeUnit.MINUTES);
            log.warn("이메일 인증 실패: {} (시도 횟수: {})", email, attempts + 1);
            return false;
        }
    }

    /**
     * 이메일 인증이 완료되었는지 검증하는 메소드
     * @param email 수신자
     * @return true / false
     */
    public boolean isEmailVerified(String email) {

        String verified = stringRedisTemplate.opsForValue().get("verified:" + email);
        return "true".equals(verified);
    }

    /**
     * 랜덤으로 6자리 정수 코드를 만드는 메소드
     * @param length 코드의 길이
     * @return code 인증 코드
     */
    private String generateNumericCode(int length) {

        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < length; i++){
            code.append(random.nextInt(10));
        }

        return code.toString();
    }
}
