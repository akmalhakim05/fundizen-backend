package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.AuthService;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // Register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            User user = authService.registerUser(
                    body.get("email"),
                    body.get("password"),
                    body.get("username")
            );
            return ResponseEntity.ok(user);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify token & check login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String idToken = body.get("idToken"); // frontend must send Firebase ID token
            User user = authService.verifyIdToken(idToken);

            if (!user.isVerified()) {
                return ResponseEntity.status(403).body(Map.of("error", "Email not verified"));
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
