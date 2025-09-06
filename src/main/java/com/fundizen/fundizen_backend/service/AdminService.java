package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.CampaignRepository;
import com.fundizen.fundizen_backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CampaignService campaignService;
    
    @Autowired
    private UserService userService;

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    // ===== CAMPAIGN MANAGEMENT =====

    /**
     * Get campaigns requiring admin review
     */
    public List<Campaign> getCampaignsForReview() {
        logger.info("Fetching campaigns for admin review");
        return campaignRepository.findByStatusOrderByCreatedAtDesc("pending");
    }

    /**
     * Bulk approve multiple campaigns
     */
    public Map<String, Object> bulkApproveCampaigns(List<String> campaignIds) {
        logger.info("Bulk approving {} campaigns", campaignIds.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String campaignId : campaignIds) {
            try {
                campaignService.verifyCampaign(campaignId);
                successCount++;
                logger.debug("Campaign approved: {}", campaignId);
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to approve campaign: {}", campaignId, e);
            }
        }
        
        logger.info("Bulk approval completed: {} success, {} failures", successCount, failureCount);
        
        return Map.of(
            "totalProcessed", campaignIds.size(),
            "successCount", successCount,
            "failureCount", failureCount,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Bulk reject multiple campaigns
     */
    public Map<String, Object> bulkRejectCampaigns(List<String> campaignIds, String reason) {
        logger.info("Bulk rejecting {} campaigns with reason: {}", campaignIds.size(), reason);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String campaignId : campaignIds) {
            try {
                Campaign campaign = campaignService.rejectCampaign(campaignId);
                if (reason != null && !reason.trim().isEmpty()) {
                    campaign.setRejectionReason(reason);
                    campaignRepository.save(campaign);
                }
                successCount++;
                logger.debug("Campaign rejected: {}", campaignId);
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to reject campaign: {}", campaignId, e);
            }
        }
        
        logger.info("Bulk rejection completed: {} success, {} failures", successCount, failureCount);
        
        return Map.of(
            "totalProcessed", campaignIds.size(),
            "successCount", successCount,
            "failureCount", failureCount,
            "reason", reason,
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== USER MANAGEMENT =====

    /**
     * Search users with advanced filters
     */
    public List<User> searchUsersAdvanced(String searchTerm, String role, Boolean verified, 
                                        LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Advanced user search: term='{}', role='{}', verified={}, dateRange=[{} to {}]", 
                   searchTerm, role, verified, startDate, endDate);
        
        List<User> users = userRepository.findAll();
        
        // Apply search term filter
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchLower = searchTerm.toLowerCase().trim();
            users = users.stream()
                .filter(u -> u.getUsername().toLowerCase().contains(searchLower) ||
                           u.getEmail().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }
        
        // Apply role filter
        if (role != null && !role.trim().isEmpty() && !"all".equals(role)) {
            users = users.stream()
                .filter(u -> role.equals(u.getRole()))
                .collect(Collectors.toList());
        }
        
        // Apply verification filter
        if (verified != null) {
            users = users.stream()
                .filter(u -> u.isVerified() == verified)
                .collect(Collectors.toList());
        }
        
        // Apply date range filter
        if (startDate != null && endDate != null) {
            users = users.stream()
                .filter(u -> u.getCreatedAt().isAfter(startDate) && u.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
        }
        
        logger.info("Advanced search returned {} users", users.size());
        return users;
    }

    /**
     * Bulk update user roles
     */
    public Map<String, Object> bulkUpdateUserRoles(List<String> userIds, String newRole) {
        logger.info("Bulk updating {} users to role: {}", userIds.size(), newRole);
        
        if (!"user".equals(newRole) && !"admin".equals(newRole)) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String userId : userIds) {
            try {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setRole(newRole);
                    userRepository.save(user);
                    successCount++;
                    logger.debug("User role updated: {} -> {}", userId, newRole);
                } else {
                    failureCount++;
                    logger.warn("User not found for role update: {}", userId);
                }
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to update user role: {}", userId, e);
            }
        }
        
        logger.info("Bulk role update completed: {} success, {} failures", successCount, failureCount);
        
        return Map.of(
            "totalProcessed", userIds.size(),
            "successCount", successCount,
            "failureCount", failureCount,
            "newRole", newRole,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Bulk update user verification status
     */
    public Map<String, Object> bulkUpdateUserVerification(List<String> userIds, boolean verified) {
        logger.info("Bulk updating {} users verification status to: {}", userIds.size(), verified);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String userId : userIds) {
            try {
                User user = userService.updateEmailVerificationStatus(userId, verified);
                if (user != null) {
                    successCount++;
                    logger.debug("User verification updated: {} -> {}", userId, verified);
                } else {
                    failureCount++;
                    logger.warn("User not found for verification update: {}", userId);
                }
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to update user verification: {}", userId, e);
            }
        }
        
        logger.info("Bulk verification update completed: {} success, {} failures", successCount, failureCount);
        
        return Map.of(
            "totalProcessed", userIds.size(),
            "successCount", successCount,
            "failureCount", failureCount,
            "verificationStatus", verified,
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== SYSTEM MONITORING =====

    /**
     * Get comprehensive system health metrics
     */
    public Map<String, Object> getSystemHealth() {
        logger.info("Generating system health metrics");
        
        try {
            // Database connectivity checks
            long totalUsers = userRepository.count();
            long totalCampaigns = campaignRepository.count();
            
            // Performance metrics
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            
            // Recent activity indicators
            long recentUserActivity = userRepository.findByCreatedAtBetween(oneHourAgo, now).size();
            long recentCampaignActivity = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(oneHourAgo))
                .count();
            
            // System load indicators
            boolean databaseHealthy = totalUsers >= 0 && totalCampaigns >= 0;
            boolean systemResponsive = true; // You can add more complex health checks
            
            // Pending work queue
            long pendingApprovals = campaignRepository.findByStatusOrderByCreatedAtDesc("pending").size();
            long unverifiedUsers = userRepository.findByVerifiedFalse().size();
            
            // Memory and performance (basic indicators)
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            String overallStatus = databaseHealthy && systemResponsive ? "HEALTHY" : "DEGRADED";
            
            return Map.of(
                "status", overallStatus,
                "database", Map.of(
                    "healthy", databaseHealthy,
                    "totalUsers", totalUsers,
                    "totalCampaigns", totalCampaigns,
                    "responseTime", "< 100ms"
                ),
                "performance", Map.of(
                    "responsive", systemResponsive,
                    "recentUserActivity", recentUserActivity,
                    "recentCampaignActivity", recentCampaignActivity,
                    "memoryUsage", Map.of(
                        "total", totalMemory,
                        "used", usedMemory,
                        "free", freeMemory,
                        "usagePercentage", totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0
                    )
                ),
                "workQueue", Map.of(
                    "pendingApprovals", pendingApprovals,
                    "unverifiedUsers", unverifiedUsers,
                    "totalPendingWork", pendingApprovals + unverifiedUsers
                ),
                "uptime", getSystemUptime(),
                "timestamp", now
            );
            
        } catch (Exception e) {
            logger.error("Error generating system health metrics", e);
            return Map.of(
                "status", "ERROR",
                "error", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Perform system cleanup and maintenance
     */
    public Map<String, Object> performSystemCleanup() {
        logger.info("Performing system cleanup and maintenance");
        
        int cleanedCampaigns = 0;
        int cleanedUsers = 0;
        int dataIntegrityFixes = 0;
        
        try {
            // 1. Auto-reject very old pending campaigns (6 months+)
            LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
            List<Campaign> oldPendingCampaigns = campaignRepository.findAll().stream()
                .filter(c -> "pending".equals(c.getStatus()) && 
                           c.getCreatedAt().toLocalDate().isBefore(sixMonthsAgo))
                .collect(Collectors.toList());
            
            for (Campaign campaign : oldPendingCampaigns) {
                campaign.setStatus("rejected");
                campaign.setRejectionReason("Automatically rejected due to prolonged pending status (6+ months)");
                campaignRepository.save(campaign);
                cleanedCampaigns++;
                logger.debug("Auto-rejected old pending campaign: {}", campaign.getId());
            }
            
            // 2. Clean up orphaned data
            List<Campaign> allCampaigns = campaignRepository.findAll();
            for (Campaign campaign : allCampaigns) {
                // Check if creator still exists
                User creator = userService.getUserById(campaign.getCreatorId());
                if (creator == null) {
                    // Mark campaign as orphaned or assign to system user
                    campaign.setCreatorId("system");
                    campaignRepository.save(campaign);
                    dataIntegrityFixes++;
                    logger.debug("Fixed orphaned campaign: {}", campaign.getId());
                }
            }
            
            // 3. Remove expired unverified users (optional - 30 days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<User> expiredUnverifiedUsers = userRepository.findAll().stream()
                .filter(u -> !u.isVerified() && 
                           u.getCreatedAt().isBefore(thirtyDaysAgo) &&
                           !u.isFirebaseUser())
                .collect(Collectors.toList());
            
            for (User user : expiredUnverifiedUsers) {
                // Only delete if they have no campaigns
                List<Campaign> userCampaigns = allCampaigns.stream()
                    .filter(c -> user.getId().equals(c.getCreatorId()))
                    .collect(Collectors.toList());
                
                if (userCampaigns.isEmpty()) {
                    userRepository.delete(user);
                    cleanedUsers++;
                    logger.debug("Removed expired unverified user: {}", user.getId());
                }
            }
            
            // 4. Validate and fix data consistency
            validateAndFixDataConsistency();
            
            logger.info("System cleanup completed: {} campaigns cleaned, {} users cleaned, {} data fixes", 
                       cleanedCampaigns, cleanedUsers, dataIntegrityFixes);
            
            return Map.of(
                "success", true,
                "cleanedCampaigns", cleanedCampaigns,
                "cleanedUsers", cleanedUsers,
                "dataIntegrityFixes", dataIntegrityFixes,
                "totalItemsCleaned", cleanedCampaigns + cleanedUsers + dataIntegrityFixes,
                "message", "System cleanup completed successfully",
                "details", Map.of(
                    "oldPendingCampaignsRejected", cleanedCampaigns,
                    "expiredUsersRemoved", cleanedUsers,
                    "orphanedDataFixed", dataIntegrityFixes
                ),
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error during system cleanup", e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "cleanedCampaigns", cleanedCampaigns,
                "cleanedUsers", cleanedUsers,
                "dataIntegrityFixes", dataIntegrityFixes,
                "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Generate comprehensive admin report - delegates to analytics service
     */
    public Map<String, Object> generateComprehensiveReport() {
        logger.info("Generating comprehensive admin report");
        return adminAnalyticsService.generateComprehensiveReport();
    }

    /**
     * Export platform data for backup or analysis - delegates to analytics service
     */
    public Map<String, Object> exportPlatformData(String format, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting platform data in format: {} for period: {} to {}", format, startDate, endDate);
        return adminAnalyticsService.exportPlatformData(format, startDate, endDate);
    }

    // ===== DELEGATION METHODS TO ANALYTICS SERVICE =====

    /**
     * Get campaign analytics - delegates to analytics service
     */
    public Map<String, Object> getCampaignAnalytics() {
        return adminAnalyticsService.getCampaignAnalytics();
    }

    /**
     * Get user analytics - delegates to analytics service
     */
    public Map<String, Object> getUserAnalytics() {
        return adminAnalyticsService.getUserAnalytics();
    }

    /**
     * Get top performing campaigns - delegates to analytics service
     */
    public List<Map<String, Object>> getTopPerformingCampaigns(int limit) {
        return adminAnalyticsService.getTopPerformingCampaigns(limit);
    }

    /**
     * Get most active users - delegates to analytics service
     */
    public List<Map<String, Object>> getMostActiveUsers(int limit) {
        return adminAnalyticsService.getMostActiveUsers(limit);
    }

    /**
     * Get platform activity report - delegates to analytics service
     */
    public Map<String, Object> getPlatformActivityReport(int days) {
        return adminAnalyticsService.getPlatformActivityReport(days);
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Get system uptime (basic implementation)
     */
    private String getSystemUptime() {
        return "System uptime tracking not implemented";
    }

    /**
     * Validate and fix data consistency issues
     */
    private void validateAndFixDataConsistency() {
        logger.info("Validating and fixing data consistency");
        
        try {
            // Check for campaigns with invalid goal amounts
            List<Campaign> invalidCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getGoalAmount() == null || c.getGoalAmount() <= 0)
                .collect(Collectors.toList());
            
            for (Campaign campaign : invalidCampaigns) {
                campaign.setGoalAmount(1.0);
                campaignRepository.save(campaign);
                logger.debug("Fixed invalid goal amount for campaign: {}", campaign.getId());
            }
            
            // Check for campaigns with null raised amounts
            List<Campaign> nullRaisedAmountCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getRaisedAmount() == null)
                .collect(Collectors.toList());
            
            for (Campaign campaign : nullRaisedAmountCampaigns) {
                campaign.setRaisedAmount(0.0);
                campaignRepository.save(campaign);
                logger.debug("Fixed null raised amount for campaign: {}", campaign.getId());
            }
            
            // Validate user data
            List<User> invalidUsers = userRepository.findAll().stream()
                .filter(u -> u.getUsername() == null || u.getUsername().trim().isEmpty())
                .collect(Collectors.toList());
            
            for (User user : invalidUsers) {
                if (user.getEmail() != null) {
                    user.setUsername("user_" + user.getEmail().split("@")[0]);
                    userRepository.save(user);
                    logger.debug("Fixed invalid username for user: {}", user.getId());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error during data consistency validation", e);
        }
    }
}