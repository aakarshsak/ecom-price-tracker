package com.sinha.ecom_system.user_service.repository;

import com.sinha.ecom_system.common.enums.UserStatus;
import com.sinha.ecom_system.user_service.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> getUserById(UUID id);

    Optional<User> getUserByEmail(String email);

    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userStatus = :status, u.updatedAt = :updatedAt WHERE u.id = :id")
    void updateUserStatus(@Param("id") UUID id, @Param("status") UserStatus status, @Param("updatedAt") LocalDateTime updatedAt);
}
