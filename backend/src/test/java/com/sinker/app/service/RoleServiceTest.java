package com.sinker.app.service;

import com.sinker.app.dto.role.RoleDetailDTO;
import com.sinker.app.dto.role.UpdateRoleRequest;
import com.sinker.app.entity.Permission;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.RolePermission;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.PermissionRepository;
import com.sinker.app.repository.RolePermissionRepository;
import com.sinker.app.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, permissionRepository, rolePermissionRepository);
    }

    private Role createTestRole(Long id, String code, String name, boolean isSystem) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setDescription("Test description");
        role.setIsSystem(isSystem);
        role.setIsActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }

    private Permission createTestPermission(Long id, String code, String name, String module) {
        Permission p = new Permission();
        p.setId(id);
        p.setCode(code);
        p.setName(name);
        p.setModule(module);
        p.setIsActive(true);
        return p;
    }

    // --- getAllRoles ---

    @Test
    void getAllRolesReturnsAllRoles() {
        Role admin = createTestRole(1L, "admin", "Administrator", true);
        Role sales = createTestRole(2L, "sales", "Sales", true);
        when(roleRepository.findAllByOrderByIdAsc()).thenReturn(List.of(admin, sales));

        @SuppressWarnings("unchecked")
        Map<String, Object> result = roleService.getAllRoles();
        List<?> roles = (List<?>) result.get("roles");

        assertEquals(2, roles.size());
    }

    @Test
    void getAllRolesReturnsEmptyList() {
        when(roleRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        Map<String, Object> result = roleService.getAllRoles();
        List<?> roles = (List<?>) result.get("roles");

        assertTrue(roles.isEmpty());
    }

    // --- getRoleById ---

    @Test
    void getRoleByIdReturnsRoleWithPermissions() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");
        Permission p2 = createTestPermission(2L, "user.create", "Create Users", "user");
        Permission p3 = createTestPermission(5L, "role.view", "View Roles", "role");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1, p2, p3));

        RoleDetailDTO result = roleService.getRoleById(1L);

        assertEquals(1L, result.getId());
        assertEquals("admin", result.getCode());
        assertEquals(3, result.getPermissions().size());
    }

    @Test
    void getRoleByIdNotFoundThrows() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> roleService.getRoleById(99L));
    }

    @Test
    void getRoleByIdPermissionsGroupedByModule() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");
        Permission p2 = createTestPermission(2L, "user.create", "Create Users", "user");
        Permission p3 = createTestPermission(5L, "role.view", "View Roles", "role");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1, p2, p3));

        RoleDetailDTO result = roleService.getRoleById(1L);

        assertNotNull(result.getPermissionsByModule());
        assertEquals(2, result.getPermissionsByModule().size());
        assertEquals(2, result.getPermissionsByModule().get("user").size());
        assertEquals(1, result.getPermissionsByModule().get("role").size());
    }

    // --- updateRole ---

    @Test
    void updateRoleWithValidData() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1L))).thenReturn(List.of(p1));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated Admin");
        request.setDescription("Updated description");
        request.setPermissionIds(List.of(1L));

        RoleDetailDTO result = roleService.updateRole(1L, request);

        assertEquals("Updated Admin", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(rolePermissionRepository).save(any(RolePermission.class));
    }

    @Test
    void updateRoleNotFoundThrows() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        assertThrows(ResourceNotFoundException.class, () -> roleService.updateRole(99L, request));
    }

    @Test
    void updateRoleWithInvalidPermissionIdsThrows() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setPermissionIds(List.of(1L, 999L));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> roleService.updateRole(1L, request));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    void updateRoleWithEmptyPermissions() {
        Role role = createTestRole(1L, "admin", "Administrator", true);

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of());

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setPermissionIds(List.of());

        RoleDetailDTO result = roleService.updateRole(1L, request);

        verify(rolePermissionRepository).deleteByRoleId(1L);
        verify(rolePermissionRepository, never()).save(any(RolePermission.class));
        assertTrue(result.getPermissions().isEmpty());
    }

    @Test
    void updateRoleNameOnlyKeepsPermissions() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("New Name");

        RoleDetailDTO result = roleService.updateRole(1L, request);

        assertEquals("New Name", result.getName());
        verify(rolePermissionRepository, never()).deleteByRoleId(anyLong());
        assertEquals(1, result.getPermissions().size());
    }

    @Test
    void updateRoleWithDuplicatePermissionIdsDeduplicates() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1L))).thenReturn(List.of(p1));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setPermissionIds(List.of(1L, 1L, 1L));

        roleService.updateRole(1L, request);

        verify(rolePermissionRepository, times(1)).save(any(RolePermission.class));
    }

    @Test
    void updateRoleWithNullPermissionIdsInArrayFiltersNulls() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1L))).thenReturn(List.of(p1));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        ids.add(1L);
        ids.add(null);
        request.setPermissionIds(ids);

        roleService.updateRole(1L, request);

        verify(rolePermissionRepository, times(1)).save(any(RolePermission.class));
    }

    @Test
    void updateRoleUpdatesTimestamp() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        LocalDateTime before = role.getUpdatedAt();

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of());

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated");

        roleService.updateRole(1L, request);

        assertNotEquals(before, role.getUpdatedAt());
    }

    @Test
    void updateRoleCanUpdateSystemRoleNameAndPermissions() {
        Role role = createTestRole(1L, "admin", "Administrator", true);
        assertTrue(role.getIsSystem());

        Permission p1 = createTestPermission(1L, "user.view", "View Users", "user");

        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(1L))).thenReturn(List.of(p1));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setName("Updated System Role");
        request.setPermissionIds(List.of(1L));

        RoleDetailDTO result = roleService.updateRole(1L, request);

        assertEquals("Updated System Role", result.getName());
    }
}
