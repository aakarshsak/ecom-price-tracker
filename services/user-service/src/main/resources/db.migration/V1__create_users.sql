--USERS (Core User Profile)

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Basic Info
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,  -- Duplicate from auth
    mobile_number       VARCHAR(20),

    -- Personal Info
    dob                 DATE,
    gender              VARCHAR(20),
    nationality         VARCHAR(50),

    -- Status
    user_status         VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, SUSPENDED, DELETED
    account_type        VARCHAR(50) DEFAULT 'RETAIL',  -- RETAIL, INSTITUTIONAL, VIP

    -- KYC Status
    kyc_status          VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, VERIFIED, REJECTED, EXPIRED
    kyc_verified_at     TIMESTAMP,
    kyc_verified_by     UUID,

    -- Trading Specific
    trading_status      VARCHAR(20) DEFAULT 'RESTRICTED',  -- RESTRICTED, ENABLED, SUSPENDED
    risk_profile        VARCHAR(20),  -- CONSERVATIVE, MODERATE, AGGRESSIVE

    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    last_active_at      TIMESTAMP,
    deleted_at          TIMESTAMP  -- Soft delete
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(user_status);
CREATE INDEX idx_users_kyc ON users(kyc_status);
CREATE INDEX idx_users_trading ON users(trading_status);
CREATE INDEX idx_users_created ON users(created_at DESC);



--USER_PROFILES (Extended Profile)

CREATE TABLE user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,

    -- Professional Info
    occupation          VARCHAR(100),
    annual_income       VARCHAR(50),
    source_of_funds     VARCHAR(100),
    employer_name       VARCHAR(200),

    -- Additional Info
    profile_picture_url TEXT,
    bio                 TEXT,
    referral_code       VARCHAR(20) UNIQUE,
    referred_by         UUID,

    -- Communication Preferences
    language_preference VARCHAR(10) DEFAULT 'en',
    timezone            VARCHAR(50) DEFAULT 'UTC',

    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_profile_referral ON user_profiles(referral_code);


--USER ADDRESSES

CREATE TABLE user_addresses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    address_type        VARCHAR(20) NOT NULL,  -- PERMANENT, CURRENT, BILLING

    -- Address Details
    address_line1       VARCHAR(255) NOT NULL,
    address_line2       VARCHAR(255),
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(50) NOT NULL,

    -- Verification
    is_verified         BOOLEAN DEFAULT FALSE,
    verified_at         TIMESTAMP,

    -- Metadata
    is_primary          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_address_user ON user_addresses(user_id);
CREATE INDEX idx_address_type ON user_addresses(address_type);
