package com.fundizen.fundizen_backend.models;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Document(collection = "campaigns")
@CompoundIndex(name = "status_verified_idx", def = "{'status': 1, 'verified': 1}")
@CompoundIndex(name = "category_verified_idx", def = "{'category': 1, 'verified': 1}")
public class Campaign {

    @Id
    private String id;

    @Indexed // For efficient queries
    private String creatorId;

    @NotNull(message = "Campaign name is required")
    @Size(min = 3, max = 100, message = "Campaign name must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,!?]+$", message = "Campaign name contains invalid characters")
    private String name;

    @NotNull(message = "Category is required")
    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    @Indexed // For filtering by category
    private String category;

    @NotNull(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    // Use Cloudinary to save image
    @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|webp)$", 
             message = "Image URL must be a valid HTTP/HTTPS URL ending with jpg, jpeg, png, gif, or webp",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String imageUrl;

    // Use Cloudinary to save documents for admin review (medical reports, etc.)
    @Pattern(regexp = "^https?://.*\\.(pdf|doc|docx)$", 
             message = "Document URL must be a valid HTTP/HTTPS URL ending with pdf, doc, or docx",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String documentUrl;

    @NotNull(message = "Goal amount is required")
    @DecimalMin(value = "1.0", message = "Goal amount must be at least RM 1.00")
    @DecimalMax(value = "1000000.0", message = "Goal amount cannot exceed RM 1,000,000")
    @Digits(integer = 7, fraction = 2, message = "Invalid amount format")
    private Double goalAmount;

    @DecimalMin(value = "0.0", message = "Raised amount cannot be negative")
    @Digits(integer = 7, fraction = 2, message = "Invalid raised amount format")
    private Double raisedAmount = 0.0;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Pattern(regexp = "^(pending|approved|rejected)$", 
             message = "Status must be one of: pending, approved, rejected")
    @Indexed // For efficient status filtering
    private String status = "pending";

    private boolean verified = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Indexed
    private boolean deleted = false;
    
    private LocalDateTime deletedAt;
    
    private String rejectionReason;

    // Custom validation method
    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return endDate.isAfter(startDate);
    }

    @AssertTrue(message = "Campaign duration must be at least 7 days and at most 365 days")
    public boolean isValidDuration() {
        if (startDate == null || endDate == null) {
            return true;
        }
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        return daysBetween >= 7 && daysBetween <= 365;
    }

    // Constructors
    public Campaign() {}

    public Campaign(String creatorId, String name, String category, String description) {
        this.creatorId = creatorId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.status = "pending";
        this.verified = false;
        this.raisedAmount = 0.0;
    }

    // Helper methods
    public double getCompletionPercentage() {
        if (goalAmount == null || goalAmount == 0) {
            return 0.0;
        }
        return Math.min((raisedAmount / goalAmount) * 100, 100.0);
    }

    public boolean isActive() {
        LocalDate now = LocalDate.now();
        return "approved".equals(status) && 
               verified && 
               startDate != null && 
               endDate != null &&
               !now.isBefore(startDate) && 
               !now.isAfter(endDate);
    }

    public boolean isExpired() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    public long getDaysRemaining() {
        if (endDate == null) return -1;
        LocalDate now = LocalDate.now();
        return now.isBefore(endDate) ? 
               java.time.temporal.ChronoUnit.DAYS.between(now, endDate) : 0;
    }

    // Business logic methods
    public void approve() {
        this.status = "approved";
        this.verified = true;
    }

    public void reject() {
        this.status = "rejected";
        this.verified = false;
    }

    public boolean canReceiveDonations() {
        return isActive() && getCompletionPercentage() < 100.0;
    }

    // Soft delete method
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public Double getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(Double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public Double getRaisedAmount() {
        return raisedAmount;
    }

    public void setRaisedAmount(Double raisedAmount) {
        this.raisedAmount = raisedAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campaign campaign = (Campaign) o;
        return verified == campaign.verified &&
                Objects.equals(id, campaign.id) &&
                Objects.equals(creatorId, campaign.creatorId) &&
                Objects.equals(name, campaign.name) &&
                Objects.equals(category, campaign.category) &&
                Objects.equals(goalAmount, campaign.goalAmount) &&
                Objects.equals(raisedAmount, campaign.raisedAmount) &&
                Objects.equals(status, campaign.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, creatorId, name, category, goalAmount, raisedAmount, status, verified);
    }

    @Override
    public String toString() {
        return "Campaign{" +
                "id='" + id + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", goalAmount=" + goalAmount +
                ", raisedAmount=" + raisedAmount +
                ", status='" + status + '\'' +
                ", verified=" + verified +
                ", completionPercentage=" + getCompletionPercentage() + "%" +
                ", daysRemaining=" + getDaysRemaining() +
                '}';
    }
}