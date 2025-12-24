package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.model.User;
import com.sinha.ecom_system.user_service.model.UserStatus;
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
                .userStatus(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
                .userStatus(val.getUserStatus())
                .createdAt(val.getCreatedAt())
                .updatedAt(val.getUpdatedAt())
                .email(val.getEmail())
                .build();
    }


    public User getUserFromDB(Long id) throws Exception {
        Optional<User> optionalUser = userRepository.getUserById(id);

        User val = optionalUser.orElseThrow(() -> new Exception("User not found"));

        return val;
    }

    @Override
    public void updateUser(Long id, RegisterUserRequest body) throws Exception {

        User existingUser = getUserFromDB(id);

        // Update only non-null fields (partial update)
        if (body.getFirstName() != null && !body.getFirstName().trim().isEmpty()) {
            existingUser.setFirstName(body.getFirstName());
        }

        if (body.getLastName() != null && !body.getLastName().trim().isEmpty()) {
            existingUser.setLastName(body.getLastName());
        }

        if (body.getEmail() != null && !body.getEmail().trim().isEmpty()) {
            existingUser.setEmail(body.getEmail());
        }

        if (body.getMobileNumber() != null && !body.getMobileNumber().trim().isEmpty()) {
            existingUser.setMobileNumber(body.getMobileNumber());
        }

        if (body.getDob() != null) {
            existingUser.setDob(body.getDob());
        }

        // Update the updatedAt timestamp
        existingUser.setUpdatedAt(LocalDateTime.now());

        userRepository.save(existingUser);
    }

    @Override
    public void updateUserStatus(Long id, UserStatus status) {
        switch (status) {
            case ACTIVE -> {
                makeUserActive(id);
            }
            case INACTIVE -> {
                makeUserInactive(id);
            }
            case DISABLED -> {
                makeUserDisabled(id);
            }
        }
    }

    @Override
    public void makeUserActive(Long id) {
        userRepository.updateUserStatus(id, UserStatus.ACTIVE, LocalDateTime.now());
    }

    @Override
    public void makeUserInactive(Long id) {
        userRepository.updateUserStatus(id, UserStatus.INACTIVE, LocalDateTime.now());
    }

    @Override
    public void makeUserDisabled(Long id) {
        userRepository.updateUserStatus(id, UserStatus.DISABLED, LocalDateTime.now());
    }
}


