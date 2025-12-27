package com.sinha.ecom_system.auth_service.model;

public enum UserStatus {
    ACTIVE,      // User account is active and operational
    INACTIVE,    // User account is inactive (not deleted)
    SUSPENDED,   // User account is temporarily suspended
    DELETED      // User account is soft deleted
}
