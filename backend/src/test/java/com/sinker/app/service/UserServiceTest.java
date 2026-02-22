package com.sinker.app.service;

import com.sinker.app.dto.user.*;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.User;
import com.sinker.app.exception.DuplicateFieldException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.RoleRepository;
import com.sinker.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

    private Role createTestRole(Long id, String code, String name) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setIsActive(true);
        return role;
    }

    private User createTestUser(Long id, String username, Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@sinker.local");
        user.setHashedPassword(passwordEncoder.encode("password123"));
        user.setFullName("Test User");
        user.setRole(role);
        user.setDepartment("IT");
        user.setPhone("1234567890");
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setFailedLoginCount(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    // --- createUser tests ---

    @Test
    void createUserWithValidData() {
        Role role = createTestRole(1L, "admin", "Administrator");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@sinker.local")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@sinker.local");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setRoleId(1L);
        request.setDepartment("Engineering");

        UserDTO result = userService.createUser(request, 1L);

        assertEquals(10L, result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("new@sinker.local", result.getEmail());
        assertNotNull(result.getCreatedAt());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(passwordEncoder.matches("password123", saved.getHashedPassword()));
        assertEquals(1L, saved.getCreatedBy());
    }

    @Test
    void createUserWithDuplicateUsername() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("existing");
        request.setEmail("new@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class,
                () -> userService.createUser(request, 1L));
        assertEquals("username", ex.getField());
    }

    @Test
    void createUserWithDuplicateEmail() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@sinker.local")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("existing@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class,
                () -> userService.createUser(request, 1L));
        assertEquals("email", ex.getField());
    }

    @Test
    void createUserWithInvalidRole() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@sinker.local")).thenReturn(false);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@sinker.local");
        request.setPassword("password123");
        request.setRoleId(99L);

        assertThrows(ResourceNotFoundException.class, () -> userService.createUser(request, 1L));
    }

    @Test
    void createUserSalesRoleWithoutChannelsThrows() {
        Role salesRole = createTestRole(2L, "sales", "Sales");
        when(userRepository.existsByUsername("salesuser")).thenReturn(false);
        when(userRepository.existsByEmail("sales@sinker.local")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(salesRole));

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("salesuser");
        request.setEmail("sales@sinker.local");
        request.setPassword("password123");
        request.setRoleId(2L);
        // no channels

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(request, 1L));
        assertTrue(ex.getMessage().contains("channel"));
    }

    @Test
    void createUserSalesRoleWithValidChannels() {
        Role salesRole = createTestRole(2L, "sales", "Sales");
        when(userRepository.existsByUsername("salesuser")).thenReturn(false);
        when(userRepository.existsByEmail("sales@sinker.local")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(salesRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("salesuser");
        request.setEmail("sales@sinker.local");
        request.setPassword("password123");
        request.setRoleId(2L);
        request.setChannels(List.of("PX/大全聯", "家樂福"));

        UserDTO result = userService.createUser(request, 1L);
        assertEquals(11L, result.getId());
    }

    @Test
    void createUserSalesRoleWithInvalidChannelThrows() {
        Role salesRole = createTestRole(2L, "sales", "Sales");
        when(userRepository.existsByUsername("salesuser")).thenReturn(false);
        when(userRepository.existsByEmail("sales@sinker.local")).thenReturn(false);
        when(roleRepository.findById(2L)).thenReturn(Optional.of(salesRole));

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("salesuser");
        request.setEmail("sales@sinker.local");
        request.setPassword("password123");
        request.setRoleId(2L);
        request.setChannels(List.of("InvalidChannel"));

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, 1L));
    }

    @Test
    void createUserPasswordIsHashed() {
        Role role = createTestRole(1L, "admin", "Administrator");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@sinker.local")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("new@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        userService.createUser(request, 1L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getHashedPassword().startsWith("$2a$"));
    }

    @Test
    void createUserTrimsUsername() {
        Role role = createTestRole(1L, "admin", "Administrator");
        when(userRepository.existsByUsername("trimmed")).thenReturn(false);
        when(userRepository.existsByEmail("trim@sinker.local")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("  trimmed  ");
        request.setEmail("trim@sinker.local");
        request.setPassword("password123");
        request.setRoleId(1L);

        UserDTO result = userService.createUser(request, 1L);
        assertEquals("trimmed", result.getUsername());
    }

    // --- updateUser tests ---

    @Test
    void updateUserWithValidData() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("newemail@sinker.local", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("newemail@sinker.local");
        request.setFullName("Updated Name");
        request.setDepartment("New Dept");

        UserDTO result = userService.updateUser(1L, request);

        assertEquals("newemail@sinker.local", result.getEmail());
        assertEquals("Updated Name", result.getFullName());
    }

    @Test
    void updateUserWithPassword() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        assertNull(user.getPasswordChangedAt());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("newpassword123");

        userService.updateUser(1L, request);

        assertTrue(passwordEncoder.matches("newpassword123", user.getHashedPassword()));
        assertNotNull(user.getPasswordChangedAt());
    }

    @Test
    void updateUserWithoutPasswordKeepsOldPassword() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        String originalHash = user.getHashedPassword();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated");

        userService.updateUser(1L, request);

        assertEquals(originalHash, user.getHashedPassword());
    }

    @Test
    void updateUserWithDuplicateUsername() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("taken", 1L)).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("taken");

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class,
                () -> userService.updateUser(1L, request));
        assertEquals("username", ex.getField());
    }

    @Test
    void updateUserWithDuplicateEmail() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("taken@sinker.local", 1L)).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("taken@sinker.local");

        DuplicateFieldException ex = assertThrows(DuplicateFieldException.class,
                () -> userService.updateUser(1L, request));
        assertEquals("email", ex.getField());
    }

    @Test
    void updateUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated");

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(99L, request));
    }

    // --- deleteUser tests ---

    @Test
    void deleteUserSuccess() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
    }

    // --- toggleActive tests ---

    @Test
    void toggleUserStatusDeactivates() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        user.setIsActive(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.toggleActive(1L);
        assertFalse(result.getIsActive());
    }

    @Test
    void toggleUserStatusActivates() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "testuser", role);
        user.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO result = userService.toggleActive(1L);
        assertTrue(result.getIsActive());
    }

    @Test
    void toggleUserStatusNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.toggleActive(99L));
    }

    // --- getUserById tests ---

    @Test
    void getUserByIdReturnsDto() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user = createTestUser(1L, "admin", role);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDTO dto = userService.getUserById(1L);
        assertEquals(1L, dto.getId());
        assertEquals("admin", dto.getUsername());
        assertEquals("admin", dto.getRole().getCode());
    }

    @Test
    void getUserByIdNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // --- listUsers tests ---

    @Test
    void listUsersWithPagination() {
        Role role = createTestRole(1L, "admin", "Administrator");
        User user1 = createTestUser(1L, "user1", role);
        User user2 = createTestUser(2L, "user2", role);
        Page<User> page = new PageImpl<>(List.of(user1, user2));

        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        UserListResponse response = userService.listUsers(null, null, null, 0, 20, "id", "asc");

        assertEquals(2, response.getUsers().size());
        assertEquals(2, response.getTotalElements());
        assertEquals("user1", response.getUsers().get(0).getUsername());
    }

    @Test
    void listUsersReturnsEmptyList() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        UserListResponse response = userService.listUsers(null, null, null, 0, 20, "id", "asc");

        assertTrue(response.getUsers().isEmpty());
        assertEquals(0, response.getTotalElements());
    }
}
