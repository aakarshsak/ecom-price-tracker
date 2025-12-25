package com.sinha.ecom_system.auth_service.repository;

import com.sinha.ecom_system.auth_service.model.AuthCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthRepository extends JpaRepository<AuthCredential, UUID> {

    // Find by email
    Optional<AuthCredential> findByEmail(String email);

    // Find by id
    Optional<AuthCredential> findById(UUID id);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find auth credential with roles loaded
    @Query("SELECT ac FROM AuthCredential ac LEFT JOIN FETCH ac.userRoles ur LEFT JOIN FETCH ur.role WHERE ac.email = :email")
    Optional<AuthCredential> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT ac FROM AuthCredential ac LEFT JOIN FETCH ac.userRoles ur LEFT JOIN FETCH ur.role WHERE ac.id = :id")
    Optional<AuthCredential> findByUserIdWithRoles(@Param("id") UUID id);
}