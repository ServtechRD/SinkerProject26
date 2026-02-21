package com.sinker.app.dto.auth;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private UserInfo user;

    public LoginResponse() {}

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String roleCode;

        public UserInfo() {}

        public UserInfo(Long id, String username, String email, String fullName, String roleCode) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.fullName = fullName;
            this.roleCode = roleCode;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    }
}
