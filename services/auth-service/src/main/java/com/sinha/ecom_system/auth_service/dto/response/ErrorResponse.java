package com.sinha.ecom_system.auth_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {
    private String status;
    private String message;
    private Integer errorCode;
    private LocalDateTime timestamp;
}