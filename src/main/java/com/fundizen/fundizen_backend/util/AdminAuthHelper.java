package com.fundizen.fundizen_backend.util;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.UserService;
import com.fundizen.fundizen_backend.service.FirebaseService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AdminAuthHelper {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuthHelper.class);

    @Autowired
    private UserService userService;

    @Autowired
    private FirebaseService firebaseService;

    /**
     * Verify if user is admin based on Firebase token
     */
    public AdminAuthResult verifyAdminAccess(String firebaseToken) {
        try {
            if (firebaseToken == null || firebaseToken.trim().isEmpty()) {
                return AdminAuthResult.failure("Firebase token is required");
            }

            // Verify Firebase token
            FirebaseToken decodedToken = firebaseService.verifyIdToken(firebaseToken);
            
            // Find user in database
            User user = userService.getUserByFirebaseUid(decodedToken.getUid());
            if (user == null) {
                // Try to find by email as fallback
                user = userService.getUserByEmail(decodedToken.getEmail());
            }

            if (user == null) {
                return AdminAuthResult.failure("User not found in system");
            }

            if (!user.isAdmin()) {
                return AdminAuthResult.failure("Access denied: Admin privileges required");
            }

            if (!user.isVerified()) {
                return AdminAuthResult.failure("Account not verified");
            }

            logger.info("Admin access verified for user: {}", user.getUsername());
            return AdminAuthResult.success(user);

        } catch (FirebaseAuthException e) {
            logger.error("Firebase token verification failed: {}", e.getMessage());
            return AdminAuthResult.failure("Invalid Firebase token");
        } catch (Exception e) {
            logger.error("Error verifying admin access", e);
            return AdminAuthResult.failure("Authentication error");
        }
    }

    /**
     * Verify admin access using username/email and password (legacy method)
     */
    public AdminAuthResult verifyAdminAccessLegacy(String usernameOrEmail, String password) {
        try {
            if (usernameOrEmail == null || password == null) {
                return AdminAuthResult.failure("Username/email and password are required");
            }

            User user = userService.authenticateUser(usernameOrEmail, password);

            if (!user.isAdmin()) {
                return AdminAuthResult.failure("Access denied: Admin privileges required");
            }

            logger.info("Admin access verified (legacy) for user: {}", user.getUsername());
            return AdminAuthResult.success(user);

        } catch (RuntimeException e) {
            logger.error("Legacy admin authentication failed: {}", e.getMessage());
            return AdminAuthResult.failure("Invalid credentials");
        } catch (Exception e) {
            logger.error("Error verifying legacy admin access", e);
            return AdminAuthResult.failure("Authentication error");
        }
    }

    /**
     * Check if user has specific admin permissions
     */
    public boolean hasPermission(User user, AdminPermission permission) {
        if (user == null || !user.isAdmin()) {
            return false;
        }

        // For now, all admins have all permissions
        // You can extend this for role-based access control
        switch (permission) {
            case MANAGE_CAMPAIGNS:
            case MANAGE_USERS:
            case VIEW_ANALYTICS:
            case SYSTEM_ADMIN:
                return true;
            default:
                return false;
        }
    }

    /**
     * Get admin info for response
     */
    public Map<String, Object> getAdminInfo(User admin) {
        return Map.of(
            "id", admin.getId(),
            "username", admin.getUsername(),
            "email", admin.getEmail(),
            "role", admin.getRole(),
            "verified", admin.isVerified(),
            "isAdmin", true,
            "permissions", java.util.Arrays.asList(
                "MANAGE_CAMPAIGNS",
                "MANAGE_USERS", 
                "VIEW_ANALYTICS",
                "SYSTEM_ADMIN"
            )
        );
    }

    // Result class for admin authentication
    public static class AdminAuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        private AdminAuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static AdminAuthResult success(User user) {
            return new AdminAuthResult(true, "Authentication successful", user);
        }

        public static AdminAuthResult failure(String message) {
            return new AdminAuthResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    // Admin permissions enum
    public enum AdminPermission {
        MANAGE_CAMPAIGNS,
        MANAGE_USERS,
        VIEW_ANALYTICS,
        SYSTEM_ADMIN
    }
}