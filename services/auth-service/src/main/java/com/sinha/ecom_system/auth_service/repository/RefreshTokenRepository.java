package com.sinha.ecom_system.auth_service.repository;

import com.sinha.ecom_system.auth_service.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Find by token hash
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Find all tokens for a user
    List<RefreshToken> findByAuthId(UUID authId);

    // Find all valid (not revoked, not expired) tokens for a user
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.authId = :authId AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findValidTokensByAuthId(@Param("authId") UUID authId, @Param("now") LocalDateTime now);

    // Revoke all tokens for a user
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.authId = :authId AND rt.revoked = false")
    int revokeAllUserTokens(@Param("authId") UUID authId, @Param("now") LocalDateTime now);

    // Revoke specific token
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :now WHERE rt.tokenHash = :tokenHash")
    int revokeToken(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    // Delete expired tokens (cleanup job)
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revokedAt < :cutoffDate")
    int deleteExpiredTokens(@Param("now") LocalDateTime now, @Param("cutoffDate") LocalDateTime cutoffDate);

    // Count active tokens for a user
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.authId = :authId AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByAuthId(@Param("authId") UUID authId, @Param("now") LocalDateTime now);
}

