package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.UserService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    // User registration
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user, BindingResult result) {
        try {
        // Check for validation errors
        if (result.hasErrors()) {
            List<String> errors = result.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.status(400).body(Map.of("errors", errors));
        }
        
        User createdUser = userService.createUser(user);
        
        // Return only essential user information
        Map<String, Object> userResponse = Map.of(
            "id", createdUser.getId(),
            "role", createdUser.getRole()
        );
        
        return ResponseEntity.ok(Map.of(
            "message", "User registered successfully",
            "user", userResponse
        ));
        
    } catch (RuntimeException e) {
        return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", "Error creating user: " + e.getMessage()));
    }
}

    // User login
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        try {
            String usernameOrEmail = credentials.get("usernameOrEmail");
            String password = credentials.get("password");
            
            if (usernameOrEmail == null || password == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Username/email and password are required"));
            }
            
            User authenticatedUser = userService.authenticateUser(usernameOrEmail, password);
            
            // Return only essential user information
            Map<String, Object> userResponse = Map.of(
                "id", authenticatedUser.getId(),
                "role", authenticatedUser.getRole()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "user", userResponse
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error during authentication: " + e.getMessage()));
        }
    }

    // Get all users (with pagination)
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> users = userService.getAllUsers(pageable);
            
            // Remove passwords from all users for security
            users.getContent().forEach(user -> user.setPassword(null));
            
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching users: " + e.getMessage()));
        }
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        try {
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            // Remove password for security
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching user: " + e.getMessage()));
        }
    }

    // Get user by username
    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            // Remove password for security
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching user: " + e.getMessage()));
        }
    }

    // Get users by role
    @GetMapping("/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        try {
            List<User> users = userService.getUsersByRole(role);
            // Remove passwords for security
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching users by role: " + e.getMessage()));
        }
    }

    // Search users
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String q) {
        try {
            List<User> users = userService.searchUsers(q);
            // Remove passwords for security
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error searching users: " + e.getMessage()));
        }
    }

    // Update user profile
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @Valid @RequestBody User user, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(400).body(Map.of("errors", errors));
            }

            User updatedUser = userService.updateUser(id, user);
            if (updatedUser == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            // Remove password for security
            updatedUser.setPassword(null);
            return ResponseEntity.ok(Map.of(
                "message", "User updated successfully",
                "user", updatedUser
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error updating user: " + e.getMessage()));
        }
    }

    // Update user password
    @PutMapping("/{id}/password")
    public ResponseEntity<?> updateUserPassword(@PathVariable String id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.status(400).body(Map.of("error", "New password must be at least 6 characters"));
            }

            userService.updateUserPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error updating password: " + e.getMessage()));
        }
    }

    // Role management endpoints
    @PostMapping("/{id}/promote")
    public ResponseEntity<?> promoteToAdmin(@PathVariable String id) {
        try {
            User user = userService.promoteToAdmin(id);
            user.setPassword(null);
            return ResponseEntity.ok(Map.of(
                "message", "User promoted to admin successfully",
                "user", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error promoting user: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/demote")
    public ResponseEntity<?> demoteToUser(@PathVariable String id) {
        try {
            User user = userService.demoteToUser(id);
            user.setPassword(null);
            return ResponseEntity.ok(Map.of(
                "message", "User demoted to regular user successfully",
                "user", user
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error demoting user: " + e.getMessage()));
        }
    }

    // Delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            boolean deleted = userService.deleteUser(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error deleting user: " + e.getMessage()));
        }
    }

    // Validation endpoints
    @GetMapping("/check/username/{username}")
    public ResponseEntity<?> checkUsernameAvailability(@PathVariable String username) {
        try {
            boolean available = userService.isUsernameAvailable(username);
            return ResponseEntity.ok(Map.of(
                "username", username,
                "available", available
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error checking username: " + e.getMessage()));
        }
    }

    @GetMapping("/check/email/{email}")
    public ResponseEntity<?> checkEmailAvailability(@PathVariable String email) {
        try {
            boolean available = userService.isEmailAvailable(email);
            return ResponseEntity.ok(Map.of(
                "email", email,
                "available", available
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error checking email: " + e.getMessage()));
        }
    }

    // Statistics endpoints
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> stats = Map.of(
                "totalUsers", userService.getTotalUsers(),
                "adminUsers", userService.getTotalAdmins(),
                "timestamp", LocalDateTime.now()
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching statistics: " + e.getMessage()));
        }
    }

    // Get recent users (last 30 days)
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentUsers(@RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            List<User> users = userService.getRecentUsers(since);
            // Remove passwords for security
            users.forEach(user -> user.setPassword(null));
            return ResponseEntity.ok(Map.of(
                "users", users,
                "period", days + " days",
                "count", users.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error fetching recent users: " + e.getMessage()));
        }
    }
}