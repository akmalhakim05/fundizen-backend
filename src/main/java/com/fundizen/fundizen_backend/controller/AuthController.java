package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.AuthService;
import com.google.firebase.auth.FirebaseAuthException;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PostMapping;

// Defines HTTP endpoints
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 🔐 Firebase‐backed registration
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestHeader("Authorization") String authHeader) {
        try {
            // 1. Strip "Bearer " prefix
            String token = authHeader.replace("Bearer ", "");
            // 2. Delegate to AuthService
            String result = authService.registerWithFirebase(token);
            // 3. Return 200 OK with message
            return ResponseEntity.ok(result);
        } catch (FirebaseAuthException e) {
            // 4. Invalid/expired Firebase token
            return ResponseEntity.status(401).body("Invalid Firebase token.");
        }
    }

    // 🔑 Firebase token–based login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String result = authService.loginWithFirebase(token);
            return ResponseEntity.ok(result);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("❌ Invalid Firebase token");
        }
    }
}
