package com.fundizen.fundizen_backend.models;

import java.time.LocalDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "campaigns")
public class Campaign {
    @Id
    private String id;
    private String creatorId;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private double goalAmount;
    private double raisedAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String documentUrl;
    private String status;  // "pending", "approved", "rejected"
    private boolean verified;  
}
