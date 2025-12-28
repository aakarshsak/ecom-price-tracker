package com.sinha.ecom_system.user_service.service;

import com.sinha.ecom_system.common.RiskProfile;
import com.sinha.ecom_system.common.dto.UserInfoRequest;
import com.sinha.ecom_system.common.dto.UserInfoResponse;
import com.sinha.ecom_system.common.dto.UserStatus;
import com.sinha.ecom_system.user_service.model.User;
import com.sinha.ecom_system.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserInfoResponse addUser(UserInfoRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new User entity
        User userModel = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .dob(request.getDob())
                .gender(request.getGender())
                .nationality(request.getNationality())
                .riskProfile(RiskProfile.CONSERVATIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Persist to database (ID auto-generated)
        userModel = userRepository.save(userModel);

        // Return auth response with tokens and user info
        return UserInfoResponse.builder()
                .userId(userModel.getId())
                .firstName(userModel.getFirstName())
                .lastName(userModel.getLastName())
                .email(userModel.getEmail())
                .mobileNumber(userModel.getMobileNumber())
                .dob(userModel.getDob())
                .gender(userModel.getGender())
                .nationality(userModel.getNationality())
                .userStatus(userModel.getUserStatus())
                .accountType(userModel.getAccountType())
                .kycStatus(userModel.getKycStatus())
                .kycVerifiedAt(userModel.getKycVerifiedAt())
                .kycVerifiedBy(userModel.getKycVerifiedBy())
                .tradingStatus(userModel.getTradingStatus())
                .riskProfile(userModel.getRiskProfile())
                .createdAt(userModel.getCreatedAt())
                .updatedAt(userModel.getUpdatedAt())
                .lastActiveAt(userModel.getLastActiveAt())
                .deletedAt(userModel.getDeletedAt())
                .build();
    }


    @Override
    public UserInfoResponse getUser(UUID id) throws Exception {
        Optional<User> optionalUser = userRepository.getUserById(id);

        User userModel = optionalUser.orElseThrow(() -> new Exception("User not found"));

        return UserInfoResponse.builder()
                .userId(userModel.getId())
                .firstName(userModel.getFirstName())
                .lastName(userModel.getLastName())
                .email(userModel.getEmail())
                .mobileNumber(userModel.getMobileNumber())
                .dob(userModel.getDob())
                .gender(userModel.getGender())
                .nationality(userModel.getNationality())
                .userStatus(userModel.getUserStatus())
                .accountType(userModel.getAccountType())
                .kycStatus(userModel.getKycStatus())
                .kycVerifiedAt(userModel.getKycVerifiedAt())
                .kycVerifiedBy(userModel.getKycVerifiedBy())
                .tradingStatus(userModel.getTradingStatus())
                .riskProfile(userModel.getRiskProfile())
                .createdAt(userModel.getCreatedAt())
                .updatedAt(userModel.getUpdatedAt())
                .lastActiveAt(userModel.getLastActiveAt())
                .deletedAt(userModel.getDeletedAt())
                .build();
    }


    public User getUserFromDB(UUID id) throws Exception {
        Optional<User> optionalUser = userRepository.getUserById(id);

        User val = optionalUser.orElseThrow(() -> new Exception("User not found"));

        return val;
    }

    @Override
    public UserInfoResponse updateUser(UUID id, UserInfoRequest body) throws Exception {

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

        if (body.getGender() != null) {
            existingUser.setGender(body.getGender());
        }

        if (body.getNationality() != null && !body.getNationality().trim().isEmpty()) {
            existingUser.setNationality(body.getNationality());
        }

        // Update the updatedAt timestamp
        existingUser.setUpdatedAt(LocalDateTime.now());

        // Save and return the updated user
        User updatedUser = userRepository.save(existingUser);

        return UserInfoResponse.builder()
                .userId(updatedUser.getId())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .email(updatedUser.getEmail())
                .mobileNumber(updatedUser.getMobileNumber())
                .dob(updatedUser.getDob())
                .gender(updatedUser.getGender())
                .nationality(updatedUser.getNationality())
                .userStatus(updatedUser.getUserStatus())
                .accountType(updatedUser.getAccountType())
                .kycStatus(updatedUser.getKycStatus())
                .kycVerifiedAt(updatedUser.getKycVerifiedAt())
                .kycVerifiedBy(updatedUser.getKycVerifiedBy())
                .tradingStatus(updatedUser.getTradingStatus())
                .riskProfile(updatedUser.getRiskProfile())
                .createdAt(updatedUser.getCreatedAt())
                .updatedAt(updatedUser.getUpdatedAt())
                .lastActiveAt(updatedUser.getLastActiveAt())
                .deletedAt(updatedUser.getDeletedAt())
                .build();
    }

    @Override
    public void updateUserStatus(UUID id, UserStatus status) {
        userRepository.updateUserStatus(id, status, LocalDateTime.now());
    }
}


