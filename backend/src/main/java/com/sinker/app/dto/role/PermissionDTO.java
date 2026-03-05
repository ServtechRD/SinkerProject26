package com.sinker.app.dto.role;

import com.sinker.app.entity.Permission;

public class PermissionDTO {

    private Long id;
    private String code;
    private String name;
    private String module;

    public static PermissionDTO fromEntity(Permission p) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(p.getId());
        dto.setCode(p.getCode());
        dto.setName(p.getName());
        dto.setModule(p.getModule());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
}
