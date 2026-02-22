# T002: Backend Authentication with JWT

## Context
This task implements the authentication layer for the Spring Boot 3.2.12 backend. It provides JWT-based authentication with token generation on successful login and request filtering for protected endpoints. The system uses Spring Security 6.x with bcrypt password hashing.

## Goal
Implement a complete JWT authentication flow including login endpoint, token generation, token validation filter, and Spring Security configuration to protect API endpoints while whitelisting public routes.

## Scope

### In Scope
- POST /api/auth/login endpoint accepting username/password
- JWT token generation with 24-hour expiration
- JWT authentication filter for validating tokens on protected routes
- Spring Security configuration
- User entity and repository
- Login request/response DTOs
- Business logic: user validation, password verification, account locking check, last login update
- Bcrypt password verification
- CORS configuration for React frontend

### Out of Scope
- User registration endpoint
- Password reset functionality
- Refresh token mechanism
- Login attempt logging (covered in T010)
- Account lockout after failed attempts (covered in T010)
- Sales channel assignment (covered in T007)

## Requirements
- Accept POST /api/auth/login with JSON body: {username, password}
- Validate username exists in database
- Check user is_active flag (reject if false)
- Check is_locked flag (reject if true)
- Verify password using bcrypt
- Generate JWT token with 24-hour expiration containing: user_id, username, role_code
- Update users.last_login_at timestamp on successful login
- Return JWT token and user info (id, username, email, full_name, role_code)
- Implement JWT filter to extract and validate token from Authorization header
- Set Spring Security context with authenticated user
- Whitelist /api/auth/login, /swagger-ui/**, /v3/api-docs/**
- Return appropriate HTTP status codes: 200 (success), 401 (invalid credentials), 403 (locked account)
- CORS allow localhost:3000 for development

## Implementation Notes
- Use io.jsonwebtoken (jjwt) library for JWT generation/parsing
- Store JWT secret in application.yml (use environment variable in production)
- JWT claims: sub=username, userId, roleCode, iat, exp
- Use Spring Security's OncePerRequestFilter for JWT filter
- Configure SecurityFilterChain bean with HttpSecurity DSL
- Disable CSRF (stateless API)
- Use BCryptPasswordEncoder bean for password verification
- Create custom UserDetailsService implementation
- Handle exceptions: UsernameNotFoundException, BadCredentialsException, LockedException
- Return standardized error response format

## Files to Change
- config/SecurityConfig.java (new)
- security/JwtTokenProvider.java (new)
- security/JwtAuthenticationFilter.java (new)
- controller/AuthController.java (new)
- service/AuthService.java (new)
- dto/auth/LoginRequest.java (new)
- dto/auth/LoginResponse.java (new)
- entity/User.java (new)
- entity/Role.java (new)
- repository/UserRepository.java (new)
- repository/RoleRepository.java (new)
- exception/AccountLockedException.java (new)
- pom.xml (add jjwt dependency if not present)

## Dependencies
- T001: Requires users and roles tables to exist
