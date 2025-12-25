package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.model.RolePermissions;
import com.sinha.ecom_system.auth_service.repository.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public Role createRole(String name, String description, RolePermissions permissions) {
        // Check if role already exists
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("Role already exists: " + name);
        }

        Role role = Role.builder()
                .name(name)
                .description(description)
                .permissions(permissions)
                .isActive(true)
                .build();

        return roleRepository.save(role);
    }

    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Role not found: " + name));
    }

    public List<Role> getAllActiveRoles() {
        return roleRepository.findByIsActive(true);
    }

    @Transactional
    public Role updateRolePermissions(UUID roleId, RolePermissions permissions) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        role.setPermissions(permissions);
        return roleRepository.save(role);
    }

    @Transactional
    public void deactivateRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        role.setIsActive(false);
        roleRepository.save(role);
    }

    public boolean hasPermission(String roleName, String permission) {
        Role role = getRoleByName(roleName);
        return role.getPermissions() != null &&
                role.getPermissions().hasPermission(permission);
    }
}
