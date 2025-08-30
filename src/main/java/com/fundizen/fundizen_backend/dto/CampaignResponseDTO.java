package com.fundizen.fundizen_backend.dto;

import com.fundizen.fundizen_backend.models.Campaign;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CampaignResponseDTO {
    private String id;
    private String creatorId;
    private String creatorUsername;
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private Double goalAmount;
    private Double raisedAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private boolean verified;
    private LocalDateTime createdAt;
    private double completionPercentage;
    private long daysRemaining;
    private boolean isActive;
    private boolean canReceiveDonations;

    // Private constructor for builder
    private CampaignResponseDTO(Builder builder) {
        this.id = builder.id;
        this.creatorId = builder.creatorId;
        this.creatorUsername = builder.creatorUsername;
        this.name = builder.name;
        this.category = builder.category;
        this.description = builder.description;
        this.imageUrl = builder.imageUrl;
        this.goalAmount = builder.goalAmount;
        this.raisedAmount = builder.raisedAmount;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.status = builder.status;
        this.verified = builder.verified;
        this.createdAt = builder.createdAt;
        this.completionPercentage = builder.completionPercentage;
        this.daysRemaining = builder.daysRemaining;
        this.isActive = builder.isActive;
        this.canReceiveDonations = builder.canReceiveDonations;
    }

    // Default constructor
    public CampaignResponseDTO() {}

    // Static method to create builder
    public static Builder builder() {
        return new Builder();
    }

    // Builder class
    public static class Builder {
        private String id;
        private String creatorId;
        private String creatorUsername;
        private String name;
        private String category;
        private String description;
        private String imageUrl;
        private Double goalAmount;
        private Double raisedAmount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private boolean verified;
        private LocalDateTime createdAt;
        private double completionPercentage;
        private long daysRemaining;
        private boolean isActive;
        private boolean canReceiveDonations;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder creatorId(String creatorId) {
            this.creatorId = creatorId;
            return this;
        }

        public Builder creatorUsername(String creatorUsername) {
            this.creatorUsername = creatorUsername;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder goalAmount(Double goalAmount) {
            this.goalAmount = goalAmount;
            return this;
        }

        public Builder raisedAmount(Double raisedAmount) {
            this.raisedAmount = raisedAmount;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder verified(boolean verified) {
            this.verified = verified;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder completionPercentage(double completionPercentage) {
            this.completionPercentage = completionPercentage;
            return this;
        }

        public Builder daysRemaining(long daysRemaining) {
            this.daysRemaining = daysRemaining;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder canReceiveDonations(boolean canReceiveDonations) {
            this.canReceiveDonations = canReceiveDonations;
            return this;
        }

        public CampaignResponseDTO build() {
            return new CampaignResponseDTO(this);
        }
    }

    // Helper method to create from Campaign entity
    public static CampaignResponseDTO fromCampaign(Campaign campaign, String creatorUsername) {
        return CampaignResponseDTO.builder()
                .id(campaign.getId())
                .creatorId(campaign.getCreatorId())
                .creatorUsername(creatorUsername)
                .name(campaign.getName())
                .category(campaign.getCategory())
                .description(campaign.getDescription())
                .imageUrl(campaign.getImageUrl())
                .goalAmount(campaign.getGoalAmount())
                .raisedAmount(campaign.getRaisedAmount())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .verified(campaign.isVerified())
                .createdAt(campaign.getCreatedAt())
                .completionPercentage(campaign.getCompletionPercentage())
                .daysRemaining(campaign.getDaysRemaining())
                .isActive(campaign.isActive())
                .canReceiveDonations(campaign.canReceiveDonations())
                .build();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    
    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Double getGoalAmount() { return goalAmount; }
    public void setGoalAmount(Double goalAmount) { this.goalAmount = goalAmount; }
    
    public Double getRaisedAmount() { return raisedAmount; }
    public void setRaisedAmount(Double raisedAmount) { this.raisedAmount = raisedAmount; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
    
    public long getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(long daysRemaining) { this.daysRemaining = daysRemaining; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public boolean isCanReceiveDonations() { return canReceiveDonations; }
    public void setCanReceiveDonations(boolean canReceiveDonations) { this.canReceiveDonations = canReceiveDonations; }
}