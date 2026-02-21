package com.sinker.app.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class IPAddressUtilTest {

    @Test
    void extractIPFromXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.1, 192.0.2.1");

        assertEquals("203.0.113.1", IPAddressUtil.extractIP(request));
    }

    @Test
    void extractIPFromXRealIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "10.0.0.50");

        assertEquals("10.0.0.50", IPAddressUtil.extractIP(request));
    }

    @Test
    void extractIPFromRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        assertEquals("192.168.1.100", IPAddressUtil.extractIP(request));
    }

    @Test
    void xForwardedForTakesPrecedenceOverXRealIP() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1");
        request.addHeader("X-Real-IP", "10.0.0.50");

        assertEquals("203.0.113.1", IPAddressUtil.extractIP(request));
    }

    @Test
    void extractIPHandlesIPv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");

        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", IPAddressUtil.extractIP(request));
    }

    @Test
    void extractIPHandlesCompressedIPv6() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("2001:db8::1");

        assertEquals("2001:db8::1", IPAddressUtil.extractIP(request));
    }

    @Test
    void extractIPFromNullRequest() {
        assertNull(IPAddressUtil.extractIP(null));
    }

    @Test
    void extractUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", IPAddressUtil.extractUserAgent(request));
    }

    @Test
    void extractUserAgentMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertNull(IPAddressUtil.extractUserAgent(request));
    }

    @Test
    void extractUserAgentFromNullRequest() {
        assertNull(IPAddressUtil.extractUserAgent(null));
    }

    @Test
    void extractIPTrimsWhitespace() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "  203.0.113.1  ");

        assertEquals("203.0.113.1", IPAddressUtil.extractIP(request));
    }
}
