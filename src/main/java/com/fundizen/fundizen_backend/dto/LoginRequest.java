package com.fundizen.fundizen_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * DTO for user login request.
 */
@Data
@AllArgsConstructor
@Builder
public class LoginRequest {
    @Email(message="Invalid email format")
    @NotBlank(message="Email is required")
    private final String email;

    @NotBlank(message="Password is required")
    private final String password;
}
