package com.sinker.app.controller;

import com.sinker.app.dto.role.RoleDetailDTO;
import com.sinker.app.dto.role.CreateRoleRequest;
import com.sinker.app.dto.role.UpdateRoleRequest;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role.view')")
    public ResponseEntity<Map<String, Object>> listRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('role.view')")
    public ResponseEntity<Map<String, Object>> listAllPermissionsGroupedByModule() {
        Map<String, Object> resp = Map.of(
                "permissionsByModule", roleService.getAllPermissionsGroupedByModule(),
                "permissions", java.util.List.of()
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role.view')")
    public ResponseEntity<RoleDetailDTO> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role.edit')")
    public ResponseEntity<RoleDetailDTO> updateRole(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role.create')")
    public ResponseEntity<RoleDetailDTO> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleDetailDTO created = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role.delete')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                ex.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message, String path) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", error,
                "message", message,
                "path", path
        ));
    }
}
