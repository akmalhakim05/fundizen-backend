package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.FirebaseService;
import com.fundizen.fundizen_backend.service.UserService;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private UserService userService;

    /**
     * Firebase authentication endpoint
     * Frontend sends Firebase ID token, backend verifies it
     */
    @PostMapping("/firebase/verify")
    public ResponseEntity<?> verifyFirebaseToken(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Firebase ID token is required"));
            }

            // Verify Firebase token
            FirebaseToken decodedToken = firebaseService.verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            // Check if user exists in our database
            User user = userService.getUserByEmail(email);
            
            if (user == null) {
                // Create new user if doesn't exist
                user = new User();
                user.setEmail(email);
                user.setUsername(email.split("@")[0]); // Use email prefix as username
                user.setPassword(""); // No password needed for Firebase users
                user.setRole("user");
                
                try {
                    user = userService.createUserFromFirebase(user, uid);
                } catch (Exception e) {
                    // Handle username conflict by appending UID
                    user.setUsername(email.split("@")[0] + "_" + uid.substring(0, 6));
                    user = userService.createUserFromFirebase(user, uid);
                }
            }

            // Return user info
            Map<String, Object> userResponse = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "firebaseUid", uid
            );

            return ResponseEntity.ok(Map.of(
                "message", "Firebase authentication successful",
                "user", userResponse,
                "token", idToken // Return the same token for frontend use
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid Firebase token: " + e.getMessage()));
        }
    }

    /**
     * Get current user info from Firebase token
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            String firebaseUid = (String) request.getAttribute("firebaseUid");
            String firebaseEmail = (String) request.getAttribute("firebaseEmail");

            if (firebaseUid == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            // Get user from database
            User user = userService.getUserByEmail(firebaseEmail);
            
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            // Remove password and return user info
            user.setPassword(null);
            return ResponseEntity.ok(Map.of(
                "user", user,
                "firebaseUid", firebaseUid
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error getting user info: " + e.getMessage()));
        }
    }

    /**
     * Logout endpoint (mainly for clearing any server-side sessions if needed)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // With Firebase, logout is mainly handled on frontend
        // This endpoint can be used for any server-side cleanup
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}