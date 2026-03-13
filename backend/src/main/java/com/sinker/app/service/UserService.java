package com.sinker.app.service;

import com.sinker.app.dto.user.*;
import com.sinker.app.entity.Role;
import com.sinker.app.entity.User;
import com.sinker.app.exception.DuplicateFieldException;
import com.sinker.app.exception.ResourceNotFoundException;
import com.sinker.app.repository.RoleRepository;
import com.sinker.app.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final String SALES_ROLE_CODE = "sales";

    private static final Set<String> ALLOWED_CHANNELS = Set.of(
            "PX + 大全聯", "家樂福", "愛買", "7-11", "全家", "Ok+萊爾富",
            "好市多", "楓康", "美聯社", "康是美", "電商", "市面經銷"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public UserListResponse listUsers(String keyword, Long roleId, Boolean isActive,
                                      int page, int size, String sortBy, String sortOrder) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = buildSpecification(keyword, roleId, isActive);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserDTO> users = userPage.getContent().stream()
                .map(UserDTO::fromEntity)
                .toList();

        return new UserListResponse(users, userPage.getTotalElements(),
                                    userPage.getTotalPages(), userPage.getNumber());
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        UserDTO dto = UserDTO.fromEntity(user);
        List<String> channels = jdbcTemplate.query(
                "SELECT channel FROM sales_channels_users WHERE user_id = ? ORDER BY channel",
                (rs, rowNum) -> rs.getString("channel"), id);
        dto.setChannels(channels);
        return dto;
    }

    @Transactional
    public UserDTO createUser(CreateUserRequest request, Long createdBy) {
        String username = request.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateFieldException("username", "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateFieldException("email", "Email already exists");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));

        validateSalesChannels(role.getCode(), request.getChannels());

        User user = new User();
        user.setUsername(username);
        user.setEmail(request.getEmail());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setDepartment(request.getDepartment());
        user.setPhone(request.getPhone());
        user.setIsActive(true);
        user.setIsLocked(false);
        user.setFailedLoginCount(0);
        user.setCreatedBy(createdBy);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        if (SALES_ROLE_CODE.equals(role.getCode()) && request.getChannels() != null && !request.getChannels().isEmpty()) {
            persistChannels(saved.getId(), request.getChannels());
        }
        return toDtoWithChannels(saved);
    }

    @Transactional
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (StringUtils.hasText(request.getUsername())) {
            String newUsername = request.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsernameAndIdNot(newUsername, id)) {
                    throw new DuplicateFieldException("username", "Username already exists");
                }
                user.setUsername(newUsername);
            }
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new DuplicateFieldException("email", "Email already exists");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
            user.setPasswordChangedAt(LocalDateTime.now());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getRoleId() != null) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));
            validateSalesChannels(role.getCode(), request.getChannels());
            user.setRole(role);
        }

        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);

        // Sync sales_channels_users: persist when sales with channels, clear when not sales
        if (SALES_ROLE_CODE.equals(saved.getRole().getCode()) && request.getChannels() != null && !request.getChannels().isEmpty()) {
            persistChannels(saved.getId(), request.getChannels());
        } else if (!SALES_ROLE_CODE.equals(saved.getRole().getCode())) {
            jdbcTemplate.update("DELETE FROM sales_channels_users WHERE user_id = ?", saved.getId());
        }

        return toDtoWithChannels(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDTO toggleActive(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        return UserDTO.fromEntity(saved);
    }

    private void persistChannels(Long userId, List<String> channels) {
        jdbcTemplate.update("DELETE FROM sales_channels_users WHERE user_id = ?", userId);
        for (String ch : channels) {
            if (ALLOWED_CHANNELS.contains(ch)) {
                jdbcTemplate.update("INSERT INTO sales_channels_users (user_id, channel) VALUES (?, ?)", userId, ch);
            }
        }
    }

    private UserDTO toDtoWithChannels(User user) {
        UserDTO dto = UserDTO.fromEntity(user);
        List<String> channels = jdbcTemplate.query(
                "SELECT channel FROM sales_channels_users WHERE user_id = ? ORDER BY channel",
                (rs, rowNum) -> rs.getString("channel"), user.getId());
        dto.setChannels(channels);
        return dto;
    }

    private void validateSalesChannels(String roleCode, List<String> channels) {
        if (SALES_ROLE_CODE.equals(roleCode)) {
            if (channels == null || channels.isEmpty()) {
                throw new IllegalArgumentException("Sales role requires at least one channel");
            }
            for (String ch : channels) {
                if (!ALLOWED_CHANNELS.contains(ch)) {
                    throw new IllegalArgumentException("Invalid channel: " + ch);
                }
            }
        }
    }

    private Specification<User> buildSpecification(String keyword, Long roleId, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                Predicate usernameLike = cb.like(cb.lower(root.get("username")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                Predicate fullNameLike = cb.like(cb.lower(root.get("fullName")), pattern);
                predicates.add(cb.or(usernameLike, emailLike, fullNameLike));
            }

            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("id"), roleId));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
