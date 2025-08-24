package com.fundizen.fundizen_backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collation="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder 
public class User {
    
    @Id
    private String id;
    private String uid;  // Firebase UID
    private String email;
    private String username;
    private String role = "user";
    private boolean verified = false;
}
