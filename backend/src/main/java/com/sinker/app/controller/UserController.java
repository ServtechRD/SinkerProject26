package com.sinker.app.controller;

import com.sinker.app.dto.user.*;
import com.sinker.app.exception.DuplicateFieldException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.security.JwtUserPrincipal;
import com.sinker.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user.view')")
    public ResponseEntity<UserListResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {
        UserListResponse response = userService.listUsers(keyword, roleId, isActive,
                                                          page, size, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user.view')")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user.create')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request,
                                              @AuthenticationPrincipal JwtUserPrincipal principal) {
        UserDTO created = userService.createUser(request, principal.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user.edit')")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id,
                                              @Valid @RequestBody UpdateUserRequest request) {
        UserDTO updated = userService.updateUser(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user.delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('user.edit')")
    public ResponseEntity<UserDTO> toggleActive(@PathVariable Long id) {
        UserDTO updated = userService.toggleActive(id);
        return ResponseEntity.ok(updated);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(
            DuplicateFieldException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict",
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
