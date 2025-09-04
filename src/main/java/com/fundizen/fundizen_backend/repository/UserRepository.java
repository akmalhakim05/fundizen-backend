package com.fundizen.fundizen_backend.repository;

import com.fundizen.fundizen_backend.models.User;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Find by unique identifiers
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUid(String uid); // Firebase UID
    
    // Check existence methods
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUid(String uid);
    
    // Find by username or email (useful for login)
    @Query("{ $or: [ { 'username': ?0 }, { 'email': ?0 } ] }")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    
    // Role-based queries
    List<User> findByRole(String role);
    
    // Email verification queries
    List<User> findByVerifiedTrue();
    List<User> findByVerifiedFalse();
    long countByVerified(boolean verified);
    
    // Pagination support
    Page<User> findAll(Pageable pageable);
    Page<User> findByRole(String role, Pageable pageable);
    Page<User> findByVerified(boolean verified, Pageable pageable);
    
    // Search users by username or email containing text (case-insensitive)
    @Query("{ $or: [ " +
           "{ 'username': { $regex: ?0, $options: 'i' } }, " +
           "{ 'email': { $regex: ?0, $options: 'i' } } ] }")
    List<User> searchUsers(String searchTerm);
    
    // Find users created within a date range
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Count methods
    long countByRole(String role);
    
    // Firebase-specific queries
    @Query("{ 'uid': { $exists: true, $ne: null } }")
    List<User> findFirebaseUsers();
    
    @Query("{ 'uid': { $exists: false } }")
    List<User> findNonFirebaseUsers();
    
    // Advanced queries
    @Query("{ 'verified': true, 'role': ?0 }")
    List<User> findVerifiedUsersByRole(String role);
    
    @Query("{ 'createdAt': { $gte: ?0 }, 'verified': ?1 }")
    List<User> findRecentUsersByVerificationStatus(LocalDateTime since, boolean verified);
    
    // Find users with Firebase UID but not verified (edge case handling)
    @Query("{ 'uid': { $exists: true, $ne: null }, 'verified': false }")
    List<User> findFirebaseUsersNotVerified();
}