package com.sinker.app.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class IPAddressUtil {

    private IPAddressUtil() {}

    public static String extractIP(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // X-Forwarded-For may contain multiple IPs: client, proxy1, proxy2
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    public static String extractUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getHeader("User-Agent");
    }
}
