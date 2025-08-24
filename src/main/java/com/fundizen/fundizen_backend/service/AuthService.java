package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;

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

    // Registers a user with Firebase using the provided token.
    public String registerWithFirebase(String token) throws FirebaseAuthException {
        // Verify the Firebase ID token
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

        if (!decodedToken.isEmailVerified()) {
           return "Email not verified";
        }

        String email = decodedToken.getEmail();
        String uid = decodedToken.getUid();

        // 2. Check if user already exists in MongoDB
        if (userRepository.findByEmail(email).isPresent()) {
            return "User already exists";
        }

        User user = new User();
        user.setUid(uid);
        user.setEmail(email);
        user.setVerified(true);
        userRepository.save(user);

        return "User registered successfully";
    }

    // Logs in a user with Firebase using the provided token.
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

