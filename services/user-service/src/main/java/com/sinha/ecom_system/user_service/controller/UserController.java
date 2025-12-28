package com.sinha.ecom_system.user_service.controller;

import com.sinha.ecom_system.common.dto.*;
import com.sinha.ecom_system.common.enums.UserStatus;
import com.sinha.ecom_system.user_service.Constants;
import com.sinha.ecom_system.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("")
    public ResponseEntity<ApiResponse<UserInfoResponse>> addUser(@RequestBody UserInfoRequest body) {
        UserInfoResponse userInfoResposne = userService.addUser(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserInfoResponse>builder()
                        .status(Constants.SUCCESS)
                        .message("User addition successful.")
                        .data(userInfoResposne)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUser(@PathVariable UUID id) throws Exception {
        UserInfoResponse userInfoResponse = userService.getUser(id);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<UserInfoResponse>builder()
                .status(Constants.SUCCESS)
                .message("User addition successful.")
                .data(userInfoResponse)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUser(@PathVariable UUID id, @RequestBody UserInfoRequest body) throws Exception{
        UserInfoResponse userInfoResponse = userService.updateUser(id, body);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.<UserInfoResponse>builder()
                .status(Constants.SUCCESS)
                .message("Updated User Successfully.")
                .data(userInfoResponse)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/update-status")
    public ResponseEntity<MessageResponse> makeUserActive(@PathVariable UUID id, @RequestParam(defaultValue = "ACTIVE") UserStatus status) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(MessageResponse.builder().build());
    }
}
