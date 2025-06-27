package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.model.User;
import com.fundizen.fundizen_backend.repository.UserRepository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;            
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;

    public String registerWithFirebase(String token) throws FirebaseAuthException {
        // Verify the Firebase ID token (https://firebase.google.com/docs/auth/admin/verify-id-tokens#java)
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

        if (!decodedToken.isEmailVerified()) {
           return "Email not verified";
        }

        String email = decodedToken.getEmail();
        String uid = decodedToken.getUid();

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
}
