package com.sinha.ecom_system.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class BasicResponse {
    private String status;
    private String message;
    private LocalDateTime timestamp;
}
