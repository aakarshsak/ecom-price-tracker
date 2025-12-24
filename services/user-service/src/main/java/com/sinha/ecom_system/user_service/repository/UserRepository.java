package com.sinha.ecom_system.user_service.repository;

import com.sinha.ecom_system.user_service.model.User;
import com.sinha.ecom_system.user_service.model.UserStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> getUserById(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.userStatus = :status, u.updatedAt = :updatedAt WHERE u.id = :id")
    void updateUserStatus(@Param("id") Long id, @Param("status") UserStatus status, @Param("updatedAt") LocalDateTime updatedAt);
}
