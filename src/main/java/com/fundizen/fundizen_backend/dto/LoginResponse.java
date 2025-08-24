package com.fundizen.fundizen_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user login response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String message;
}
