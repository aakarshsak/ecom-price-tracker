package com.sinha.ecom_system.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String status;      // "success" or "error"
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String requestId;   // For tracing/debugging
}