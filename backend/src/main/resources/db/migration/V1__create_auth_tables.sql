-- V1: Create authentication and authorization tables

CREATE TABLE roles (
    id          INT           NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    description TEXT          NULL,
    is_system   BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_roles_code (code),
    INDEX idx_roles_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id                   INT           NOT NULL AUTO_INCREMENT,
    username             VARCHAR(50)   NOT NULL,
    email                VARCHAR(100)  NOT NULL,
    hashed_password      VARCHAR(255)  NOT NULL,
    full_name            VARCHAR(100)  NULL,
    role_id              INT           NOT NULL,
    department           VARCHAR(50)   NULL,
    phone                VARCHAR(20)   NULL,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    is_locked            BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_login_count   INT           NOT NULL DEFAULT 0,
    last_login_at        TIMESTAMP     NULL,
    password_changed_at  TIMESTAMP     NULL,
    created_by           INT           NULL,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_users_username (username),
    UNIQUE INDEX idx_users_email (email),
    INDEX idx_users_role_id (role_id),
    INDEX idx_users_is_active (is_active),
    CONSTRAINT fk_users_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id          INT           NOT NULL AUTO_INCREMENT,
    code        VARCHAR(100)  NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    module      VARCHAR(50)   NOT NULL,
    description TEXT          NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_permissions_code (code),
    INDEX idx_permissions_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permissions (
    id            INT       NOT NULL AUTO_INCREMENT,
    role_id       INT       NOT NULL,
    permission_id INT       NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_role_permissions_pair (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role_id FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
