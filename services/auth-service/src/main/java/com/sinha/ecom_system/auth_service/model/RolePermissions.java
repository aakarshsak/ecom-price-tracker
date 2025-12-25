package com.sinha.ecom_system.auth_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissions implements Serializable {

    @JsonProperty("canTrade")
    private Boolean canTrade;

    @JsonProperty("canWithdraw")
    private Boolean canWithdraw;

    @JsonProperty("canManageUsers")
    private Boolean canManageUsers;

    @JsonProperty("canViewReports")
    private Boolean canViewReports;

    @JsonProperty("canModifyOrders")
    private Boolean canModifyOrders;

    @JsonProperty("canAccessAPI")
    private Boolean canAccessAPI;

    // Helper methods
    public boolean hasPermission(String permission) {
        switch (permission.toLowerCase()) {
            case "trade":
                return Boolean.TRUE.equals(canTrade);
            case "withdraw":
                return Boolean.TRUE.equals(canWithdraw);
            case "manageusers":
                return Boolean.TRUE.equals(canManageUsers);
            case "viewreports":
                return Boolean.TRUE.equals(canViewReports);
            case "modifyorders":
                return Boolean.TRUE.equals(canModifyOrders);
            case "accessapi":
                return Boolean.TRUE.equals(canAccessAPI);
            default:
                return false;
        }
    }
}
