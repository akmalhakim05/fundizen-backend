package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.UserService;
import com.fundizen.fundizen_backend.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin // Simple CORS - allows all origins
public class AuthController {

    @Autowired
    private UserService userService;

    private final FirebaseService firebaseService;

    public AuthController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody User user, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(400).body(Map.of("errors", errors));
            }
            
            User createdUser = userService.createUser(user);
            
            Map<String, Object> userResponse = Map.of(
                "id", createdUser.getId(),
                "username", createdUser.getUsername(),
                "email", createdUser.getEmail(),
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

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        try {
            String usernameOrEmail = credentials.get("usernameOrEmail");
            String password = credentials.get("password");
            
            if (usernameOrEmail == null || password == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Username/email and password are required"));
            }
            
            User authenticatedUser = userService.authenticateUser(usernameOrEmail, password);
            
            Map<String, Object> userResponse = Map.of(
                "id", authenticatedUser.getId(),
                "username", authenticatedUser.getUsername(),
                "email", authenticatedUser.getEmail(),
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

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("token");

        if (idToken == null || idToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
        }

        try {
            FirebaseToken decodedToken = firebaseService.verifyIdToken(idToken);
            return ResponseEntity.ok(Map.of(
                    "uid", decodedToken.getUid(),
                    "email", decodedToken.getEmail(),
                    "valid", true
            ));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Firebase token",
                    "valid", false
            ));
        }
    }
}