package com.fundizen.fundizen_backend.models;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String firebaseUid;  // Firebase UID for linking

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed(unique = true)
    private String email;

    private String username;

    private String role = "user";   // default role

    private boolean verified = false; // email verified status

    // Constructors
    public User() {}

    public User(String id, String firebaseUid, LocalDateTime createdAt, LocalDateTime updatedAt,
                String email, String username, String role, boolean verified) {
        this.id = id;
        this.firebaseUid = firebaseUid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.email = email;
        this.username = username;
        this.role = role;
        this.verified = verified;
    }

    public User(String email, String username) {
        this.email = email;
        this.username = username;
        this.role = "user";
        this.verified = false;
    }

    // Builder pattern
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String id;
        private String firebaseUid;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String email;
        private String username;
        private String role = "user";
        private boolean verified = false;

        public UserBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserBuilder firebaseUid(String firebaseUid) {
            this.firebaseUid = firebaseUid;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder role(String role) {
            this.role = role;
            return this;
        }

        public UserBuilder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public User build() {
            return new User(id, firebaseUid, createdAt, updatedAt, email, username, role, verified);
        }
    }

    // Getters & Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return verified == user.verified &&
                Objects.equals(id, user.id) &&
                Objects.equals(firebaseUid, user.firebaseUid) &&
                Objects.equals(email, user.email) &&
                Objects.equals(username, user.username) &&
                Objects.equals(role, user.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firebaseUid, email, username, role, verified);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", firebaseUid='" + firebaseUid + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", verified=" + verified +
                '}';
    }
}
