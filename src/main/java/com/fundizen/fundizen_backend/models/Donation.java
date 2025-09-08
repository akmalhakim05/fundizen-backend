package com.fundizen.fundizen_backend.models;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.validation.constraints.*;

@Document(collection = "donations")
public class Donation {
    @Id
    private String id;

    @NotNull(message = "Campaign ID is required")
    @Indexed // For efficient campaign queries
    private String campaignId;

    @Indexed // For user donation history
    private String donorId; // Can be null for anonymous donations

    private String donorName; // Display name for the donation

    private String donorEmail; // Contact email (can be different from user email)

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Donation amount must be at least RM 1.00")
    @DecimalMax(value = "100000.0", message = "Donation amount cannot exceed RM 100,000")
    @Digits(integer = 6, fraction = 2, message = "Invalid amount format")
    private Double amount;

    @NotNull(message = "Currency is required")
    @Pattern(regexp = "^(MYR|USD|SGD)$", message = "Currency must be MYR, USD, or SGD")
    private String currency = "MYR";

    // Payment processing fields
    @NotNull(message = "Payment intent ID is required")
    @Indexed // For Stripe webhook lookups
    private String stripePaymentIntentId;

    private String stripePaymentMethodId;

    private String stripeChargeId;

    @Pattern(regexp = "^(pending|processing|succeeded|failed|canceled|refunded)$",
             message = "Status must be one of: pending, processing, succeeded, failed, canceled, refunded")
    @Indexed // For status filtering
    private String paymentStatus = "pending";

    // Donation metadata
    private String message; // Optional message from donor

    private boolean isAnonymous = false;

    private boolean showInPublicList = true; // Whether to show in public donor list

    private boolean receiveUpdates = false; // Whether donor wants campaign updates

    // Timestamps
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt; // When payment was completed

    // Fee information
    private Double stripeFee; // Stripe processing fee

    private Double platformFee; // Platform fee (if any)

    private Double netAmount; // Amount after fees

    // Refund information
    private boolean isRefunded = false;

    private String refundReason;

    private LocalDateTime refundedAt;

    private String refundId;

    // IP tracking for fraud prevention
    private String donorIpAddress;

    private String donorCountry;

    // Constructors
    public Donation() {}

    public Donation(String campaignId, String donorId, Double amount, String stripePaymentIntentId) {
        this.campaignId = campaignId;
        this.donorId = donorId;
        this.amount = amount;
        this.stripePaymentIntentId = stripePaymentIntentId;
        this.currency = "MYR";
        this.paymentStatus = "pending";
        this.isAnonymous = false;
        this.showInPublicList = true;
    }

    // Business logic methods
    public void markAsSucceeded() {
        this.paymentStatus = "succeeded";
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.paymentStatus = "failed";
    }

    public void markAsRefunded(String reason) {
        this.paymentStatus = "refunded";
        this.isRefunded = true;
        this.refundReason = reason;
        this.refundedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return "succeeded".equals(paymentStatus);
    }

    public boolean isPending() {
        return "pending".equals(paymentStatus) || "processing".equals(paymentStatus);
    }

    public boolean canBeRefunded() {
        return isCompleted() && !isRefunded;
    }

    public void calculateNetAmount() {
        if (amount != null) {
            double fees = (stripeFee != null ? stripeFee : 0.0) + (platformFee != null ? platformFee : 0.0);
            this.netAmount = amount - fees;
        }
    }

    // Display methods
    public String getDisplayName() {
        if (isAnonymous) {
            return "Anonymous";
        }
        return donorName != null && !donorName.trim().isEmpty() ? donorName : "Anonymous";
    }

    public String getPublicDisplayAmount() {
        return String.format("RM %.2f", amount);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
        calculateNetAmount();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public String getStripePaymentMethodId() {
        return stripePaymentMethodId;
    }

    public void setStripePaymentMethodId(String stripePaymentMethodId) {
        this.stripePaymentMethodId = stripePaymentMethodId;
    }

    public String getStripeChargeId() {
        return stripeChargeId;
    }

    public void setStripeChargeId(String stripeChargeId) {
        this.stripeChargeId = stripeChargeId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAnonymous() {
        return isAnonymous;
    }

    public void setAnonymous(boolean anonymous) {
        isAnonymous = anonymous;
    }

    public boolean isShowInPublicList() {
        return showInPublicList;
    }

    public void setShowInPublicList(boolean showInPublicList) {
        this.showInPublicList = showInPublicList;
    }

    public boolean isReceiveUpdates() {
        return receiveUpdates;
    }

    public void setReceiveUpdates(boolean receiveUpdates) {
        this.receiveUpdates = receiveUpdates;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Double getStripeFee() {
        return stripeFee;
    }

    public void setStripeFee(Double stripeFee) {
        this.stripeFee = stripeFee;
        calculateNetAmount();
    }

    public Double getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(Double platformFee) {
        this.platformFee = platformFee;
        calculateNetAmount();
    }

    public Double getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Double netAmount) {
        this.netAmount = netAmount;
    }

    public boolean isRefunded() {
        return isRefunded;
    }

    public void setRefunded(boolean refunded) {
        isRefunded = refunded;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public String getRefundId() {
        return refundId;
    }

    public void setRefundId(String refundId) {
        this.refundId = refundId;
    }

    public String getDonorIpAddress() {
        return donorIpAddress;
    }

    public void setDonorIpAddress(String donorIpAddress) {
        this.donorIpAddress = donorIpAddress;
    }

    public String getDonorCountry() {
        return donorCountry;
    }

    public void setDonorCountry(String donorCountry) {
        this.donorCountry = donorCountry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Donation donation = (Donation) o;
        return Objects.equals(id, donation.id) &&
                Objects.equals(stripePaymentIntentId, donation.stripePaymentIntentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stripePaymentIntentId);
    }

    @Override
    public String toString() {
        return "Donation{" +
                "id='" + id + '\'' +
                ", campaignId='" + campaignId + '\'' +
                ", donorId='" + donorId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", isAnonymous=" + isAnonymous +
                ", createdAt=" + createdAt +
                '}';
    }

}
