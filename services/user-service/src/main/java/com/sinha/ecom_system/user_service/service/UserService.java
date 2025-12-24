package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.model.User;
import com.sinha.ecom_system.user_service.model.UserStatus;

public interface UserService {

    void addUser(RegisterUserRequest user);

    UserDTO getUser(Long id) throws Exception;

    void updateUser(Long id, RegisterUserRequest body) throws Exception;

    void updateUserStatus(Long id, UserStatus userStatus);

    void makeUserActive(Long id);

    void makeUserInactive(Long id);

    void makeUserDisabled(Long id);
}
