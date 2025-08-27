package com.fundizen.fundizen_backend.models;

import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private double raisedAmount = 0.0;
    private LocalDate startDate;
    private LocalDate endDate;
    private String documentUrl;
    private String status = "pending";  // "pending", "approved", "rejected"
    private boolean verified = false;

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

    public double getGoalAmount() {
        return goalAmount;
    }

    public void setGoalAmount(double goalAmount) {
        this.goalAmount = goalAmount;
    }

    public double getRaisedAmount() {
        return raisedAmount;
    }

    public void setRaisedAmount(double raisedAmount) {
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

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Campaign campaign = (Campaign) o;
        return Double.compare(campaign.goalAmount, goalAmount) == 0 &&
                Double.compare(campaign.raisedAmount, raisedAmount) == 0 &&
                verified == campaign.verified &&
                Objects.equals(id, campaign.id) &&
                Objects.equals(creatorId, campaign.creatorId) &&
                Objects.equals(name, campaign.name) &&
                Objects.equals(category, campaign.category) &&
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
                '}';
    }
}