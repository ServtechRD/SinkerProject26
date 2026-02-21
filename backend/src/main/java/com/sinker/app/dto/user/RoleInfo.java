package com.sinker.app.dto.user;

import com.sinker.app.entity.Role;

public class RoleInfo {

    private Long id;
    private String code;
    private String name;

    public RoleInfo() {}

    public RoleInfo(Long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public static RoleInfo fromEntity(Role role) {
        return new RoleInfo(role.getId(), role.getCode(), role.getName());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
