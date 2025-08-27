package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;
import com.fundizen.fundizen_backend.dto.RegisterRequest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    // Registers a user with Firebase using the provided token and username.
    public String registerWithFirebase(RegisterRequest request) throws FirebaseAuthException {
        // Verify Firebase ID token
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getToken());

        if (!decodedToken.isEmailVerified()) {
            return "Email not verified";
        }

        String email = decodedToken.getEmail();
        String uid = decodedToken.getUid();
                String name = decodedToken.getName(); // Get name from Firebase token


        // Check if user exists
        if (userRepository.findByEmail(email).isPresent()) {
            return "User already exists";
        }

        // Save new user with username from frontend
        User user = User.builder()
                .uid(uid)
                .email(email)
                .username(request.getUsername())
                .role("user")
                .verified(true)
                .build();

        userRepository.save(user);

        return "User registered successfully with username: " + request.getUsername();
    }

    // Login method (unchanged)
    public String loginWithFirebase(String token) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

        String email = decodedToken.getEmail();
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            return "Login successful: " + email;
        } else {
            return "User not registered";
        }
    }
}
