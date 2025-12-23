package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.model.User;
import com.sinha.ecom_system.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void addUser(RegisterUserRequest user) {
        User userModel = User.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .dob(user.getDob())
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(userModel);
    }


    @Override
    public UserDTO getUser(Long id) throws Exception {
        Optional<User> optionalUser = userRepository.getUserById(id);

        User val = optionalUser.orElseThrow(() -> new Exception("User not found"));

        return UserDTO.builder()
                .firstName(val.getFirstName())
                .lastName(val.getLastName())
                .id(val.getId())
                .mobileNumber(val.getMobileNumber())
                .dob(val.getDob())
                .createdAt(val.getCreatedAt())
                .email(val.getEmail())
                .build();
    }
}
