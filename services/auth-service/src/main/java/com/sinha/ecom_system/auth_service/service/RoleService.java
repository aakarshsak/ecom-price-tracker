package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.model.RolePermissions;

import java.util.List;
import java.util.UUID;

public interface RoleService {
    public Role createRole(String name, String description, RolePermissions permissions);

    public Role getRoleByName(String name);

    public List<Role> getAllActiveRoles();

    public Role updateRolePermissions(UUID roleId, RolePermissions permissions);

    public void deactivateRole(UUID roleId);

    public boolean hasPermission(String roleName, String permission);
}
