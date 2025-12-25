package com.sinha.ecom_system.auth_service.config;


import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.model.RolePermissions;
import com.sinha.ecom_system.auth_service.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            // Only initialize if roles don't exist
            if (roleRepository.count() == 0) {

                // ROLE_ADMIN
                Role adminRole = Role.builder()
                        .name("ROLE_ADMIN")
                        .description("System Administrator")
                        .permissions(RolePermissions.builder()
                                .canTrade(true)
                                .canWithdraw(true)
                                .canManageUsers(true)
                                .canViewReports(true)
                                .canModifyOrders(true)
                                .canAccessAPI(true)
                                .build())
                        .isActive(true)
                        .build();

                // ROLE_TRADER
                Role traderRole = Role.builder()
                        .name("ROLE_TRADER")
                        .description("Active Trader")
                        .permissions(RolePermissions.builder()
                                .canTrade(true)
                                .canWithdraw(true)
                                .canManageUsers(false)
                                .canViewReports(true)
                                .canModifyOrders(true)
                                .canAccessAPI(true)
                                .build())
                        .isActive(true)
                        .build();

                // ROLE_USER
                Role userRole = Role.builder()
                        .name("ROLE_USER")
                        .description("Basic User")
                        .permissions(RolePermissions.builder()
                                .canTrade(false)
                                .canWithdraw(false)
                                .canManageUsers(false)
                                .canViewReports(false)
                                .canModifyOrders(false)
                                .canAccessAPI(false)
                                .build())
                        .isActive(true)
                        .build();

                roleRepository.save(adminRole);
                roleRepository.save(traderRole);
                roleRepository.save(userRole);

                System.out.println("✅ Default roles initialized!");
            }
        };
    }
}