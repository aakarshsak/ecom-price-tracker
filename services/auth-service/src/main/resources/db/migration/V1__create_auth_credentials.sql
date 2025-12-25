
--AUTH_CREDENTIALS (Core Authentication)
CREATE TABLE auth_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,  -- FK to User Service
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,  -- BCrypt hash
    salt                VARCHAR(255),           -- Optional additional salt

    -- Account Security
    is_email_verified   BOOLEAN DEFAULT FALSE,
    is_phone_verified   BOOLEAN DEFAULT FALSE,
    is_2fa_enabled      BOOLEAN DEFAULT FALSE,
    totp_secret         VARCHAR(255),           -- Google Authenticator

    -- Account Protection
    failed_attempts     INT DEFAULT 0,
    locked_until        TIMESTAMP,
    last_login          TIMESTAMP,
    last_password_change TIMESTAMP DEFAULT NOW(),

    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    created_by          UUID
);

CREATE INDEX idx_auth_email ON auth_credentials(email);
CREATE INDEX idx_auth_user_id ON auth_credentials(user_id);


--ROLES (Authorization Roles)
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL UNIQUE,  -- ROLE_ADMIN, ROLE_TRADER, ROLE_USER
    description     VARCHAR(255),
    permissions     JSONB,  -- {"canTrade": true, "canWithdraw": true}
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_role_name ON roles(name);

--USER_ROLES (Many-to-Many)
CREATE TABLE user_roles (
    user_id         UUID NOT NULL,
    role_id         UUID NOT NULL,
    granted_at      TIMESTAMP DEFAULT NOW(),
    granted_by      UUID,
    expires_at      TIMESTAMP,  -- Optional: Temporary roles
    is_active       BOOLEAN DEFAULT TRUE,

    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);