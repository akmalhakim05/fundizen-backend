package com.fundizen.fundizen_backend.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseService.class);
    private final FirebaseAuth firebaseAuth;

    public FirebaseService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    /**
     * Verify Firebase ID token - simplified version
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

    /**
     * Send email verification to user
     */
    public void sendEmailVerification(String uid) throws FirebaseAuthException {
        try {
            // Generate email verification link
            String link = firebaseAuth.generateEmailVerificationLink(uid);
            logger.info("Email verification link generated for user: {}", uid);
            
            // Note: In a real application, you would send this link via your email service
            // For now, Firebase handles the email sending automatically when user registers
            
        } catch (FirebaseAuthException e) {
            logger.error("Error sending email verification for user {}: {}", uid, e.getMessage());
            throw e;
        }
    }

    /**
     * Get user by UID
     */
    public UserRecord getUserByUid(String uid) throws FirebaseAuthException {
        try {
            return firebaseAuth.getUser(uid);
        } catch (FirebaseAuthException e) {
            logger.error("Error getting user by UID {}: {}", uid, e.getMessage());
            throw e;
        }
    }

    /**
     * Check if user email is verified
     */
    public boolean isEmailVerified(String uid) {
        try {
            UserRecord userRecord = firebaseAuth.getUser(uid);
            return userRecord.isEmailVerified();
        } catch (FirebaseAuthException e) {
            logger.error("Error checking email verification for user {}: {}", uid, e.getMessage());
            return false;
        }
    }

    /**
     * Update user email verification status (admin only)
     */
    public void setEmailVerified(String uid, boolean verified) throws FirebaseAuthException {
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                    .setEmailVerified(verified);
            
            firebaseAuth.updateUser(request);
            logger.info("Updated email verification status for user {} to {}", uid, verified);
            
        } catch (FirebaseAuthException e) {
            logger.error("Error updating email verification for user {}: {}", uid, e.getMessage());
            throw e;
        }
    }

    /**
     * Refresh user token to get latest verification status
     */
    public FirebaseToken refreshToken(String idToken) throws FirebaseAuthException {
        try {
            // Verify the token to ensure it's valid and get fresh claims
            return verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            logger.error("Error refreshing token: {}", e.getMessage());
            throw e;
        }
    }
}