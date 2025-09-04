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

    /**
     * Register user with Firebase token verification
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, Object> request) {
        try {
            // Extract token and user data
            String firebaseToken = (String) request.get("token");
            Map<String, String> userData = (Map<String, String>) request.get("user");
            
            // Validate required fields
            if (firebaseToken == null || firebaseToken.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Firebase token is required"));
            }
            
            if (userData == null) {
                return ResponseEntity.status(400).body(Map.of("error", "User data is required"));
            }

            // Verify Firebase token FIRST
            FirebaseToken decodedToken;
            try {
                decodedToken = firebaseService.verifyIdToken(firebaseToken);
            } catch (FirebaseAuthException e) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Firebase token", 
                    "details", e.getMessage()
                ));
            }

            // Create user object from request data
            User user = new User();
            user.setUsername(userData.get("username"));
            user.setEmail(userData.get("email"));
            user.setPassword(userData.get("password"));

            // Validate user data manually
            if (user.getUsername() == null || user.getUsername().length() < 3) {
                return ResponseEntity.status(400).body(Map.of("error", "Username must be at least 3 characters"));
            }
            
            if (user.getEmail() == null || !user.getEmail().contains("@")) {
                return ResponseEntity.status(400).body(Map.of("error", "Valid email is required"));
            }
            
            if (user.getPassword() == null || user.getPassword().length() < 6) {
                return ResponseEntity.status(400).body(Map.of("error", "Password must be at least 6 characters"));
            }

            // Verify email matches Firebase token email
            if (!user.getEmail().equals(decodedToken.getEmail())) {
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Email in user data must match Firebase token email"
                ));
            }

            // Create user with Firebase verification
            User createdUser = userService.createUserFromFirebase(user, decodedToken.getUid());
            
            Map<String, Object> userResponse = Map.of(
                "id", createdUser.getId(),
                "username", createdUser.getUsername(),
                "email", createdUser.getEmail(),
                "role", createdUser.getRole(),
                "verified", createdUser.isVerified(),
                "firebaseUid", createdUser.getUid()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully with Firebase verification",
                "user", userResponse,
                "firebaseVerified", true
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error creating user: " + e.getMessage()));
        }
    }

    /**
     * Login user with Firebase token verification
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        try {
            // Extract token and credentials
            String firebaseToken = request.get("token");
            String usernameOrEmail = request.get("usernameOrEmail");
            String password = request.get("password");
            
            // Validate required fields
            if (firebaseToken == null || firebaseToken.trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Firebase token is required"));
            }
            
            if (usernameOrEmail == null || password == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Username/email and password are required"));
            }

            // Verify Firebase token FIRST
            FirebaseToken decodedToken;
            try {
                decodedToken = firebaseService.verifyIdToken(firebaseToken);
            } catch (FirebaseAuthException e) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Firebase token", 
                    "details", e.getMessage()
                ));
            }

            // Authenticate user with traditional credentials
            User authenticatedUser = userService.authenticateUser(usernameOrEmail, password);
            
            // CHECK: Block login if user is not verified
            if (!authenticatedUser.isVerified()) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "Account not verified. Please verify your account before logging in.",
                    "verified", false,
                    "userId", authenticatedUser.getId()
                ));
            }
            
            // Optional: Link Firebase UID if not already linked
            if (authenticatedUser.getUid() == null || authenticatedUser.getUid().isEmpty()) {
                authenticatedUser = userService.linkFirebaseAccount(authenticatedUser.getId(), decodedToken.getUid());
            }

            Map<String, Object> userResponse = Map.of(
                "id", authenticatedUser.getId(),
                "username", authenticatedUser.getUsername(),
                "email", authenticatedUser.getEmail(),
                "role", authenticatedUser.getRole(),
                "verified", authenticatedUser.isVerified()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Login successful with Firebase verification",
                "user", userResponse,
                "firebaseVerified", true
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error during authentication: " + e.getMessage()));
        }
    }

    /**
     * Standalone token verification (optional - for frontend testing)
     * POST /api/auth/verify-token
     */
    @PostMapping("/verify-token")
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
                    "valid", true,
                    "message", "Token is valid"
            ));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Firebase token",
                    "valid", false,
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * Legacy endpoints (without Firebase token verification) - Optional fallback
     */
    @PostMapping("/legacy/register")
    public ResponseEntity<?> legacyRegisterUser(@Valid @RequestBody User user, BindingResult result) {
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
                "role", createdUser.getRole(),
                "verified", createdUser.isVerified()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "User registered successfully (legacy mode)",
                "user", userResponse,
                "firebaseVerified", false
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error creating user: " + e.getMessage()));
        }
    }

    @PostMapping("/legacy/login")
    public ResponseEntity<?> legacyLoginUser(@RequestBody Map<String, String> credentials) {
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
                "role", authenticatedUser.getRole(),
                "verified", authenticatedUser.isVerified()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Login successful (legacy mode)",
                "user", userResponse,
                "firebaseVerified", false
            ));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error during authentication: " + e.getMessage()));
        }
    }
}