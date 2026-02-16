package com.sinker.app.service;

import com.sinker.app.dto.role.*;
import com.sinker.app.entity.Permission;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.RolePermission;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.PermissionRepository;
import com.sinker.app.repository.RolePermissionRepository;
import com.sinker.app.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllRoles() {
        List<RoleListDTO> roles = roleRepository.findAllByOrderByIdAsc().stream()
                .map(RoleListDTO::fromEntity)
                .toList();
        return Map.of("roles", roles);
    }

    @Transactional(readOnly = true)
    public RoleDetailDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        List<Permission> permissions = permissionRepository.findByRoleId(id);
        return buildRoleDetail(role, permissions);
    }

    @Transactional
    public RoleDetailDTO updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        if (StringUtils.hasText(request.getName())) {
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        if (request.getPermissionIds() != null) {
            List<Long> requestedIds = request.getPermissionIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (!requestedIds.isEmpty()) {
                List<Permission> found = permissionRepository.findAllById(requestedIds);
                Set<Long> foundIds = found.stream().map(Permission::getId).collect(Collectors.toSet());
                List<Long> invalidIds = requestedIds.stream()
                        .filter(pid -> !foundIds.contains(pid))
                        .toList();
                if (!invalidIds.isEmpty()) {
                    throw new IllegalArgumentException("Invalid permission IDs: " + invalidIds);
                }
            }

            rolePermissionRepository.deleteByRoleId(id);
            for (Long permId : requestedIds) {
                rolePermissionRepository.save(new RolePermission(id, permId));
            }
        }

        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        List<Permission> permissions = permissionRepository.findByRoleId(id);
        return buildRoleDetail(role, permissions);
    }

    private RoleDetailDTO buildRoleDetail(Role role, List<Permission> permissions) {
        List<PermissionDTO> permDtos = permissions.stream()
                .map(PermissionDTO::fromEntity)
                .toList();

        Map<String, List<PermissionDTO>> byModule = new LinkedHashMap<>();
        for (PermissionDTO dto : permDtos) {
            byModule.computeIfAbsent(dto.getModule(), k -> new ArrayList<>()).add(dto);
        }

        return RoleDetailDTO.fromEntity(role, permDtos, byModule);
    }
}
