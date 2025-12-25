package com.sinha.ecom_system.auth_service.repository;

import com.sinha.ecom_system.auth_service.model.UserRole;
import com.sinha.ecom_system.auth_service.model.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    // Find all roles for a user
    List<UserRole> findByAuthCredential_Id(UUID authId);

    // Find all active roles for a user
    @Query("SELECT ur FROM UserRole ur WHERE ur.authCredential.id = :authId AND ur.isActive = true")
    List<UserRole> findActiveRolesByAuthId(@Param("authId") UUID authId);

    // Find all users with a specific role
    List<UserRole> findByRole_Name(String roleName);

    // Check if user has a specific role
    boolean existsByAuthCredential_IdAndRole_Name(UUID id, String roleName);

    // Delete all roles for a user
    void deleteByAuthCredential_Id(UUID id);
}
