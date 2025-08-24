package com.fundizen.fundizen_backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder 
public class User {
    
    @Id
    private String id; //MongoDB ID
    private String uid;  // Firebase UID
    private String email;
    private String username;

    @Builder.Default
    private String role = "user";

    @Builder.Default
    private boolean verified = false;
}
