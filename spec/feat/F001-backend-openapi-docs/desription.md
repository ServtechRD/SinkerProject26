# F002 Backend OpenAPI Documentation (Swagger UI) + Auto Update Mechanism

## Goal
Enable backend OpenAPI documentation using Swagger UI for the Spring Boot backend,
and introduce a repeatable export & verification mechanism so API documentation
can be periodically updated and validated in CI.

This task ensures:
- Developers can access live API documentation.
- OpenAPI JSON can be exported into the repository.
- Documentation stays in sync with code.

---

## Tech Stack
- Spring Boot 3.2.x
- JDK 17
- Gradle
- Spring Security 6
- springdoc-openapi

---

## Scope

### Included
- Add OpenAPI + Swagger UI dependency.
- Configure endpoints under `/api`.
- Provide export script to generate OpenAPI JSON snapshot.
- Provide check script for CI validation.
- Security whitelist for Swagger endpoints.
- Metadata configuration (title, version, description).

### Excluded
- Frontend integration.
- Authentication redesign.
- Database schema changes.
- Major refactors.

---

## Required Endpoints

| Purpose | Endpoint |
|--------|---------|
| OpenAPI JSON | `/api/v3/api-docs` |
| Swagger UI | `/api/swagger-ui/index.html` |

---

## Implementation Hints (Must Follow)

### 1. Dependency (Gradle Groovy DSL)

```gradle
dependencies {
  implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0'
}
```

## 2 add prefx `/api`

- `/api/v3/api-docs`
- `/api/swagger-ui/index.html`

## 3 prod  open swagger


## 4 Security Spring
**only pass swagger require endpoints**

### pass list
- `/api/v3/api-docs/**`
- `/api/swagger-ui/**`
- `/api/swagger-ui.html`
- `/swagger-ui/**`、`/v3/api-docs/**`

### Spring Security example(java)
`SecurityConfig`  `SecurityFilterChain`：

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(
          "/api/v3/api-docs/**",
          "/api/swagger-ui/**",
          "/api/swagger-ui.html"
        ).permitAll()
        .anyRequest().authenticated()
      );

    return http.build();
}
