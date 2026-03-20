package com.sinker.app.service;

import com.sinker.app.dto.role.*;
import com.sinker.app.entity.Permission;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.RolePermission;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.PermissionRepository;
import com.sinker.app.repository.RolePermissionRepository;
import com.sinker.app.repository.RoleRepository;
import com.sinker.app.repository.UserRepository;
import org.springframework.data.domain.Sort;
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
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       RolePermissionRepository rolePermissionRepository,
                       UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
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

        List<Permission> rolePermissions = permissionRepository.findByRoleId(id);
        List<Permission> allPermissions = permissionRepository.findAll(Sort.by("module", "code"));
        return buildRoleDetail(role, rolePermissions, allPermissions);
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
            rolePermissionRepository.flush();
            for (Long permId : requestedIds) {
                rolePermissionRepository.save(new RolePermission(id, permId));
            }
        }

        role.setUpdatedAt(LocalDateTime.now());
        roleRepository.save(role);

        List<Permission> rolePermissions = permissionRepository.findByRoleId(id);
        List<Permission> allPermissions = permissionRepository.findAll(Sort.by("module", "code"));
        return buildRoleDetail(role, rolePermissions, allPermissions);
    }

    private RoleDetailDTO buildRoleDetail(Role role, List<Permission> rolePermissions, List<Permission> allPermissions) {
        List<PermissionDTO> rolePermDtos = rolePermissions.stream()
                .map(PermissionDTO::fromEntity)
                .toList();

        List<PermissionDTO> allPermDtos = allPermissions.stream()
                .map(PermissionDTO::fromEntity)
                .toList();

        Map<String, List<PermissionDTO>> byModule = new LinkedHashMap<>();
        for (PermissionDTO dto : allPermDtos) {
            byModule.computeIfAbsent(dto.getModule(), k -> new ArrayList<>()).add(dto);
        }

        return RoleDetailDTO.fromEntity(role, rolePermDtos, byModule);
    }

    @Transactional(readOnly = true)
    public Map<String, List<PermissionDTO>> getAllPermissionsGroupedByModule() {
        List<Permission> allPerms = permissionRepository.findAll(Sort.by("module", "code"));
        List<PermissionDTO> allPermDtos = allPerms.stream()
                .map(PermissionDTO::fromEntity)
                .toList();

        Map<String, List<PermissionDTO>> byModule = new LinkedHashMap<>();
        for (PermissionDTO dto : allPermDtos) {
            byModule.computeIfAbsent(dto.getModule(), k -> new ArrayList<>()).add(dto);
        }
        return byModule;
    }

    @Transactional
    public RoleDetailDTO createRole(CreateRoleRequest request) {
        String code = request.getCode() != null ? request.getCode().trim() : null;
        String name = request.getName() != null ? request.getName().trim() : null;
        String description = request.getDescription();

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("code is required");
        }
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (roleRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Role code already exists");
        }

        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setIsSystem(false);
        role.setIsActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        Role savedRole = roleRepository.save(role);

        List<Long> requestedIds = request.getPermissionIds() == null ? List.of() : request.getPermissionIds();
        requestedIds = requestedIds.stream().filter(Objects::nonNull).distinct().toList();

        if (!requestedIds.isEmpty()) {
            List<Permission> found = permissionRepository.findAllById(requestedIds);
            Set<Long> foundIds = found.stream().map(Permission::getId).collect(Collectors.toSet());
            List<Long> invalidIds = requestedIds.stream()
                    .filter(pid -> !foundIds.contains(pid))
                    .toList();
            if (!invalidIds.isEmpty()) {
                throw new IllegalArgumentException("Invalid permission IDs: " + invalidIds);
            }

            for (Long permId : requestedIds) {
                rolePermissionRepository.save(new RolePermission(savedRole.getId(), permId));
            }
        }

        List<Permission> rolePermissions = permissionRepository.findByRoleId(savedRole.getId());
        List<Permission> allPermissions = permissionRepository.findAll(Sort.by("module", "code"));
        return buildRoleDetail(savedRole, rolePermissions, allPermissions);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        long userCount = userRepository.countByRole_Id(role.getId());
        if (userCount > 0) {
            throw new IllegalArgumentException("已有帳號使用角色，不能刪除");
        }

        roleRepository.deleteById(role.getId());
    }
}
