package com.fundizen.fundizen_backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);

    /**
     * Get FirebaseAuth instance, ensuring Firebase is initialized
     */
    private FirebaseAuth getFirebaseAuth() {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase has not been initialized. Check your Firebase configuration.");
        }
        return FirebaseAuth.getInstance();
    }

    /**
     * Verify Firebase ID token and extract user information
     */
    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        try {
            return getFirebaseAuth().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            logger.error("Error verifying Firebase token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get user info from Firebase by UID
     */
    public com.google.firebase.auth.UserRecord getFirebaseUser(String uid) throws FirebaseAuthException {
        try {
            return getFirebaseAuth().getUser(uid);
        } catch (FirebaseAuthException e) {
            logger.error("Error getting Firebase user: {}", e.getMessage());
            throw e;
        }
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