package com.sinha.ecom_system.user_service.controller;

import com.sinha.ecom_system.user_service.Constants;
import com.sinha.ecom_system.user_service.dto.BasicResponse;
import com.sinha.ecom_system.user_service.dto.RegisterUserRequest;
import com.sinha.ecom_system.user_service.dto.UserDTO;
import com.sinha.ecom_system.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
        return ResponseEntity.status(200).body(userService.getUser(id));
    }
}
