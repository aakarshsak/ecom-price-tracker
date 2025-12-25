package com.sinha.ecom_system.auth_service.repository;

import com.sinha.ecom_system.auth_service.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    // Find by name
    Optional<Role> findByName(String name);

    // Check if role exists by name
    boolean existsByName(String name);

    // Find all active roles
    List<Role> findByIsActive(Boolean isActive);

    // Find roles by name pattern
    List<Role> findByNameContainingIgnoreCase(String namePattern);

    // Custom query to find roles with specific permission
    @Query(value = "SELECT * FROM roles WHERE permissions->>'canTrade' = 'true'",
            nativeQuery = true)
    List<Role> findRolesWithTradePermission();

    @Query(value = "SELECT * FROM roles WHERE permissions->>'canManageUsers' = 'true'",
            nativeQuery = true)
    List<Role> findRolesWithUserManagementPermission();
}
