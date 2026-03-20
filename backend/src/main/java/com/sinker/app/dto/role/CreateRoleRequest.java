package com.sinker.app.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateRoleRequest {

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    // 可選：建立角色時同步綁定權限
    private List<Long> permissionIds;

    public String getCode() { return code; }

    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public List<Long> getPermissionIds() { return permissionIds; }

    public void setPermissionIds(List<Long> permissionIds) { this.permissionIds = permissionIds; }
}

