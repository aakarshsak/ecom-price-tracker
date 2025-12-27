package com.sinha.ecom_system.auth_service.proxy;

import com.sinha.ecom_system.auth_service.dto.response.ApiResponse;
import com.sinha.ecom_system.auth_service.dto.user.UserInfoRequest;
import com.sinha.ecom_system.auth_service.dto.user.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "user-service", url = "http://localhost:8000/users")
public interface UserProxy {

    @PostMapping("")
    ResponseEntity<ApiResponse<UserInfoResponse>> addUser(@RequestBody UserInfoRequest body);

    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<UserInfoResponse>> getUser(@PathVariable UUID id);

}
