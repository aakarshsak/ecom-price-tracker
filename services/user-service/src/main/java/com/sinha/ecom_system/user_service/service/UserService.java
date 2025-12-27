package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.user_service.dto.UserInfoRequest;
import com.sinha.ecom_system.user_service.dto.UserInfoResponse;
import com.sinha.ecom_system.user_service.model.UserStatus;

import java.util.UUID;

public interface UserService {

    UserInfoResponse addUser(UserInfoRequest user);

    UserInfoResponse getUser(UUID id) throws Exception;

    UserInfoResponse updateUser(UUID id, UserInfoRequest body) throws Exception;

    void updateUserStatus(UUID id, UserStatus userStatus);
}
