package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/test")  // Different path to avoid security
@CrossOrigin(origins = "*")
public class TestController {

    private static final Logger LOGGER = Logger.getLogger(TestController.class.getName());

    @Autowired
    private UserService userService;

    // 🚨 PUBLIC ENDPOINT FOR TESTING ONLY
    @PostMapping("/create-user")
    public ResponseEntity<?> createTestUser(@RequestBody CreateUserRequest request) {
        try {
            LOGGER.info("🧪 TEST: Creating user - " + request.getUsername());
            
            // Validate request
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Username is required"));
            }
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Email is required"));
            }
            
            if (request.getUid() == null || request.getUid().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("UID is required"));
            }

            // Check if user already exists
            User existingUser = userService.findByEmail(request.getEmail());
            if (existingUser != null) {
                return ResponseEntity.ok(createSuccessResponse(
                    "User already exists: " + existingUser.getUsername(),
                    existingUser
                ));
            }

            existingUser = userService.findByUid(request.getUid());
            if (existingUser != null) {
                return ResponseEntity.ok(createSuccessResponse(
                    "User already exists: " + existingUser.getUsername(), 
                    existingUser
                ));
            }

            // Create new user
            User newUser = User.builder()
                    .uid(request.getUid())
                    .email(request.getEmail())
                    .username(request.getUsername())
                    .role(request.getRole() != null ? request.getRole() : "user")
                    .verified(request.isVerified())
                    .build();

            User savedUser = userService.save(newUser);
            
            LOGGER.info("✅ TEST: User created - " + savedUser.getUsername() + " (ID: " + savedUser.getId() + ")");
            
            return ResponseEntity.ok(createSuccessResponse("User created successfully", savedUser));
            
        } catch (Exception e) {
            LOGGER.severe("❌ TEST: Error creating user - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    // Get all users (no auth required for testing)
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.findAll();
            LOGGER.info("📋 TEST: Retrieved " + users.size() + " users");
            return ResponseEntity.ok(createSuccessResponse("Users retrieved", users));
        } catch (Exception e) {
            LOGGER.severe("❌ TEST: Error getting users - " + e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    // Find user by UID (for verification)
    @GetMapping("/user/{uid}")
    public ResponseEntity<?> getUserByUid(@PathVariable String uid) {
        try {
            User user = userService.findByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404).body(createErrorResponse("User not found"));
            }
            return ResponseEntity.ok(createSuccessResponse("User found", user));
        } catch (Exception e) {
            LOGGER.severe("❌ TEST: Error finding user - " + e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    // Delete user (for cleanup)
    @DeleteMapping("/user/{uid}")
    public ResponseEntity<?> deleteUserByUid(@PathVariable String uid) {
        try {
            User user = userService.findByUid(uid);
            if (user == null) {
                return ResponseEntity.status(404).body(createErrorResponse("User not found"));
            }
            
            boolean deleted = userService.deleteById(user.getId());
            if (deleted) {
                LOGGER.info("🗑️ TEST: User deleted - " + uid);
                return ResponseEntity.ok(createSuccessResponse("User deleted", null));
            } else {
                return ResponseEntity.status(500).body(createErrorResponse("Failed to delete user"));
            }
        } catch (Exception e) {
            LOGGER.severe("❌ TEST: Error deleting user - " + e.getMessage());
            return ResponseEntity.status(500).body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    // Helper methods
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    // DTO class
    public static class CreateUserRequest {
        private String uid;
        private String email;
        private String username;
        private String role = "user";
        private boolean verified = true;

        // Constructors
        public CreateUserRequest() {}

        // Getters and setters
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
    }
}