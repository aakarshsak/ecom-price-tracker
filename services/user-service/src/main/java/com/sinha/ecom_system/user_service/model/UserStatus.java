package com.sinha.ecom_system.user_service.model;

/**
 * Overall user account status
 */
public enum UserStatus {
    ACTIVE,      // User account is active and operational
    INACTIVE,    // User account is inactive (not deleted)
    SUSPENDED,   // User account is temporarily suspended
    DELETED      // User account is soft deleted
}
