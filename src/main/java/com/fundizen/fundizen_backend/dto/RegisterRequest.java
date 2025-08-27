package com.fundizen.fundizen_backend.dto;

public class RegisterRequest {
    private String username;
    private String token;

    // Default constructor
    public RegisterRequest() {}

    // Constructor with parameters
    public RegisterRequest(String username, String token) {
        this.username = username;
        this.token = token;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

