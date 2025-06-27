package com.fundizen.fundizen_backend.repository;

import com.fundizen.fundizen_backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

// Data access layer, letting Spring Boot talk to MongoDB.
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
}
