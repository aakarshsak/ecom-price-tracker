package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.model.User;

public interface UserService {

    void addUser(RegisterUserRequest user);

    UserDTO getUser(Long id) throws Exception;
}
