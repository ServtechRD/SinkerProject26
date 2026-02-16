package com.sinker.app.dto.role;

import com.sinker.app.entity.Role;

public class RoleListDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;

    public static RoleListDTO fromEntity(Role role) {
        RoleListDTO dto = new RoleListDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setIsSystem(role.getIsSystem());
        dto.setIsActive(role.getIsActive());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsSystem() { return isSystem; }
    public void setIsSystem(Boolean isSystem) { this.isSystem = isSystem; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
