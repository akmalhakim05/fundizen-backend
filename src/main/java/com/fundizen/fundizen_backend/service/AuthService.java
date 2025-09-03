package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;
import com.google.firebase.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    // Register user in Firebase + MongoDB
    public User registerUser(String email, String password, String username) throws FirebaseAuthException {
        // Create Firebase account
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setEmailVerified(false)
                .setDisplayName(username);

        UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(request);

        // Save in MongoDB
        User user = new User();
        user.setFirebaseUid(firebaseUser.getUid());
        user.setEmail(email);
        user.setUsername(username);
        user.setRole("user");
        user.setVerified(false);

        return userRepository.save(user);
    }

    // Verify Firebase token & return user
    public User verifyIdToken(String idToken) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

        User user = userRepository.findByEmail(decodedToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found in database"));

        // Sync verified status
        if (decodedToken.isEmailVerified() && !user.isVerified()) {
            user.setVerified(true);
            userRepository.save(user);
        }

        return user;
    }
}
