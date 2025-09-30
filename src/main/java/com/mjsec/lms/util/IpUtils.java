package com.mjsec.lms.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

//IP 주소 추출을 위한 유틸리티 클래스
@Component
public class IpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private static final String[] UNKNOWN_IPS = {
            "unknown",
            "0:0:0:0:0:0:0:1",
            "127.0.0.1"
    };

    //HttpServletRequest에서 클라이언트의 실제 IP 주소를 추출합니다.
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 각 헤더를 순차적으로 확인
        for (String header : IP_HEADERS) {
            String clientIp = request.getHeader(header);
            if (isValidIp(clientIp)) {
                // X-Forwarded-For의 경우 여러 IP가 콤마로 구분될 수 있음
                if ("X-Forwarded-For".equals(header) && clientIp.contains(",")) {
                    return clientIp.split(",")[0].trim();
                }
                return clientIp;
            }
        }

        // 모든 헤더에서 찾지 못한 경우 기본 방법 사용
        return request.getRemoteAddr();
    }

    //IP 주소가 유효한지 확인
    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !isUnknownIp(ip);
    }

    //알 수 없는 IP인지 확인
    private static boolean isUnknownIp(String ip) {
        if (ip == null) {
            return true;
        }

        for (String unknownIp : UNKNOWN_IPS) {
            if (unknownIp.equalsIgnoreCase(ip)) {
                return true;
            }
        }
        return false;
    }

    //IP 주소가 로컬인지 확인
    public static boolean isLocalIp(String ip) {
        return "127.0.0.1".equals(ip) ||
                "0:0:0:0:0:0:0:1".equals(ip) ||
                "localhost".equalsIgnoreCase(ip);
    }

    // 유틸리티 클래스이므로 인스턴스화 방지
    private IpUtils() {
        throw new IllegalStateException("Utility class");
    }
}