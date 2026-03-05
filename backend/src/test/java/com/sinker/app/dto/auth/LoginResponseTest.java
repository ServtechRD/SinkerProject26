package com.sinker.app.dto.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseTest {

    @Test
    void testDefaultConstructor() {
        LoginResponse response = new LoginResponse();
        assertNotNull(response);
        assertNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertNull(response.getUser());
    }

    @Test
    void testParameterizedConstructor() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(1L, "testuser", "test@example.com", "Test User", "admin");
        LoginResponse response = new LoginResponse("test-token-123", userInfo);

        assertEquals("test-token-123", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(userInfo, response.getUser());
    }

    @Test
    void testSettersAndGetters() {
        LoginResponse response = new LoginResponse();
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();

        response.setToken("new-token");
        response.setTokenType("Custom");
        response.setUser(userInfo);

        assertEquals("new-token", response.getToken());
        assertEquals("Custom", response.getTokenType());
        assertEquals(userInfo, response.getUser());
    }

    @Test
    void testUserInfoDefaultConstructor() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        assertNotNull(userInfo);
        assertNull(userInfo.getId());
        assertNull(userInfo.getUsername());
        assertNull(userInfo.getEmail());
        assertNull(userInfo.getFullName());
        assertNull(userInfo.getRoleCode());
    }

    @Test
    void testUserInfoParameterizedConstructor() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                10L, "john", "john@example.com", "John Doe", "sales"
        );

        assertEquals(10L, userInfo.getId());
        assertEquals("john", userInfo.getUsername());
        assertEquals("john@example.com", userInfo.getEmail());
        assertEquals("John Doe", userInfo.getFullName());
        assertEquals("sales", userInfo.getRoleCode());
    }

    @Test
    void testUserInfoSettersAndGetters() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();

        userInfo.setId(5L);
        userInfo.setUsername("alice");
        userInfo.setEmail("alice@example.com");
        userInfo.setFullName("Alice Smith");
        userInfo.setRoleCode("admin");

        assertEquals(5L, userInfo.getId());
        assertEquals("alice", userInfo.getUsername());
        assertEquals("alice@example.com", userInfo.getEmail());
        assertEquals("Alice Smith", userInfo.getFullName());
        assertEquals("admin", userInfo.getRoleCode());
    }

    @Test
    void testUserInfoWithNullValues() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(null, null, null, null, null);

        assertNull(userInfo.getId());
        assertNull(userInfo.getUsername());
        assertNull(userInfo.getEmail());
        assertNull(userInfo.getFullName());
        assertNull(userInfo.getRoleCode());
    }

    @Test
    void testCompleteLoginResponseFlow() {
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                100L, "testuser", "test@company.com", "Test User", "admin"
        );
        LoginResponse response = new LoginResponse("jwt-token-abc", userInfo);

        assertNotNull(response);
        assertEquals("jwt-token-abc", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertNotNull(response.getUser());
        assertEquals(100L, response.getUser().getId());
        assertEquals("testuser", response.getUser().getUsername());
        assertEquals("test@company.com", response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getFullName());
        assertEquals("admin", response.getUser().getRoleCode());
    }
}
