package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.AuthService;
import com.google.firebase.auth.FirebaseAuthException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Defines HTTP endpoints
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 🔐 Firebase‐backed registration
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Missing or invalid Authorization header.");
            }
            String token = authHeader.substring(7); // Remove "Bearer "
            String result = authService.registerWithFirebase(token);
            return ResponseEntity.ok(result);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Invalid Firebase token.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error.");
        }
    }

    // 🔑 Firebase token–based login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(400).body("Missing or invalid Authorization header.");
            }
            String token = authHeader.substring(7);
            String result = authService.loginWithFirebase(token);
            return ResponseEntity.ok(result);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Invalid Firebase token.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error.");
        }
    }
}