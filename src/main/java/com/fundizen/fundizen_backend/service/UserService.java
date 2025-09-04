package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    // User creation and registration
    public User createUser(User user) {
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }
        
        // Set default role and verification status
        user.setRole("user");
        user.setVerified(false); // Email not verified for regular registration
        
        // Hash the password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }

    // User authentication methods
    public User authenticateUser(String usernameOrEmail, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Check hashed password
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        
        throw new RuntimeException("Invalid credentials");
    }

    /**
     * Create user from Firebase authentication (automatically verified)
     */
    public User createUserFromFirebase(User user, String firebaseUid) {
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists: " + user.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }
        
        // Set default role and verify immediately for Firebase users
        user.setRole("user");
        user.setVerified(true); // Email verified through Firebase
        user.setUid(firebaseUid);
        
        // Hash the password
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            // For Firebase-only users, set a random password they'll never use
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        }
        
        return userRepository.save(user);
    }

    /**
     * Update email verification status
     */
    public User updateEmailVerificationStatus(String userId, boolean verified) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setVerified(verified);
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found");
    }

    /**
     * Link existing user account with Firebase UID
     */
    public User linkFirebaseAccount(String userId, String firebaseUid) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUid(firebaseUid);
            // Also mark as verified since Firebase handles email verification
            user.setVerified(true);
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found");
    }

    /**
     * Get user by Firebase UID
     */
    public User getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findByUid(firebaseUid).orElse(null);
    }

    /**
     * Find user by Firebase UID or create if doesn't exist
     */
    public User findOrCreateFromFirebase(String firebaseUid, String email, String username) {
        // First try to find by Firebase UID
        Optional<User> existingUser = userRepository.findByUid(firebaseUid);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Try to find by email
        existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            // Link the Firebase UID
            User user = existingUser.get();
            user.setUid(firebaseUid);
            user.setVerified(true); // Mark as verified
            return userRepository.save(user);
        }

        // Create new user
        User newUser = new User();
        newUser.setUid(firebaseUid);
        newUser.setEmail(email);
        newUser.setUsername(username != null ? username : email.split("@")[0]);
        newUser.setRole("user");
        newUser.setVerified(true);
        // Set random password since Firebase handles authentication
        newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        return userRepository.save(newUser);
    }

    // CRUD operations
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // Role management
    public List<User> getUsersByRole(String role) {
        return userRepository.findByRole(role);
    }

    public List<User> getAdminUsers() {
        return getUsersByRole("admin");
    }

    // Search functionality
    public List<User> searchUsers(String searchTerm) {
        return userRepository.searchUsers(searchTerm);
    }

    // User updates
    public User updateUser(String id, User updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);
        
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            
            // Check if username is being changed and if new username already exists
            if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new RuntimeException("Username already exists: " + updatedUser.getUsername());
            }
            
            // Check if email is being changed and if new email already exists
            if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new RuntimeException("Email already exists: " + updatedUser.getEmail());
            }
            
            // Update fields (excluding password and system fields)
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            
            return userRepository.save(existingUser);
        }
        
        return null;
    }

    public User updateUserPassword(String id, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Hash the new password
            user.setPassword(passwordEncoder.encode(newPassword));
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found");
    }

    // Role management
    public User promoteToAdmin(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.promoteToAdmin();
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found");
    }

    public User demoteToUser(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.demoteToUser();
            return userRepository.save(user);
        }
        
        throw new RuntimeException("User not found");
    }

    // Delete operations
    public boolean deleteUser(String id) {
        Optional<User> userOpt = userRepository.findById(id);
        
        if (userOpt.isPresent()) {
            userRepository.deleteById(id);
            return true;
        }
        
        return false;
    }

    // Validation methods
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // Statistics and analytics
    public long getTotalUsers() {
        return userRepository.count();
    }

    public long getTotalAdmins() {
        return userRepository.countByRole("admin");
    }

    public List<User> getRecentUsers(LocalDateTime since) {
        return userRepository.findByCreatedAtBetween(since, LocalDateTime.now());
    }

    // Email verification specific methods
    public List<User> getUnverifiedUsers() {
        return userRepository.findByVerifiedFalse();
    }

    public List<User> getVerifiedUsers() {
        return userRepository.findByVerifiedTrue();
    }

    public long getVerifiedUsersCount() {
        return userRepository.countByVerified(true);
    }

    public long getUnverifiedUsersCount() {
        return userRepository.countByVerified(false);
    }
}