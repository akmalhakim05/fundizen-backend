package com.fundizen.fundizen_backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    private final FirebaseAuth firebaseAuth;

    public FirebaseService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /**
     * Verify Firebase ID token and extract user information
     */
    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            logger.error("Error verifying Firebase token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get user info from Firebase by UID
     */
    public UserRecord getUserByUid(String uid) throws FirebaseAuthException {
        return firebaseAuth.getUser(uid);
    }

    /**
     * Create a Firebase user
     */
    public UserRecord createUser(String email, String password, String displayName) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName)
                .setEmailVerified(false);

        return firebaseAuth.createUser(request);
    }

    /**
     * Update an existing Firebase user
     */
    public UserRecord updateUser(String uid, String email, String displayName) throws FirebaseAuthException {
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                .setEmail(email)
                .setDisplayName(displayName);

        return firebaseAuth.updateUser(request);
    }

    /**
     * Delete Firebase user
     */
    public void deleteUser(String uid) throws FirebaseAuthException {
        firebaseAuth.deleteUser(uid);
    }

    /**
     * Set custom claims (roles, etc.)
     */
    public void setCustomClaims(String uid, Map<String, Object> claims) throws FirebaseAuthException {
        firebaseAuth.setCustomUserClaims(uid, claims);
    }

    /**
     * Check if Firebase token is valid
     */
    public boolean isValidToken(String idToken) {
        try {
            verifyIdToken(idToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
