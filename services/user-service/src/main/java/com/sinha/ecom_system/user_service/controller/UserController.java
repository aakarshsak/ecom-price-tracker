package com.sinha.ecom_system.user_service.controller;

import com.sinha.ecom_system.user_service.Constants;
import com.sinha.ecom_system.user_service.dto.BasicResponse;
import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.model.UserStatus;
import com.sinha.ecom_system.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/users")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("")
    public ResponseEntity<BasicResponse> addUser(@RequestBody RegisterUserRequest body) {
        userService.addUser(body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BasicResponse.builder()
                        .status(Constants.SUCCESS)
                        .message("User addition successful.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) throws Exception {
        return ResponseEntity.status(HttpStatus.OK).body(userService.getUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BasicResponse> updateUser(@PathVariable Long id, @RequestBody RegisterUserRequest body) throws Exception{
        userService.updateUser(id, body);

        return ResponseEntity.status(HttpStatus.ACCEPTED).
                body(BasicResponse.builder()
                        .status(Constants.SUCCESS)
                        .message("Successfully updated the user")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @PatchMapping("/{id}/update-status")
    public ResponseEntity<BasicResponse> makeUserActive(@PathVariable Long id, @RequestParam(defaultValue = "ACTIVE") UserStatus status) {
        userService.updateUserStatus(id, status);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(BasicResponse.builder().build());
    }
}
