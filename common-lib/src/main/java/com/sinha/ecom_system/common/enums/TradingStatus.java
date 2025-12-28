package com.sinha.ecom_system.common.enums;

/**
 * Trading permission status for users
 */
public enum TradingStatus {
    RESTRICTED,  // Trading not allowed (default for new users)
    ENABLED,     // Trading allowed
    SUSPENDED    // Trading temporarily suspended
}

