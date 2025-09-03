package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final FirebaseService firebaseService;

    public AuthController(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("token");

        try {
            FirebaseToken decodedToken = firebaseService.verifyIdToken(idToken);
            return ResponseEntity.ok(Map.of(
                    "uid", decodedToken.getUid(),
                    "email", decodedToken.getEmail(),
                    "claims", decodedToken.getClaims()
            ));
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Invalid Firebase token: " + e.getMessage());
        }
    }
}
