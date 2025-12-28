package com.sinha.ecom_system.common;

/**
 * KYC (Know Your Customer) verification status
 */
public enum KycStatus {
    PENDING,    // KYC documents submitted, awaiting verification
    VERIFIED,   // KYC approved
    REJECTED,   // KYC rejected
    EXPIRED     // KYC verification expired, needs renewal
}

