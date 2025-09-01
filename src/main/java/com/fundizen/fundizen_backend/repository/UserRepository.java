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
    
    // Check existence methods
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Find by username or email (useful for login)
    @Query("{ $or: [ { 'username': ?0 }, { 'email': ?0 } ] }")
    Optional<User> findByUsernameOrEmail(String usernameOrEmail);
    
    // Status-based queries
    List<User> findByStatus(String status);
    List<User> findByStatusAndDeletedFalse(String status);
    
    // Role-based queries
    List<User> findByRole(String role);
    List<User> findByRoleAndDeletedFalse(String role);
    
    // Find all non-deleted users
    List<User> findByDeletedFalse();
    
    // Active users (not deleted and active status)
    @Query("{ 'deleted': false, 'status': 'active' }")
    List<User> findActiveUsers();
    
    // Pagination support
    Page<User> findByDeletedFalse(Pageable pageable);
    Page<User> findByStatusAndDeletedFalse(String status, Pageable pageable);
    Page<User> findByRoleAndDeletedFalse(String role, Pageable pageable);
    
    // Search users by username or email containing text (case-insensitive)
    @Query("{ 'deleted': false, $or: [ " +
           "{ 'username': { $regex: ?0, $options: 'i' } }, " +
           "{ 'email': { $regex: ?0, $options: 'i' } }, " +
           "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
           "{ 'lastName': { $regex: ?0, $options: 'i' } } ] }")
    List<User> searchUsers(String searchTerm);
    
    // Find users created within a date range
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // Find users who haven't logged in for a certain period
    @Query("{ 'lastLoginAt': { $lt: ?0 }, 'deleted': false }")
    List<User> findInactiveUsersSince(LocalDateTime date);
    
    // Count methods
    long countByDeletedFalse();
    long countByStatusAndDeletedFalse(String status);
    long countByRoleAndDeletedFalse(String role);
}