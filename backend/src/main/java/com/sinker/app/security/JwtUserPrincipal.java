package com.sinker.app.security;

public class JwtUserPrincipal {

    private final Long userId;
    private final String username;
    private final String roleCode;

    public JwtUserPrincipal(Long userId, String username, String roleCode) {
        this.userId = userId;
        this.username = username;
        this.roleCode = roleCode;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRoleCode() { return roleCode; }

    @Override
    public String toString() {
        return "JwtUserPrincipal{userId=" + userId + ", username='" + username + "', roleCode='" + roleCode + "'}";
    }
}
