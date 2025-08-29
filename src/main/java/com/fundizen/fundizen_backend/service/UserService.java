package com.fundizen.fundizen_backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User findByUid(String uid) {
        Optional<User> user = userRepository.findByUid(uid);
        return user.orElse(null);
    }

    public User findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        return user.orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean isUserVerified(String uid) {
        User user = findByUid(uid);
        return user != null && user.isVerified();
    }
}