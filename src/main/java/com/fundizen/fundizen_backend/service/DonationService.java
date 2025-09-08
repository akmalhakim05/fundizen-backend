package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.Donation;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.DonationRepository;
import com.fundizen.fundizen_backend.repository.CampaignRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

@Service
public class DonationService {

    private static final Logger logger = LoggerFactory.getLogger(DonationService.class);

    @Autowired
    private DonationRepository donationRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private StripeService stripeService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CampaignService campaignService;

    /**
     * Create a new donation and initiate payment
     */
    @Transactional
    public DonationResult createDonation(String campaignId, String donorId, Double amount, 
                                       String donorName, String donorEmail, String donorMessage,
                                       boolean isAnonymous, boolean receiveUpdates, String ipAddress) {
        try {
            logger.info("Creating donation for campaign: {}, amount: {}, donor: {}", 
                       campaignId, amount, donorEmail);

            // Validate campaign
            Campaign campaign = campaignService.getCampaignById(campaignId);
            if (campaign == null) {
                return DonationResult.failure("Campaign not found");
            }

            if (!campaign.canReceiveDonations()) {
                return DonationResult.failure("Campaign is not accepting donations");
            }

            // Validate amount
            if (amount == null || amount < 1.0) {
                return DonationResult.failure("Minimum donation amount is RM 1.00");
            }

            if (amount > 100000.0) {
                return DonationResult.failure("Maximum donation amount is RM 100,000");
            }

            // Fraud detection - check for suspicious activity
            if (isDonationSuspicious(ipAddress, amount, donorEmail)) {
                logger.warn("Suspicious donation detected from IP: {} for amount: {}", ipAddress, amount);
                return DonationResult.failure("Unable to process donation at this time. Please contact support.");
            }

            // Create metadata for Stripe
            Map<String, String> metadata = new HashMap<>();
            metadata.put("campaign_id", campaignId);
            metadata.put("campaign_name", campaign.getName());
            if (donorId != null) {
                metadata.put("donor_id", donorId);
            }
            if (donorName != null) {
                metadata.put("donor_name", donorName);
            }
            metadata.put("is_anonymous", String.valueOf(isAnonymous));

            // Create Stripe Payment Intent
            PaymentIntent paymentIntent = stripeService.createDonationPaymentIntent(
                amount, "MYR", campaignId, donorEmail, metadata
            );

            // Calculate fees
            StripeService.StripeFeesCalculation fees = stripeService.calculateFees(amount, "MYR");

            // Create donation record
            Donation donation = new Donation();
            donation.setCampaignId(campaignId);
            donation.setDonorId(donorId);
            donation.setDonorName(donorName);
            donation.setDonorEmail(donorEmail);
            donation.setAmount(amount);
            donation.setCurrency("MYR");
            donation.setStripePaymentIntentId(paymentIntent.getId());
            donation.setPaymentStatus("pending");
            donation.setMessage(donorMessage);
            donation.setAnonymous(isAnonymous);
            donation.setReceiveUpdates(receiveUpdates);
            donation.setDonorIpAddress(ipAddress);
            donation.setStripeFee(fees.getStripeFee());
            donation.setPlatformFee(fees.getPlatformFee());
            donation.setNetAmount(fees.getNetAmount());

            // Detect country from IP (simplified)
            donation.setDonorCountry(detectCountryFromIP(ipAddress));

            // Save donation
            donation = donationRepository.save(donation);

            logger.info("Donation created successfully: {} for campaign: {}", 
                       donation.getId(), campaignId);

            // Async: Send notification to campaign creator
            notifyCampaignCreatorAsync(campaign, donation);

            return DonationResult.success(donation, paymentIntent.getClientSecret());

        } catch (StripeException e) {
            logger.error("Stripe error creating donation for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            return DonationResult.failure("Payment processing error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error("Error creating donation for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            return DonationResult.failure("Failed to create donation: " + e.getMessage());
        }
    }

    /**
     * Process successful payment (called by webhook)
     */
    @Transactional
    public void processSuccessfulPayment(String paymentIntentId, String chargeId) {
        try {
            logger.info("Processing successful payment for payment intent: {}", paymentIntentId);

            Optional<Donation> donationOpt = donationRepository.findByStripePaymentIntentId(paymentIntentId);
            if (!donationOpt.isPresent()) {
                logger.error("Donation not found for payment intent: {}", paymentIntentId);
                return;
            }

            Donation donation = donationOpt.get();
            
            // Update donation status
            donation.markAsSucceeded();
            donation.setStripeChargeId(chargeId);
            donationRepository.save(donation);

            // Update campaign raised amount
            updateCampaignRaisedAmount(donation.getCampaignId());

            logger.info("Payment processed successfully for donation: {} ({})", 
                       donation.getId(), paymentIntentId);

            // Async notifications
            sendDonationConfirmationAsync(donation);
            sendCampaignUpdateAsync(donation);

        } catch (Exception e) {
            logger.error("Error processing successful payment for intent: {} - Error: {}", 
                        paymentIntentId, e.getMessage(), e);
        }
    }

    /**
     * Process failed payment (called by webhook)
     */
    @Transactional
    public void processFailedPayment(String paymentIntentId, String failureReason) {
        try {
            logger.info("Processing failed payment for payment intent: {} - Reason: {}", 
                       paymentIntentId, failureReason);

            Optional<Donation> donationOpt = donationRepository.findByStripePaymentIntentId(paymentIntentId);
            if (!donationOpt.isPresent()) {
                logger.error("Donation not found for payment intent: {}", paymentIntentId);
                return;
            }

            Donation donation = donationOpt.get();
            donation.markAsFailed();
            donationRepository.save(donation);

            logger.info("Payment marked as failed for donation: {} ({})", 
                       donation.getId(), paymentIntentId);

            // Async: Send failure notification
            sendPaymentFailureNotificationAsync(donation, failureReason);

        } catch (Exception e) {
            logger.error("Error processing failed payment for intent: {} - Error: {}", 
                        paymentIntentId, e.getMessage(), e);
        }
    }

    /**
     * Create refund for a donation
     */
    @Transactional
    public RefundResult createRefund(String donationId, String reason, Double refundAmount) {
        try {
            logger.info("Creating refund for donation: {} - Reason: {}", donationId, reason);

            Optional<Donation> donationOpt = donationRepository.findById(donationId);
            if (!donationOpt.isPresent()) {
                return RefundResult.failure("Donation not found");
            }

            Donation donation = donationOpt.get();

            if (!donation.canBeRefunded()) {
                return RefundResult.failure("Donation cannot be refunded");
            }

            // Validate refund amount
            if (refundAmount != null && refundAmount > donation.getAmount()) {
                return RefundResult.failure("Refund amount cannot exceed donation amount");
            }

            // Create Stripe refund
            String refundReason = mapRefundReason(reason);
            Refund refund = stripeService.createRefund(
                donation.getStripePaymentIntentId(), 
                refundAmount, 
                refundReason
            );

            // Update donation
            donation.markAsRefunded(reason);
            donation.setRefundId(refund.getId());
            donationRepository.save(donation);

            // Update campaign raised amount
            updateCampaignRaisedAmount(donation.getCampaignId());

            logger.info("Refund created successfully: {} for donation: {}", refund.getId(), donationId);

            // Async: Send refund notification
            sendRefundNotificationAsync(donation, refund.getId());

            return RefundResult.success(donation, refund.getId());

        } catch (StripeException e) {
            logger.error("Stripe error creating refund for donation: {} - Error: {}", donationId, e.getMessage(), e);
            return RefundResult.failure("Refund processing error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error("Error creating refund for donation: {} - Error: {}", donationId, e.getMessage(), e);
            return RefundResult.failure("Failed to create refund: " + e.getMessage());
        }
    }

    /**
     * Get donations for a specific campaign
     */
    public List<Donation> getCampaignDonations(String campaignId, boolean includePrivate) {
        try {
            logger.debug("Fetching donations for campaign: {}", campaignId);

            if (includePrivate) {
                return donationRepository.findByCampaignIdAndPaymentStatusOrderByCreatedAtDesc(campaignId, "succeeded");
            } else {
                return donationRepository.findPublicDonationsByCampaignId(campaignId);
            }

        } catch (Exception e) {
            logger.error("Error fetching donations for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get donations by donor
     */
    public List<Donation> getDonorDonations(String donorId) {
        try {
            logger.debug("Fetching donations for donor: {}", donorId);
            return donationRepository.findByDonorIdAndPaymentStatusOrderByCreatedAtDesc(donorId, "succeeded");
        } catch (Exception e) {
            logger.error("Error fetching donations for donor: {} - Error: {}", donorId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get donations with pagination
     */
    public Page<Donation> getDonations(Pageable pageable) {
        try {
            return donationRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("Error fetching donations with pagination - Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get donation statistics for a campaign
     */
    public CampaignDonationStatistics getCampaignDonationStatistics(String campaignId) {
        try {
            logger.debug("Calculating donation statistics for campaign: {}", campaignId);

            List<Donation> successfulDonations = donationRepository.findSuccessfulDonationsByCampaignId(campaignId);

            double totalAmount = successfulDonations.stream()
                .mapToDouble(Donation::getAmount)
                .sum();

            long donationCount = successfulDonations.size();

            double averageAmount = donationCount > 0 ? totalAmount / donationCount : 0.0;

            double largestDonation = successfulDonations.stream()
                .mapToDouble(Donation::getAmount)
                .max()
                .orElse(0.0);

            long uniqueDonors = successfulDonations.stream()
                .map(Donation::getDonorId)
                .filter(donorId -> donorId != null)
                .distinct()
                .count();

            // Additional statistics
            long anonymousDonations = successfulDonations.stream()
                .filter(Donation::isAnonymous)
                .count();

            long donationsWithMessages = successfulDonations.stream()
                .filter(d -> d.getMessage() != null && !d.getMessage().trim().isEmpty())
                .count();

            return new CampaignDonationStatistics(
                totalAmount, donationCount, averageAmount, largestDonation, uniqueDonors,
                anonymousDonations, donationsWithMessages
            );

        } catch (Exception e) {
            logger.error("Error calculating statistics for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            return new CampaignDonationStatistics(0.0, 0L, 0.0, 0.0, 0L, 0L, 0L);
        }
    }

    /**
     * Get platform donation statistics
     */
    public PlatformDonationStatistics getPlatformDonationStatistics() {
        try {
            logger.debug("Calculating platform donation statistics");

            List<Donation> allSuccessfulDonations = donationRepository.findByPaymentStatus("succeeded");

            double totalAmount = allSuccessfulDonations.stream()
                .mapToDouble(Donation::getAmount)
                .sum();

            long totalDonations = allSuccessfulDonations.size();

            long uniqueCampaigns = allSuccessfulDonations.stream()
                .map(Donation::getCampaignId)
                .distinct()
                .count();

            long uniqueDonors = allSuccessfulDonations.stream()
                .map(Donation::getDonorId)
                .filter(donorId -> donorId != null)
                .distinct()
                .count();

            double totalFees = allSuccessfulDonations.stream()
                .mapToDouble(d -> (d.getStripeFee() != null ? d.getStripeFee() : 0.0) + 
                                 (d.getPlatformFee() != null ? d.getPlatformFee() : 0.0))
                .sum();

            // Recent donations (last 30 days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long recentDonations = allSuccessfulDonations.stream()
                .filter(d -> d.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

            return new PlatformDonationStatistics(
                totalAmount, totalDonations, uniqueCampaigns, uniqueDonors, totalFees, recentDonations
            );

        } catch (Exception e) {
            logger.error("Error calculating platform statistics - Error: {}", e.getMessage(), e);
            return new PlatformDonationStatistics(0.0, 0L, 0L, 0L, 0.0, 0L);
        }
    }

    /**
     * Get top donors for a campaign
     */
    public List<TopDonorInfo> getTopDonors(String campaignId, int limit) {
        try {
            logger.debug("Fetching top donors for campaign: {} (limit: {})", campaignId, limit);

            List<DonationRepository.TopDonorProjection> topDonors = 
                donationRepository.findTopDonorsByCampaignId(campaignId, limit);

            return topDonors.stream()
                .map(donor -> {
                    User user = userService.getUserById(donor.getDonorId());
                    String displayName = user != null ? user.getUsername() : "Anonymous";
                    return new TopDonorInfo(
                        donor.getDonorId(),
                        displayName,
                        donor.getTotalAmount(),
                        donor.getDonationCount()
                    );
                })
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching top donors for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get monthly donation trends
     */
    public List<MonthlyDonationTrend> getMonthlyDonationTrends(int months) {
        try {
            LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
            LocalDateTime endDate = LocalDateTime.now();

            List<DonationRepository.MonthlyDonationStats> monthlyStats = 
                donationRepository.getMonthlyDonationStats(startDate, endDate);

            return monthlyStats.stream()
                .map(stat -> new MonthlyDonationTrend(
                    stat.getId().getYear(),
                    stat.getId().getMonth(),
                    stat.getTotalAmount(),
                    stat.getCount()
                ))
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error fetching monthly donation trends - Error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Update campaign raised amount based on successful donations
     */
    private void updateCampaignRaisedAmount(String campaignId) {
        try {
            List<Donation> successfulDonations = donationRepository.findSuccessfulDonationsByCampaignId(campaignId);
            
            double totalRaised = successfulDonations.stream()
                .mapToDouble(Donation::getAmount)
                .sum();

            Campaign campaign = campaignService.getCampaignById(campaignId);
            if (campaign != null) {
                campaign.setRaisedAmount(totalRaised);
                campaignRepository.save(campaign);
                
                logger.debug("Updated raised amount for campaign: {} to {}", campaignId, totalRaised);
            }

        } catch (Exception e) {
            logger.error("Error updating raised amount for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
        }
    }

    /**
     * Map refund reason to Stripe format
     */
    private String mapRefundReason(String reason) {
        if (reason == null) return "REQUESTED_BY_CUSTOMER";
        
        switch (reason.toLowerCase()) {
            case "duplicate":
                return "DUPLICATE";
            case "fraudulent":
                return "FRAUDULENT";
            case "requested_by_customer":
                return "REQUESTED_BY_CUSTOMER";
            default:
                return "REQUESTED_BY_CUSTOMER";
        }
    }

    /**
     * Simple fraud detection
     */
    private boolean isDonationSuspicious(String ipAddress, Double amount, String email) {
        try {
            // Check for too many donations from same IP in short time
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<Donation> recentFromIP = donationRepository.findDonationsByIpAddressSince(ipAddress, oneHourAgo);
            
            if (recentFromIP.size() > 5) {
                return true; // More than 5 donations from same IP in 1 hour
            }

            // Check for unusually large donation
            if (amount > 10000.0) {
                return true; // Donations over RM 10,000 need manual review
            }

            return false;

        } catch (Exception e) {
            logger.error("Error in fraud detection - Error: {}", e.getMessage(), e);
            return false; // Don't block on error
        }
    }

    /**
     * Detect country from IP (simplified)
     */
    private String detectCountryFromIP(String ipAddress) {
        // Simplified country detection
        // In production, you'd use a proper IP geolocation service
        if (ipAddress == null || ipAddress.startsWith("127.0") || ipAddress.startsWith("192.168")) {
            return "MY"; // Default to Malaysia for local/private IPs
        }
        return "MY"; // Default to Malaysia
    }

    /**
     * Clean up old pending donations - runs every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupOldPendingDonations() {
        try {
            logger.info("Starting cleanup of old pending donations");
            
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // 24 hours old
            List<Donation> oldPendingDonations = donationRepository.findOldPendingPayments(cutoff);

            int cleanedCount = 0;
            for (Donation donation : oldPendingDonations) {
                try {
                    // Cancel the Stripe payment intent
                    stripeService.cancelPaymentIntent(donation.getStripePaymentIntentId(), "abandoned");
                    
                    // Mark donation as failed
                    donation.markAsFailed();
                    donationRepository.save(donation);
                    
                    cleanedCount++;
                    logger.debug("Cleaned up old pending donation: {}", donation.getId());
                } catch (Exception e) {
                    logger.warn("Failed to cleanup donation: {} - Error: {}", donation.getId(), e.getMessage());
                }
            }

            if (cleanedCount > 0) {
                logger.info("Cleaned up {} old pending donations", cleanedCount);
            }

        } catch (Exception e) {
            logger.error("Error during cleanup of old pending donations - Error: {}", e.getMessage(), e);
        }
    }

    // Async notification methods
    @Async
    public CompletableFuture<Void> notifyCampaignCreatorAsync(Campaign campaign, Donation donation) {
        try {
            logger.info("Sending notification to campaign creator for new donation");
            // Implementation for sending notification
            // This could be email, SMS, or push notification
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error sending campaign creator notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<Void> sendDonationConfirmationAsync(Donation donation) {
        try {
            logger.info("Sending donation confirmation to donor: {}", donation.getDonorEmail());
            // Implementation for sending confirmation email
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error sending donation confirmation", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<Void> sendCampaignUpdateAsync(Donation donation) {
        try {
            if (donation.isReceiveUpdates()) {
                logger.info("Sending campaign updates to donor: {}", donation.getDonorEmail());
                // Implementation for adding donor to campaign updates list
            }
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error setting up campaign updates", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<Void> sendPaymentFailureNotificationAsync(Donation donation, String reason) {
        try {
            logger.info("Sending payment failure notification to donor: {}", donation.getDonorEmail());
            // Implementation for sending failure notification
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error sending payment failure notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async
    public CompletableFuture<Void> sendRefundNotificationAsync(Donation donation, String refundId) {
        try {
            logger.info("Sending refund notification to donor: {}", donation.getDonorEmail());
            // Implementation for sending refund notification
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error sending refund notification", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    // Result classes
    public static class DonationResult {
        private final boolean success;
        private final String message;
        private final Donation donation;
        private final String clientSecret;

        private DonationResult(boolean success, String message, Donation donation, String clientSecret) {
            this.success = success;
            this.message = message;
            this.donation = donation;
            this.clientSecret = clientSecret;
        }

        public static DonationResult success(Donation donation, String clientSecret) {
            return new DonationResult(true, "Donation created successfully", donation, clientSecret);
        }

        public static DonationResult failure(String message) {
            return new DonationResult(false, message, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Donation getDonation() { return donation; }
        public String getClientSecret() { return clientSecret; }
    }

    public static class RefundResult {
        private final boolean success;
        private final String message;
        private final Donation donation;
        private final String refundId;

        private RefundResult(boolean success, String message, Donation donation, String refundId) {
            this.success = success;
            this.message = message;
            this.donation = donation;
            this.refundId = refundId;
        }

        public static RefundResult success(Donation donation, String refundId) {
            return new RefundResult(true, "Refund processed successfully", donation, refundId);
        }

        public static RefundResult failure(String message) {
            return new RefundResult(false, message, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Donation getDonation() { return donation; }
        public String getRefundId() { return refundId; }
    }

    // Statistics classes
    public static class CampaignDonationStatistics {
        private final Double totalAmount;
        private final Long donationCount;
        private final Double averageAmount;
        private final Double largestDonation;
        private final Long uniqueDonors;
        private final Long anonymousDonations;
        private final Long donationsWithMessages;

        public CampaignDonationStatistics(Double totalAmount, Long donationCount, Double averageAmount, 
                                        Double largestDonation, Long uniqueDonors, Long anonymousDonations,
                                        Long donationsWithMessages) {
            this.totalAmount = totalAmount;
            this.donationCount = donationCount;
            this.averageAmount = averageAmount;
            this.largestDonation = largestDonation;
            this.uniqueDonors = uniqueDonors;
            this.anonymousDonations = anonymousDonations;
            this.donationsWithMessages = donationsWithMessages;
        }

        // Getters
        public Double getTotalAmount() { return totalAmount; }
        public Long getDonationCount() { return donationCount; }
        public Double getAverageAmount() { return averageAmount; }
        public Double getLargestDonation() { return largestDonation; }
        public Long getUniqueDonors() { return uniqueDonors; }
        public Long getAnonymousDonations() { return anonymousDonations; }
        public Long getDonationsWithMessages() { return donationsWithMessages; }
    }

    public static class PlatformDonationStatistics {
        private final Double totalAmount;
        private final Long totalDonations;
        private final Long uniqueCampaigns;
        private final Long uniqueDonors;
        private final Double totalFees;
        private final Long recentDonations;

        public PlatformDonationStatistics(Double totalAmount, Long totalDonations, Long uniqueCampaigns, 
                                        Long uniqueDonors, Double totalFees, Long recentDonations) {
            this.totalAmount = totalAmount;
            this.totalDonations = totalDonations;
            this.uniqueCampaigns = uniqueCampaigns;
            this.uniqueDonors = uniqueDonors;
            this.totalFees = totalFees;
            this.recentDonations = recentDonations;
        }

        // Getters
        public Double getTotalAmount() { return totalAmount; }
        public Long getTotalDonations() { return totalDonations; }
        public Long getUniqueCampaigns() { return uniqueCampaigns; }
        public Long getUniqueDonors() { return uniqueDonors; }
        public Double getTotalFees() { return totalFees; }
        public Long getRecentDonations() { return recentDonations; }
        public Double getNetAmount() { return totalAmount - totalFees; }
    }

    public static class TopDonorInfo {
        private final String donorId;
        private final String displayName;
        private final Double totalAmount;
        private final Long donationCount;

        public TopDonorInfo(String donorId, String displayName, Double totalAmount, Long donationCount) {
            this.donorId = donorId;
            this.displayName = displayName;
            this.totalAmount = totalAmount;
            this.donationCount = donationCount;
        }

        // Getters
        public String getDonorId() { return donorId; }
        public String getDisplayName() { return displayName; }
        public Double getTotalAmount() { return totalAmount; }
        public Long getDonationCount() { return donationCount; }
    }

    public static class MonthlyDonationTrend {
        private final Integer year;
        private final Integer month;
        private final Double totalAmount;
        private final Long donationCount;

        public MonthlyDonationTrend(Integer year, Integer month, Double totalAmount, Long donationCount) {
            this.year = year;
            this.month = month;
            this.totalAmount = totalAmount;
            this.donationCount = donationCount;
        }

        // Getters
        public Integer getYear() { return year; }
        public Integer getMonth() { return month; }
        public Double getTotalAmount() { return totalAmount; }
        public Long getDonationCount() { return donationCount; }
    }
}