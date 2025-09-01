package com.fundizen.fundizen_backend.dto;

import jakarta.validation.constraints.*;

public class UserRequestDTO {
    
    // Registration DTO
    public static class RegisterRequest {
        @NotNull(message = "Username is required")
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        private String username;

        @NotNull(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @NotNull(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;

        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
        private String phoneNumber;

        // Constructors
        public RegisterRequest() {}

        public RegisterRequest(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    // Login DTO
    public static class LoginRequest {
        @NotNull(message = "Username or email is required")
        @Size(min = 3, max = 100, message = "Username or email must be between 3 and 100 characters")
        private String usernameOrEmail;

        @NotNull(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;

        // Constructors
        public LoginRequest() {}

        public LoginRequest(String usernameOrEmail, String password) {
            this.usernameOrEmail = usernameOrEmail;
            this.password = password;
        }

        // Getters and setters
        public String getUsernameOrEmail() { return usernameOrEmail; }
        public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // Update Profile DTO
    public static class UpdateProfileRequest {
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
        private String username;

        @Email(message = "Please provide a valid email address")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @Size(max = 50, message = "First name must not exceed 50 characters")
        private String firstName;

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        private String lastName;

        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Please provide a valid phone number")
        private String phoneNumber;

        // Constructors
        public UpdateProfileRequest() {}

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    // Change Password DTO
    public static class ChangePasswordRequest {
        @NotNull(message = "Current password is required")
        private String currentPassword;

        @NotNull(message = "New password is required")
        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
        private String newPassword;

        @NotNull(message = "Password confirmation is required")
        private String confirmPassword;

        // Constructors
        public ChangePasswordRequest() {}

        public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
            this.confirmPassword = confirmPassword;
        }

        // Custom validation method
        @AssertTrue(message = "New password and confirmation password must match")
        public boolean isPasswordMatching() {
            return newPassword != null && newPassword.equals(confirmPassword);
        }

        // Getters and setters
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    // Reset Password DTO
    public static class ResetPasswordRequest {
        @NotNull(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        // Constructors
        public ResetPasswordRequest() {}

        public ResetPasswordRequest(String email) {
            this.email = email;
        }

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
