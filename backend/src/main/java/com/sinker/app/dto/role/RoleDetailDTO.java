package com.sinker.app.dto.role;

import com.sinker.app.entity.Role;

import java.util.List;
import java.util.Map;

public class RoleDetailDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private List<PermissionDTO> permissions;
    private Map<String, List<PermissionDTO>> permissionsByModule;

    public static RoleDetailDTO fromEntity(Role role, List<PermissionDTO> permissions,
                                           Map<String, List<PermissionDTO>> permissionsByModule) {
        RoleDetailDTO dto = new RoleDetailDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setIsSystem(role.getIsSystem());
        dto.setIsActive(role.getIsActive());
        dto.setPermissions(permissions);
        dto.setPermissionsByModule(permissionsByModule);
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

    public List<PermissionDTO> getPermissions() { return permissions; }
    public void setPermissions(List<PermissionDTO> permissions) { this.permissions = permissions; }

    public Map<String, List<PermissionDTO>> getPermissionsByModule() { return permissionsByModule; }
    public void setPermissionsByModule(Map<String, List<PermissionDTO>> permissionsByModule) { this.permissionsByModule = permissionsByModule; }
}
