// 1. Enhanced AuthService.java - COMPLETE FIX
package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;
import com.fundizen.fundizen_backend.dto.RegisterRequest;

import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

@Service
public class AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    @Autowired
    private UserRepository userRepository;

    public String registerWithFirebase(RegisterRequest request) throws FirebaseAuthException {
        try {
            LOGGER.info("Starting registration process for token: " + request.getToken().substring(0, 10) + "...");
            
            // Verify Firebase ID token
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getToken());
            LOGGER.info("Firebase token verified successfully");

            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();
            String name = decodedToken.getName();
            
            LOGGER.info("Processing registration for UID: " + uid + ", Email: " + email);

            // Check if email is verified
            if (!decodedToken.isEmailVerified()) {
                LOGGER.warning("Email not verified for: " + email);
                throw new RuntimeException("Email not verified. Please check your email and verify your account.");
            }

            // Check if user already exists in our database
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                LOGGER.warning("User already exists with email: " + email);
                return "User already registered. Please login instead.";
            }

            Optional<User> existingUserByUid = userRepository.findByUid(uid);
            if (existingUserByUid.isPresent()) {
                LOGGER.warning("User already exists with UID: " + uid);
                return "User already registered. Please login instead.";
            }

            // Create and save new user
            User newUser = User.builder()
                    .uid(uid)
                    .email(email)
                    .username(request.getUsername()) // Use username from frontend
                    .role("user") // Default role
                    .verified(true) // Since Firebase email is verified
                    .build();

            LOGGER.info("Attempting to save user to MongoDB: " + newUser.toString());
            User savedUser = userRepository.save(newUser);
            LOGGER.info("User saved successfully to MongoDB with ID: " + savedUser.getId());

            return "User registered successfully with username: " + request.getUsername();

        } catch (FirebaseAuthException e) {
            LOGGER.severe("Firebase authentication error: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.severe("Registration error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    public String loginWithFirebase(String token) throws FirebaseAuthException {
        try {
            LOGGER.info("Starting login process");
            
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();
            
            LOGGER.info("Processing login for UID: " + uid + ", Email: " + email);

            // Check if user exists in our database
            Optional<User> existingUser = userRepository.findByUid(uid);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                LOGGER.info("Login successful for user: " + user.getUsername());
                return "Login successful: " + email;
            } else {
                LOGGER.warning("User not found in database for UID: " + uid);
                
                // Try to create user automatically if they exist in Firebase but not in our DB
                // This handles cases where Firebase user exists but MongoDB user doesn't
                try {
                    User newUser = User.builder()
                            .uid(uid)
                            .email(email)
                            .username(email.split("@")[0]) // Use email prefix as username
                            .role("user")
                            .verified(decodedToken.isEmailVerified())
                            .build();
                    
                    User savedUser = userRepository.save(newUser);
                    LOGGER.info("Auto-created user in database: " + savedUser.getUsername());
                    return "Login successful (user auto-created): " + email;
                    
                } catch (Exception e) {
                    LOGGER.severe("Failed to auto-create user: " + e.getMessage());
                    throw new RuntimeException("User not registered in our system. Please register first.");
                }
            }
        } catch (FirebaseAuthException e) {
            LOGGER.severe("Firebase authentication error during login: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            LOGGER.severe("Login error: " + e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }
}