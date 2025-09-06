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
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    // ===== CAMPAIGN ANALYTICS =====

    /**
     * Get campaign analytics for admin dashboard
     */
    public Map<String, Object> getCampaignAnalytics() {
        logger.info("Generating campaign analytics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        LocalDate now = LocalDate.now();
        
        // Status distribution
        Map<String, Long> statusDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        // Category distribution
        Map<String, Long> categoryDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        // Active vs Expired
        long activeCampaigns = allCampaigns.stream()
            .filter(Campaign::isActive)
            .count();
        
        long expiredCampaigns = allCampaigns.stream()
            .filter(Campaign::isExpired)
            .count();
        
        // Financial metrics
        double totalGoalAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getGoalAmount)
            .sum();
        
        double totalRaisedAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .sum();
        
        // Success rate
        long successfulCampaigns = allCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .count();
        
        double successRate = allCampaigns.size() > 0 ? 
            (double) successfulCampaigns / allCampaigns.size() * 100 : 0;
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        return Map.of(
            "statusDistribution", statusDistribution,
            "categoryDistribution", categoryDistribution,
            "activityMetrics", Map.of(
                "active", activeCampaigns,
                "expired", expiredCampaigns,
                "recentlyCreated", recentCampaigns
            ),
            "financialMetrics", Map.of(
                "totalGoalAmount", totalGoalAmount,
                "totalRaisedAmount", totalRaisedAmount,
                "averageGoalAmount", allCampaigns.size() > 0 ? totalGoalAmount / allCampaigns.size() : 0,
                "averageRaisedAmount", allCampaigns.size() > 0 ? totalRaisedAmount / allCampaigns.size() : 0,
                "fundingEfficiency", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0
            ),
            "performanceMetrics", Map.of(
                "successfulCampaigns", successfulCampaigns,
                "successRate", successRate,
                "totalCampaigns", allCampaigns.size()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get detailed campaign statistics by date range
     */
    public Map<String, Object> getCampaignStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating campaign stats for date range: {} to {}", startDate, endDate);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate) && c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Daily breakdown
        Map<LocalDate, Long> dailyCampaignCount = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Category performance
        Map<String, Map<String, Object>> categoryPerformance = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    double totalGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double totalRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "count", categoryCampaigns.size(),
                        "totalGoal", totalGoal,
                        "totalRaised", totalRaised,
                        "efficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
                    );
                }
            ));
        
        return Map.of(
            "period", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalCampaigns", filteredCampaigns.size()
            ),
            "dailyBreakdown", dailyCampaignCount,
            "categoryPerformance", categoryPerformance,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get top performing campaigns
     */
    public List<Map<String, Object>> getTopPerformingCampaigns(int limit) {
        logger.info("Fetching top {} performing campaigns", limit);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        return allCampaigns.stream()
            .filter(c -> c.getGoalAmount() > 0)
            .sorted((a, b) -> Double.compare(b.getCompletionPercentage(), a.getCompletionPercentage()))
            .limit(limit)
            .map(campaign -> {
                User creator = userService.getUserById(campaign.getCreatorId());
                return Map.of(
                    "id", campaign.getId(),
                    "name", campaign.getName(),
                    "category", campaign.getCategory(),
                    "goalAmount", campaign.getGoalAmount(),
                    "raisedAmount", campaign.getRaisedAmount(),
                    "completionPercentage", campaign.getCompletionPercentage(),
                    "status", campaign.getStatus(),
                    "creatorUsername", creator != null ? creator.getUsername() : "Unknown",
                    "createdAt", campaign.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Get advanced campaign statistics with filters
     */
    public Map<String, Object> getAdvancedCampaignStats(String category, String status, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating advanced campaign statistics with filters");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Apply filters
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> category == null || category.equals("all") || category.equals(c.getCategory()))
            .filter(c -> status == null || status.equals("all") || status.equals(c.getStatus()))
            .filter(c -> startDate == null || c.getCreatedAt().isAfter(startDate))
            .filter(c -> endDate == null || c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Calculate detailed statistics
        double totalGoal = filteredCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaised = filteredCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        Map<String, Long> statusBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        Map<String, Long> categoryBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        List<Campaign> successfulCampaigns = filteredCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .collect(Collectors.toList());
        
        return Map.of(
            "filters", Map.of(
                "category", category != null ? category : "all",
                "status", status != null ? status : "all",
                "startDate", startDate,
                "endDate", endDate
            ),
            "summary", Map.of(
                "totalCampaigns", filteredCampaigns.size(),
                "totalGoalAmount", totalGoal,
                "totalRaisedAmount", totalRaised,
                "averageGoalAmount", filteredCampaigns.size() > 0 ? totalGoal / filteredCampaigns.size() : 0,
                "averageRaisedAmount", filteredCampaigns.size() > 0 ? totalRaised / filteredCampaigns.size() : 0,
                "successfulCampaigns", successfulCampaigns.size(),
                "successRate", filteredCampaigns.size() > 0 ? 
                    (double) successfulCampaigns.size() / filteredCampaigns.size() * 100 : 0,
                "fundingEfficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
            ),
            "breakdowns", Map.of(
                "byStatus", statusBreakdown,
                "byCategory", categoryBreakdown
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get campaign performance trends
     */
public Map<String, Object> getCampaignPerformanceTrends(int days) {
        logger.info("Generating campaign performance trends for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Campaign> recentCampaigns = campaignRepository.findAll().stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        // Daily campaign creation trend
        Map<LocalDate, Long> dailyCreations = recentCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Daily approval trend
        Map<LocalDate, Long> dailyApprovals = recentCampaigns.stream()
            .filter(c -> "approved".equals(c.getStatus()))
            .collect(Collectors.groupingBy(
                c -> c.getUpdatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Success rate trend
        Map<LocalDate, Double> dailySuccessRates = recentCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> dayCampaigns = entry.getValue();
                    long successful = dayCampaigns.stream()
                        .filter(c -> c.getCompletionPercentage() >= 100.0)
                        .count();
                    return dayCampaigns.size() > 0 ? (double) successful / dayCampaigns.size() * 100 : 0;
                }
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "totalCampaigns", recentCampaigns.size()
            ),
            "trends", Map.of(
                "dailyCreations", dailyCreations,
                "dailyApprovals", dailyApprovals,
                "dailySuccessRates", dailySuccessRates
            ),
            "summary", Map.of(
                "averageDailyCreations", days > 0 ? (double) recentCampaigns.size() / days : 0,
                "totalGoalAmount", recentCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum(),
                "totalRaisedAmount", recentCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== USER ANALYTICS =====

    /**
     * Get user analytics for admin dashboard
     */
    public Map<String, Object> getUserAnalytics() {
        logger.info("Generating user analytics");
        
        List<User> allUsers = userRepository.findAll();
        
        // Role distribution
        Map<String, Long> roleDistribution = allUsers.stream()
            .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        
        // Verification status
        long verifiedUsers = allUsers.stream()
            .filter(User::isVerified)
            .count();
        
        long unverifiedUsers = allUsers.size() - verifiedUsers;
        
        // Firebase vs Local users
        long firebaseUsers = allUsers.stream()
            .filter(User::isFirebaseUser)
            .count();
        
        long localUsers = allUsers.size() - firebaseUsers;
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentSignups = allUsers.stream()
            .filter(u -> u.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        return Map.of(
            "roleDistribution", roleDistribution,
            "verificationMetrics", Map.of(
                "verified", verifiedUsers,
                "unverified", unverifiedUsers,
                "verificationRate", allUsers.size() > 0 ? (double) verifiedUsers / allUsers.size() * 100 : 0
            ),
            "authenticationMetrics", Map.of(
                "firebaseUsers", firebaseUsers,
                "localUsers", localUsers,
                "firebaseAdoptionRate", allUsers.size() > 0 ? (double) firebaseUsers / allUsers.size() * 100 : 0
            ),
            "activityMetrics", Map.of(
                "totalUsers", allUsers.size(),
                "recentSignups", recentSignups,
                "activeUsers", allUsers.size()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get user registration trends
     */
    public Map<String, Object> getUserRegistrationTrends(int days) {
        logger.info("Generating user registration trends for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<User> recentUsers = userRepository.findByCreatedAtBetween(startDate, LocalDateTime.now());
        
        // Daily registration count
        Map<LocalDate, Long> dailyRegistrations = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Registration by verification status
        Map<String, Long> verificationBreakdown = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.isVerified() ? "verified" : "unverified",
                Collectors.counting()
            ));
        
        // Registration by authentication method
        Map<String, Long> authMethodBreakdown = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.isFirebaseUser() ? "firebase" : "local",
                Collectors.counting()
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "totalNewUsers", recentUsers.size()
            ),
            "dailyRegistrations", dailyRegistrations,
            "verificationBreakdown", verificationBreakdown,
            "authMethodBreakdown", authMethodBreakdown,
            "averageDaily", days > 0 ? (double) recentUsers.size() / days : 0,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get most active users (by campaign creation)
     */
    public List<Map<String, Object>> getMostActiveUsers(int limit) {
        logger.info("Fetching top {} most active users", limit);
        
        List<User> allUsers = userRepository.findAll();
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Count campaigns per user
        Map<String, Long> campaignCounts = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCreatorId, Collectors.counting()));
        
        return allUsers.stream()
            .map(user -> {
                long campaignCount = campaignCounts.getOrDefault(user.getId(), 0L);
                return Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "verified", user.isVerified(),
                    "campaignCount", campaignCount,
                    "createdAt", user.getCreatedAt()
                );
            })
            .filter(userMap -> (Long) userMap.get("campaignCount") > 0)
            .sorted((a, b) -> Long.compare((Long) b.get("campaignCount"), (Long) a.get("campaignCount")))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get user engagement metrics
     */
    public Map<String, Object> getUserEngagementMetrics() {
        logger.info("Generating user engagement metrics");
        
        List<User> allUsers = userRepository.findAll();
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Calculate engagement metrics
        Map<String, Long> userCampaignCounts = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCreatorId, Collectors.counting()));
        
        long activeCreators = userCampaignCounts.size();
        long passiveUsers = allUsers.size() - activeCreators;
        
        double averageCampaignsPerUser = allUsers.size() > 0 ? 
            (double) allCampaigns.size() / allUsers.size() : 0;
        
        double averageCampaignsPerActiveUser = activeCreators > 0 ? 
            (double) allCampaigns.size() / activeCreators : 0;
        
        // User distribution by campaign count
        Map<String, Long> userDistribution = Map.of(
            "0_campaigns", (long) passiveUsers,
            "1_campaign", userCampaignCounts.values().stream().filter(count -> count == 1).count(),
            "2-5_campaigns", userCampaignCounts.values().stream().filter(count -> count >= 2 && count <= 5).count(),
            "6+_campaigns", userCampaignCounts.values().stream().filter(count -> count > 5).count()
        );
        
        return Map.of(
            "overview", Map.of(
                "totalUsers", allUsers.size(),
                "activeCreators", activeCreators,
                "passiveUsers", passiveUsers,
                "engagementRate", allUsers.size() > 0 ? (double) activeCreators / allUsers.size() * 100 : 0
            ),
            "campaignMetrics", Map.of(
                "totalCampaigns", allCampaigns.size(),
                "averageCampaignsPerUser", averageCampaignsPerUser,
                "averageCampaignsPerActiveUser", averageCampaignsPerActiveUser
            ),
            "userDistribution", userDistribution,
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== FINANCIAL ANALYTICS =====

    /**
     * Get financial analytics and metrics
     */
    public Map<String, Object> getFinancialAnalytics() {
        logger.info("Generating financial analytics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Overall financial metrics
        double totalGoalAmount = allCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaisedAmount = allCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        // Category-wise financial breakdown
        Map<String, Map<String, Double>> categoryFinancials = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    double categoryGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double categoryRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "totalGoal", categoryGoal,
                        "totalRaised", categoryRaised,
                        "efficiency", categoryGoal > 0 ? (categoryRaised / categoryGoal) * 100 : 0,
                        "campaignCount", (double) categoryCampaigns.size()
                    );
                }
            ));
        
        // Monthly financial trends (last 12 months)
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        Map<String, Map<String, Double>> monthlyTrends = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(twelveMonthsAgo))
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().getYear() + "-" + String.format("%02d", c.getCreatedAt().getMonthValue())
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> monthCampaigns = entry.getValue();
                    double monthGoal = monthCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double monthRaised = monthCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "goal", monthGoal,
                        "raised", monthRaised,
                        "campaignCount", (double) monthCampaigns.size()
                    );
                }
            ));
        
        // Success metrics
        List<Campaign> successfulCampaigns = allCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .collect(Collectors.toList());
        
        double averageSuccessfulAmount = successfulCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .average()
            .orElse(0.0);
        
        return Map.of(
            "overview", Map.of(
                "totalGoalAmount", totalGoalAmount,
                "totalRaisedAmount", totalRaisedAmount,
                "overallEfficiency", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0,
                "averageGoalPerCampaign", allCampaigns.size() > 0 ? totalGoalAmount / allCampaigns.size() : 0,
                "averageRaisedPerCampaign", allCampaigns.size() > 0 ? totalRaisedAmount / allCampaigns.size() : 0
            ),
            "categoryBreakdown", categoryFinancials,
            "monthlyTrends", monthlyTrends,
            "successMetrics", Map.of(
                "successfulCampaigns", successfulCampaigns.size(),
                "successRate", allCampaigns.size() > 0 ? (double) successfulCampaigns.size() / allCampaigns.size() * 100 : 0,
                "averageSuccessfulAmount", averageSuccessfulAmount,
                "totalSuccessfulAmount", successfulCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== PLATFORM ACTIVITY & REPORTS =====

    /**
     * Get platform activity report
     */
    public Map<String, Object> getPlatformActivityReport(int days) {
        logger.info("Generating platform activity report for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        
        // User activity
        List<User> newUsers = userRepository.findByCreatedAtBetween(startDate, endDate);
        
        // Campaign activity
        List<Campaign> allCampaigns = campaignRepository.findAll();
        List<Campaign> newCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        List<Campaign> approvedCampaigns = allCampaigns.stream()
            .filter(c -> c.getUpdatedAt().isAfter(startDate) && "approved".equals(c.getStatus()))
            .collect(Collectors.toList());
        
        List<Campaign> rejectedCampaigns = allCampaigns.stream()
            .filter(c -> c.getUpdatedAt().isAfter(startDate) && "rejected".equals(c.getStatus()))
            .collect(Collectors.toList());
        
        // Financial activity
        double newFundingGoals = newCampaigns.stream()
            .mapToDouble(Campaign::getGoalAmount)
            .sum();
        
        double newFundsRaised = newCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .sum();
        
        // Daily breakdown
        Map<LocalDate, Long> dailyUserSignups = newUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        Map<LocalDate, Long> dailyCampaignCreations = newCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "endDate", endDate
            ),
            "userActivity", Map.of(
                "newUsers", newUsers.size(),
                "dailySignups", dailyUserSignups,
                "newUserDetails", newUsers.stream()
                    .map(u -> Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail(),
                        "verified", u.isVerified(),
                        "firebaseUser", u.isFirebaseUser(),
                        "createdAt", u.getCreatedAt()
                    ))
                    .collect(Collectors.toList())
            ),
            "campaignActivity", Map.of(
                "newCampaigns", newCampaigns.size(),
                "approvedCampaigns", approvedCampaigns.size(),
                "rejectedCampaigns", rejectedCampaigns.size(),
                "dailyCreations", dailyCampaignCreations,
                "newFundingGoals", newFundingGoals,
                "newFundsRaised", newFundsRaised,
                "approvalRate", newCampaigns.size() > 0 ? 
                    (double) approvedCampaigns.size() / newCampaigns.size() * 100 : 0
            ),
            "summary", Map.of(
                "totalNewItems", newUsers.size() + newCampaigns.size(),
                "avgDailySignups", days > 0 ? (double) newUsers.size() / days : 0,
                "avgDailyCampaigns", days > 0 ? (double) newCampaigns.size() / days : 0,
                "platformGrowthRate", calculateGrowthRate(days)
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Generate comprehensive admin report
     */
    public Map<String, Object> generateComprehensiveReport() {
        logger.info("Generating comprehensive admin report");
        
        try {
            // Get all analytics data
            Map<String, Object> campaignAnalytics = getCampaignAnalytics();
            Map<String, Object> userAnalytics = getUserAnalytics();
            Map<String, Object> financialAnalytics = getFinancialAnalytics();
            
            // Get recent activity (last 7 days)
            Map<String, Object> recentActivity = getPlatformActivityReport(7);
            
            // Get top performers
            List<Map<String, Object>> topCampaigns = getTopPerformingCampaigns(10);
            List<Map<String, Object>> topUsers = getMostActiveUsers(10);
            
            // Financial summary
            List<Campaign> allCampaigns = campaignRepository.findAll();
            double totalGoalsAllTime = allCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
            double totalRaisedAllTime = allCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
            
            // Platform metrics
            long totalUsers = userRepository.count();
            long totalCampaigns = campaignRepository.count();
            long activeCampaigns = allCampaigns.stream().filter(Campaign::isActive).count();
            
            return Map.of(
                "reportMetadata", Map.of(
                    "generatedAt", LocalDateTime.now(),
                    "reportType", "COMPREHENSIVE_ADMIN_REPORT",
                    "version", "1.0"
                ),
                "executiveSummary", Map.of(
                    "totalUsers", totalUsers,
                    "totalCampaigns", totalCampaigns,
                    "activeCampaigns", activeCampaigns,
                    "totalGoalsAllTime", totalGoalsAllTime,
                    "totalRaisedAllTime", totalRaisedAllTime,
                    "platformSuccessRate", totalGoalsAllTime > 0 ? (totalRaisedAllTime / totalGoalsAllTime) * 100 : 0
                ),
                "campaignAnalytics", campaignAnalytics,
                "userAnalytics", userAnalytics,
                "financialAnalytics", financialAnalytics,
                "recentActivity", recentActivity,
                "topPerformers", Map.of(
                    "campaigns", topCampaigns,
                    "users", topUsers
                ),
                "recommendations", generateRecommendations(),
                "alerts", generateSystemAlerts()
            );
            
        } catch (Exception e) {
            logger.error("Error generating comprehensive report", e);
            return Map.of(
                "error", "Failed to generate comprehensive report",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Export platform data for backup or analysis
     */
    public Map<String, Object> exportPlatformData(String format, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting platform data in format: {} for period: {} to {}", format, startDate, endDate);
        
        try {
            // Filter data by date range
            List<User> users = userRepository.findByCreatedAtBetween(startDate, endDate);
            List<Campaign> campaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(startDate) && c.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
            
            // Prepare export data
            List<Map<String, Object>> exportUsers = users.stream()
                .map(user -> Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "verified", user.isVerified(),
                    "firebaseUser", user.isFirebaseUser(),
                    "createdAt", user.getCreatedAt(),
                    "updatedAt", user.getUpdatedAt()
                ))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> exportCampaigns = campaigns.stream()
                .map(campaign -> {
                    User creator = userService.getUserById(campaign.getCreatorId());
                    return Map.of(
                        "id", campaign.getId(),
                        "name", campaign.getName(),
                        "category", campaign.getCategory(),
                        "description", campaign.getDescription(),
                        "goalAmount", campaign.getGoalAmount(),
                        "raisedAmount", campaign.getRaisedAmount(),
                        "status", campaign.getStatus(),
                        "verified", campaign.isVerified(),
                        "creatorId", campaign.getCreatorId(),
                        "creatorUsername", creator != null ? creator.getUsername() : "Unknown",
                        "startDate", campaign.getStartDate(),
                        "endDate", campaign.getEndDate(),
                        "createdAt", campaign.getCreatedAt(),
                        "updatedAt", campaign.getUpdatedAt(),
                        "completionPercentage", campaign.getCompletionPercentage(),
                        "isActive", campaign.isActive()
                    );
                })
                .collect(Collectors.toList());
            
            return Map.of(
                "exportMetadata", Map.of(
                    "format", format,
                    "startDate", startDate,
                    "endDate", endDate,
                    "exportedAt", LocalDateTime.now(),
                    "userCount", exportUsers.size(),
                    "campaignCount", exportCampaigns.size()
                ),
                "users", exportUsers,
                "campaigns", exportCampaigns,
                "summary", Map.of(
                    "totalRecords", exportUsers.size() + exportCampaigns.size(),
                    "dateRange", Map.of(
                        "start", startDate,
                        "end", endDate
                    )
                )
            );
            
        } catch (Exception e) {
            logger.error("Error exporting platform data", e);
            return Map.of(
                "error", "Failed to export platform data",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Calculate platform growth rate
     */
    private double calculateGrowthRate(int days) {
        try {
            LocalDateTime periodStart = LocalDateTime.now().minusDays(days);
            LocalDateTime previousPeriodStart = LocalDateTime.now().minusDays(days * 2);
            
            // Current period counts
            long currentUsers = userRepository.findByCreatedAtBetween(periodStart, LocalDateTime.now()).size();
            long currentCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(periodStart))
                .count();
            
            // Previous period counts
            long previousUsers = userRepository.findByCreatedAtBetween(previousPeriodStart, periodStart).size();
            long previousCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(previousPeriodStart) && c.getCreatedAt().isBefore(periodStart))
                .count();
            
            long currentTotal = currentUsers + currentCampaigns;
            long previousTotal = previousUsers + previousCampaigns;
            
            if (previousTotal == 0) {
                return currentTotal > 0 ? 100.0 : 0.0;
            }
            
            return ((double) (currentTotal - previousTotal) / previousTotal) * 100;
            
        } catch (Exception e) {
            logger.error("Error calculating growth rate", e);
            return 0.0;
        }
    }

    /**
     * Generate system recommendations based on current state
     */
    private List<Map<String, Object>> generateRecommendations() {
        logger.debug("Generating system recommendations");
        
        List<Map<String, Object>> recommendations = new java.util.ArrayList<>();
        
        try {
            // Check pending campaign backlog
            long pendingCount = campaignRepository.findByStatusOrderByCreatedAtDesc("pending").size();
            if (pendingCount > 10) {
                recommendations.add(Map.of(
                    "type", "WORKFLOW",
                    "priority", "HIGH",
                    "title", "High Pending Campaign Backlog",
                    "description", "There are " + pendingCount + " campaigns pending approval. Consider reviewing them to improve user experience.",
                    "action", "Review pending campaigns",
                    "category", "CAMPAIGN_MANAGEMENT"
                ));
            }
            
            // Check unverified user count
            long unverifiedCount = userRepository.findByVerifiedFalse().size();
            if (unverifiedCount > 50) {
                recommendations.add(Map.of(
                    "type", "USER_MANAGEMENT",
                    "priority", "MEDIUM",
                    "title", "Many Unverified Users",
                    "description", "There are " + unverifiedCount + " unverified users. Consider implementing automated verification reminders.",
                    "action", "Improve verification process",
                    "category", "USER_MANAGEMENT"
                ));
            }
            
            // Check campaign success rates
            List<Campaign> allCampaigns = campaignRepository.findAll();
            long successfulCampaigns = allCampaigns.stream()
                .filter(c -> c.getCompletionPercentage() >= 100.0)
                .count();
            
            double successRate = allCampaigns.size() > 0 ? 
                (double) successfulCampaigns / allCampaigns.size() * 100 : 0;
            
            if (successRate < 30) {
                recommendations.add(Map.of(
                    "type", "PLATFORM_OPTIMIZATION",
                    "priority", "HIGH",
                    "title", "Low Campaign Success Rate",
                    "description", "Current success rate is " + String.format("%.1f", successRate) + "%. Consider providing better guidance to campaign creators.",
                    "action", "Improve campaign creation guidance",
                    "category", "PLATFORM_OPTIMIZATION"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error generating recommendations", e);
        }
        
        return recommendations;
    }

    /**
     * Generate system alerts for critical issues
     */
    private List<Map<String, Object>> generateSystemAlerts() {
        logger.debug("Generating system alerts");
        
        List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        
        try {
            // Check for campaigns pending too long (30+ days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long oldPendingCampaigns = campaignRepository.findAll().stream()
                .filter(c -> "pending".equals(c.getStatus()) && c.getCreatedAt().isBefore(thirtyDaysAgo))
                .count();
            
            if (oldPendingCampaigns > 0) {
                alerts.add(Map.of(
                    "type", "WARNING",
                    "severity", "MEDIUM",
                    "title", "Old Pending Campaigns",
                    "message", oldPendingCampaigns + " campaigns have been pending for over 30 days",
                    "action", "Review and process old pending campaigns",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
            // Check system health
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUsage = ((double) (totalMemory - freeMemory) / totalMemory) * 100;
            
            if (memoryUsage > 80) {
                alerts.add(Map.of(
                    "type", "SYSTEM",
                    "severity", "HIGH",
                    "title", "High Memory Usage",
                    "message", "System memory usage is at " + String.format("%.1f", memoryUsage) + "%",
                    "action", "Monitor system performance and consider scaling",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
            // Check for data integrity issues
            List<Campaign> orphanedCampaigns = campaignRepository.findAll().stream()
                .filter(c -> userService.getUserById(c.getCreatorId()) == null)
                .collect(Collectors.toList());
            
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
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    // ===== CAMPAIGN ANALYTICS =====

    /**
     * Get campaign analytics for admin dashboard
     */
    public Map<String, Object> getCampaignAnalytics() {
        logger.info("Generating campaign analytics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        LocalDate now = LocalDate.now();
        
        // Status distribution
        Map<String, Long> statusDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        // Category distribution
        Map<String, Long> categoryDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        // Active vs Expired
        long activeCampaigns = allCampaigns.stream()
            .filter(Campaign::isActive)
            .count();
        
        long expiredCampaigns = allCampaigns.stream()
            .filter(Campaign::isExpired)
            .count();
        
        // Financial metrics
        double totalGoalAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getGoalAmount)
            .sum();
        
        double totalRaisedAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .sum();
        
        // Success rate
        long successfulCampaigns = allCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .count();
        
        double successRate = allCampaigns.size() > 0 ? 
            (double) successfulCampaigns / allCampaigns.size() * 100 : 0;
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        return Map.of(
            "statusDistribution", statusDistribution,
            "categoryDistribution", categoryDistribution,
            "activityMetrics", Map.of(
                "active", activeCampaigns,
                "expired", expiredCampaigns,
                "recentlyCreated", recentCampaigns
            ),
            "financialMetrics", Map.of(
                "totalGoalAmount", totalGoalAmount,
                "totalRaisedAmount", totalRaisedAmount,
                "averageGoalAmount", allCampaigns.size() > 0 ? totalGoalAmount / allCampaigns.size() : 0,
                "averageRaisedAmount", allCampaigns.size() > 0 ? totalRaisedAmount / allCampaigns.size() : 0,
                "fundingEfficiency", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0
            ),
            "performanceMetrics", Map.of(
                "successfulCampaigns", successfulCampaigns,
                "successRate", successRate,
                "totalCampaigns", allCampaigns.size()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get detailed campaign statistics by date range
     */
    public Map<String, Object> getCampaignStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating campaign stats for date range: {} to {}", startDate, endDate);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate) && c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Daily breakdown
        Map<LocalDate, Long> dailyCampaignCount = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Category performance
        Map<String, Map<String, Object>> categoryPerformance = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    double totalGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double totalRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "count", categoryCampaigns.size(),
                        "totalGoal", totalGoal,
                        "totalRaised", totalRaised,
                        "efficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
                    );
                }
            ));
        
        return Map.of(
            "period", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalCampaigns", filteredCampaigns.size()
            ),
            "dailyBreakdown", dailyCampaignCount,
            "categoryPerformance", categoryPerformance,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get top performing campaigns
     */
    public List<Map<String, Object>> getTopPerformingCampaigns(int limit) {
        logger.info("Fetching top {} performing campaigns", limit);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        return allCampaigns.stream()
            .filter(c -> c.getGoalAmount() > 0)
            .sorted((a, b) -> Double.compare(b.getCompletionPercentage(), a.getCompletionPercentage()))
            .limit(limit)
            .map(campaign -> {
                User creator = userService.getUserById(campaign.getCreatorId());
                return Map.of(
                    "id", campaign.getId(),
                    "name", campaign.getName(),
                    "category", campaign.getCategory(),
                    "goalAmount", campaign.getGoalAmount(),
                    "raisedAmount", campaign.getRaisedAmount(),
                    "completionPercentage", campaign.getCompletionPercentage(),
                    "status", campaign.getStatus(),
                    "creatorUsername", creator != null ? creator.getUsername() : "Unknown",
                    "createdAt", campaign.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Get advanced campaign statistics with filters
     */
    public Map<String, Object> getAdvancedCampaignStats(String category, String status, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating advanced campaign statistics with filters");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Apply filters
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> category == null || category.equals("all") || category.equals(c.getCategory()))
            .filter(c -> status == null || status.equals("all") || status.equals(c.getStatus()))
            .filter(c -> startDate == null || c.getCreatedAt().isAfter(startDate))
            .filter(c -> endDate == null || c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Calculate detailed statistics
        double totalGoal = filteredCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaised = filteredCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        Map<String, Long> statusBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        Map<String, Long> categoryBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        List<Campaign> successfulCampaigns = filteredCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .collect(Collectors.toList());
        
        return Map.of(
            "filters", Map.of(
                "category", category != null ? category : "all",
                "status", status != null ? status : "all",
                "startDate", startDate,
                "endDate", endDate
            ),
            "summary", Map.of(
                "totalCampaigns", filteredCampaigns.size(),
                "totalGoalAmount", totalGoal,
                "totalRaisedAmount", totalRaised,
                "averageGoalAmount", filteredCampaigns.size() > 0 ? totalGoal / filteredCampaigns.size() : 0,
                "averageRaisedAmount", filteredCampaigns.size() > 0 ? totalRaised / filteredCampaigns.size() : 0,
                "successfulCampaigns", successfulCampaigns.size(),
                "successRate", filteredCampaigns.size() > 0 ? 
                    (double) successfulCampaigns.size() / filteredCampaigns.size() * 100 : 0,
                "fundingEfficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
            ),
            "breakdowns", Map.of(
                "byStatus", statusBreakdown,
                "byCategory", categoryBreakdown
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get campaign performance trends
     */
  /**
     * Get campaign performance trends
     */
    public Map<String, Object> getCampaignPerformanceTrends(int days) {
        logger.info("Generating campaign performance trends for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Campaign> recentCampaigns = campaignRepository.findAll().stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        // Daily campaign creation trend
        Map<LocalDate, Long> dailyCreations = recentCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Daily approval trend
        Map<LocalDate, Long> dailyApprovals = recentCampaigns.stream()
            .filter(c -> "approved".equals(c.getStatus()))
            .collect(Collectors.groupingBy(
                c -> c.getUpdatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Success rate trend
        Map<LocalDate, Double> dailySuccessRates = recentCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate()
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> dayCampaigns = entry.getValue();
                    long successful = dayCampaigns.stream()
                        .filter(c -> c.getCompletionPercentage() >= 100.0)
                        .count();
                    return dayCampaigns.size() > 0 ? (double) successful / dayCampaigns.size() * 100 : 0;
                }
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "totalCampaigns", recentCampaigns.size()
            ),
            "trends", Map.of(
                "dailyCreations", dailyCreations,
                "dailyApprovals", dailyApprovals,
                "dailySuccessRates", dailySuccessRates
            ),
            "summary", Map.of(
                "averageDailyCreations", days > 0 ? (double) recentCampaigns.size() / days : 0,
                "totalGoalAmount", recentCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum(),
                "totalRaisedAmount", recentCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== USER ANALYTICS =====

    /**
     * Get user analytics for admin dashboard
     */
    public Map<String, Object> getUserAnalytics() {
        logger.info("Generating user analytics");
        
        List<User> allUsers = userRepository.findAll();
        
        // Role distribution
        Map<String, Long> roleDistribution = allUsers.stream()
            .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
        
        // Verification status
        long verifiedUsers = allUsers.stream()
            .filter(User::isVerified)
            .count();
        
        long unverifiedUsers = allUsers.size() - verifiedUsers;
        
        // Firebase vs Local users
        long firebaseUsers = allUsers.stream()
            .filter(User::isFirebaseUser)
            .count();
        
        long localUsers = allUsers.size() - firebaseUsers;
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentSignups = allUsers.stream()
            .filter(u -> u.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        return Map.of(
            "roleDistribution", roleDistribution,
            "verificationMetrics", Map.of(
                "verified", verifiedUsers,
                "unverified", unverifiedUsers,
                "verificationRate", allUsers.size() > 0 ? (double) verifiedUsers / allUsers.size() * 100 : 0
            ),
            "authenticationMetrics", Map.of(
                "firebaseUsers", firebaseUsers,
                "localUsers", localUsers,
                "firebaseAdoptionRate", allUsers.size() > 0 ? (double) firebaseUsers / allUsers.size() * 100 : 0
            ),
            "activityMetrics", Map.of(
                "totalUsers", allUsers.size(),
                "recentSignups", recentSignups,
                "activeUsers", allUsers.size()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get user registration trends
     */
    public Map<String, Object> getUserRegistrationTrends(int days) {
        logger.info("Generating user registration trends for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<User> recentUsers = userRepository.findByCreatedAtBetween(startDate, LocalDateTime.now());
        
        // Daily registration count
        Map<LocalDate, Long> dailyRegistrations = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Registration by verification status
        Map<String, Long> verificationBreakdown = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.isVerified() ? "verified" : "unverified",
                Collectors.counting()
            ));
        
        // Registration by authentication method
        Map<String, Long> authMethodBreakdown = recentUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.isFirebaseUser() ? "firebase" : "local",
                Collectors.counting()
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "totalNewUsers", recentUsers.size()
            ),
            "dailyRegistrations", dailyRegistrations,
            "verificationBreakdown", verificationBreakdown,
            "authMethodBreakdown", authMethodBreakdown,
            "averageDaily", days > 0 ? (double) recentUsers.size() / days : 0,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get most active users (by campaign creation)
     */
    public List<Map<String, Object>> getMostActiveUsers(int limit) {
        logger.info("Fetching top {} most active users", limit);
        
        List<User> allUsers = userRepository.findAll();
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Count campaigns per user
        Map<String, Long> campaignCounts = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCreatorId, Collectors.counting()));
        
        return allUsers.stream()
            .map(user -> {
                long campaignCount = campaignCounts.getOrDefault(user.getId(), 0L);
                return Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "verified", user.isVerified(),
                    "campaignCount", campaignCount,
                    "createdAt", user.getCreatedAt()
                );
            })
            .filter(userMap -> (Long) userMap.get("campaignCount") > 0)
            .sorted((a, b) -> Long.compare((Long) b.get("campaignCount"), (Long) a.get("campaignCount")))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get user engagement metrics
     */
    public Map<String, Object> getUserEngagementMetrics() {
        logger.info("Generating user engagement metrics");
        
        List<User> allUsers = userRepository.findAll();
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Calculate engagement metrics
        Map<String, Long> userCampaignCounts = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCreatorId, Collectors.counting()));
        
        long activeCreators = userCampaignCounts.size();
        long passiveUsers = allUsers.size() - activeCreators;
        
        double averageCampaignsPerUser = allUsers.size() > 0 ? 
            (double) allCampaigns.size() / allUsers.size() : 0;
        
        double averageCampaignsPerActiveUser = activeCreators > 0 ? 
            (double) allCampaigns.size() / activeCreators : 0;
        
        // User distribution by campaign count
        Map<String, Long> userDistribution = Map.of(
            "0_campaigns", (long) passiveUsers,
            "1_campaign", userCampaignCounts.values().stream().filter(count -> count == 1).count(),
            "2-5_campaigns", userCampaignCounts.values().stream().filter(count -> count >= 2 && count <= 5).count(),
            "6+_campaigns", userCampaignCounts.values().stream().filter(count -> count > 5).count()
        );
        
        return Map.of(
            "overview", Map.of(
                "totalUsers", allUsers.size(),
                "activeCreators", activeCreators,
                "passiveUsers", passiveUsers,
                "engagementRate", allUsers.size() > 0 ? (double) activeCreators / allUsers.size() * 100 : 0
            ),
            "campaignMetrics", Map.of(
                "totalCampaigns", allCampaigns.size(),
                "averageCampaignsPerUser", averageCampaignsPerUser,
                "averageCampaignsPerActiveUser", averageCampaignsPerActiveUser
            ),
            "userDistribution", userDistribution,
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== FINANCIAL ANALYTICS =====

    /**
     * Get financial analytics and metrics
     */
    public Map<String, Object> getFinancialAnalytics() {
        logger.info("Generating financial analytics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Overall financial metrics
        double totalGoalAmount = allCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaisedAmount = allCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        // Category-wise financial breakdown
        Map<String, Map<String, Double>> categoryFinancials = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    double categoryGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double categoryRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "totalGoal", categoryGoal,
                        "totalRaised", categoryRaised,
                        "efficiency", categoryGoal > 0 ? (categoryRaised / categoryGoal) * 100 : 0,
                        "campaignCount", (double) categoryCampaigns.size()
                    );
                }
            ));
        
        // Monthly financial trends (last 12 months)
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        Map<String, Map<String, Double>> monthlyTrends = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(twelveMonthsAgo))
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().getYear() + "-" + String.format("%02d", c.getCreatedAt().getMonthValue())
            ))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> monthCampaigns = entry.getValue();
                    double monthGoal = monthCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double monthRaised = monthCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "goal", monthGoal,
                        "raised", monthRaised,
                        "campaignCount", (double) monthCampaigns.size()
                    );
                }
            ));
        
        // Success metrics
        List<Campaign> successfulCampaigns = allCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .collect(Collectors.toList());
        
        double averageSuccessfulAmount = successfulCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .average()
            .orElse(0.0);
        
        return Map.of(
            "overview", Map.of(
                "totalGoalAmount", totalGoalAmount,
                "totalRaisedAmount", totalRaisedAmount,
                "overallEfficiency", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0,
                "averageGoalPerCampaign", allCampaigns.size() > 0 ? totalGoalAmount / allCampaigns.size() : 0,
                "averageRaisedPerCampaign", allCampaigns.size() > 0 ? totalRaisedAmount / allCampaigns.size() : 0
            ),
            "categoryBreakdown", categoryFinancials,
            "monthlyTrends", monthlyTrends,
            "successMetrics", Map.of(
                "successfulCampaigns", successfulCampaigns.size(),
                "successRate", allCampaigns.size() > 0 ? (double) successfulCampaigns.size() / allCampaigns.size() * 100 : 0,
                "averageSuccessfulAmount", averageSuccessfulAmount,
                "totalSuccessfulAmount", successfulCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    // ===== PLATFORM ACTIVITY & REPORTS =====

    /**
     * Get platform activity report
     */
    public Map<String, Object> getPlatformActivityReport(int days) {
        logger.info("Generating platform activity report for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        
        // User activity
        List<User> newUsers = userRepository.findByCreatedAtBetween(startDate, endDate);
        
        // Campaign activity
        List<Campaign> allCampaigns = campaignRepository.findAll();
        List<Campaign> newCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate))
            .collect(Collectors.toList());
        
        List<Campaign> approvedCampaigns = allCampaigns.stream()
            .filter(c -> c.getUpdatedAt().isAfter(startDate) && "approved".equals(c.getStatus()))
            .collect(Collectors.toList());
        
        List<Campaign> rejectedCampaigns = allCampaigns.stream()
            .filter(c -> c.getUpdatedAt().isAfter(startDate) && "rejected".equals(c.getStatus()))
            .collect(Collectors.toList());
        
        // Financial activity
        double newFundingGoals = newCampaigns.stream()
            .mapToDouble(Campaign::getGoalAmount)
            .sum();
        
        double newFundsRaised = newCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .sum();
        
        // Daily breakdown
        Map<LocalDate, Long> dailyUserSignups = newUsers.stream()
            .collect(Collectors.groupingBy(
                u -> u.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        Map<LocalDate, Long> dailyCampaignCreations = newCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        return Map.of(
            "period", Map.of(
                "days", days,
                "startDate", startDate,
                "endDate", endDate
            ),
            "userActivity", Map.of(
                "newUsers", newUsers.size(),
                "dailySignups", dailyUserSignups,
                "newUserDetails", newUsers.stream()
                    .map(u -> Map.of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail(),
                        "verified", u.isVerified(),
                        "firebaseUser", u.isFirebaseUser(),
                        "createdAt", u.getCreatedAt()
                    ))
                    .collect(Collectors.toList())
            ),
            "campaignActivity", Map.of(
                "newCampaigns", newCampaigns.size(),
                "approvedCampaigns", approvedCampaigns.size(),
                "rejectedCampaigns", rejectedCampaigns.size(),
                "dailyCreations", dailyCampaignCreations,
                "newFundingGoals", newFundingGoals,
                "newFundsRaised", newFundsRaised,
                "approvalRate", newCampaigns.size() > 0 ? 
                    (double) approvedCampaigns.size() / newCampaigns.size() * 100 : 0
            ),
            "summary", Map.of(
                "totalNewItems", newUsers.size() + newCampaigns.size(),
                "avgDailySignups", days > 0 ? (double) newUsers.size() / days : 0,
                "avgDailyCampaigns", days > 0 ? (double) newCampaigns.size() / days : 0,
                "platformGrowthRate", calculateGrowthRate(days)
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Generate comprehensive admin report
     */
    public Map<String, Object> generateComprehensiveReport() {
        logger.info("Generating comprehensive admin report");
        
        try {
            // Get all analytics data
            Map<String, Object> campaignAnalytics = getCampaignAnalytics();
            Map<String, Object> userAnalytics = getUserAnalytics();
            Map<String, Object> financialAnalytics = getFinancialAnalytics();
            
            // Get recent activity (last 7 days)
            Map<String, Object> recentActivity = getPlatformActivityReport(7);
            
            // Get top performers
            List<Map<String, Object>> topCampaigns = getTopPerformingCampaigns(10);
            List<Map<String, Object>> topUsers = getMostActiveUsers(10);
            
            // Financial summary
            List<Campaign> allCampaigns = campaignRepository.findAll();
            double totalGoalsAllTime = allCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
            double totalRaisedAllTime = allCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
            
            // Platform metrics
            long totalUsers = userRepository.count();
            long totalCampaigns = campaignRepository.count();
            long activeCampaigns = allCampaigns.stream().filter(Campaign::isActive).count();
            
            return Map.of(
                "reportMetadata", Map.of(
                    "generatedAt", LocalDateTime.now(),
                    "reportType", "COMPREHENSIVE_ADMIN_REPORT",
                    "version", "1.0"
                ),
                "executiveSummary", Map.of(
                    "totalUsers", totalUsers,
                    "totalCampaigns", totalCampaigns,
                    "activeCampaigns", activeCampaigns,
                    "totalGoalsAllTime", totalGoalsAllTime,
                    "totalRaisedAllTime", totalRaisedAllTime,
                    "platformSuccessRate", totalGoalsAllTime > 0 ? (totalRaisedAllTime / totalGoalsAllTime) * 100 : 0
                ),
                "campaignAnalytics", campaignAnalytics,
                "userAnalytics", userAnalytics,
                "financialAnalytics", financialAnalytics,
                "recentActivity", recentActivity,
                "topPerformers", Map.of(
                    "campaigns", topCampaigns,
                    "users", topUsers
                ),
                "recommendations", generateRecommendations(),
                "alerts", generateSystemAlerts()
            );
            
        } catch (Exception e) {
            logger.error("Error generating comprehensive report", e);
            return Map.of(
                "error", "Failed to generate comprehensive report",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Export platform data for backup or analysis
     */
    public Map<String, Object> exportPlatformData(String format, LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Exporting platform data in format: {} for period: {} to {}", format, startDate, endDate);
        
        try {
            // Filter data by date range
            List<User> users = userRepository.findByCreatedAtBetween(startDate, endDate);
            List<Campaign> campaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(startDate) && c.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
            
            // Prepare export data
            List<Map<String, Object>> exportUsers = users.stream()
                .map(user -> Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "verified", user.isVerified(),
                    "firebaseUser", user.isFirebaseUser(),
                    "createdAt", user.getCreatedAt(),
                    "updatedAt", user.getUpdatedAt()
                ))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> exportCampaigns = campaigns.stream()
                .map(campaign -> {
                    User creator = userService.getUserById(campaign.getCreatorId());
                    return Map.of(
                        "id", campaign.getId(),
                        "name", campaign.getName(),
                        "category", campaign.getCategory(),
                        "description", campaign.getDescription(),
                        "goalAmount", campaign.getGoalAmount(),
                        "raisedAmount", campaign.getRaisedAmount(),
                        "status", campaign.getStatus(),
                        "verified", campaign.isVerified(),
                        "creatorId", campaign.getCreatorId(),
                        "creatorUsername", creator != null ? creator.getUsername() : "Unknown",
                        "startDate", campaign.getStartDate(),
                        "endDate", campaign.getEndDate(),
                        "createdAt", campaign.getCreatedAt(),
                        "updatedAt", campaign.getUpdatedAt(),
                        "completionPercentage", campaign.getCompletionPercentage(),
                        "isActive", campaign.isActive()
                    );
                })
                .collect(Collectors.toList());
            
            return Map.of(
                "exportMetadata", Map.of(
                    "format", format,
                    "startDate", startDate,
                    "endDate", endDate,
                    "exportedAt", LocalDateTime.now(),
                    "userCount", exportUsers.size(),
                    "campaignCount", exportCampaigns.size()
                ),
                "users", exportUsers,
                "campaigns", exportCampaigns,
                "summary", Map.of(
                    "totalRecords", exportUsers.size() + exportCampaigns.size(),
                    "dateRange", Map.of(
                        "start", startDate,
                        "end", endDate
                    )
                )
            );
            
        } catch (Exception e) {
            logger.error("Error exporting platform data", e);
            return Map.of(
                "error", "Failed to export platform data",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Calculate platform growth rate
     */
    private double calculateGrowthRate(int days) {
        try {
            LocalDateTime periodStart = LocalDateTime.now().minusDays(days);
            LocalDateTime previousPeriodStart = LocalDateTime.now().minusDays(days * 2);
            
            // Current period counts
            long currentUsers = userRepository.findByCreatedAtBetween(periodStart, LocalDateTime.now()).size();
            long currentCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(periodStart))
                .count();
            
            // Previous period counts
            long previousUsers = userRepository.findByCreatedAtBetween(previousPeriodStart, periodStart).size();
            long previousCampaigns = campaignRepository.findAll().stream()
                .filter(c -> c.getCreatedAt().isAfter(previousPeriodStart) && c.getCreatedAt().isBefore(periodStart))
                .count();
            
            long currentTotal = currentUsers + currentCampaigns;
            long previousTotal = previousUsers + previousCampaigns;
            
            if (previousTotal == 0) {
                return currentTotal > 0 ? 100.0 : 0.0;
            }
            
            return ((double) (currentTotal - previousTotal) / previousTotal) * 100;
            
        } catch (Exception e) {
            logger.error("Error calculating growth rate", e);
            return 0.0;
        }
    }

    /**
     * Generate system recommendations based on current state
     */
    private List<Map<String, Object>> generateRecommendations() {
        logger.debug("Generating system recommendations");
        
        List<Map<String, Object>> recommendations = new java.util.ArrayList<>();
        
        try {
            // Check pending campaign backlog
            long pendingCount = campaignRepository.findByStatusOrderByCreatedAtDesc("pending").size();
            if (pendingCount > 10) {
                recommendations.add(Map.of(
                    "type", "WORKFLOW",
                    "priority", "HIGH",
                    "title", "High Pending Campaign Backlog",
                    "description", "There are " + pendingCount + " campaigns pending approval. Consider reviewing them to improve user experience.",
                    "action", "Review pending campaigns",
                    "category", "CAMPAIGN_MANAGEMENT"
                ));
            }
            
            // Check unverified user count
            long unverifiedCount = userRepository.findByVerifiedFalse().size();
            if (unverifiedCount > 50) {
                recommendations.add(Map.of(
                    "type", "USER_MANAGEMENT",
                    "priority", "MEDIUM",
                    "title", "Many Unverified Users",
                    "description", "There are " + unverifiedCount + " unverified users. Consider implementing automated verification reminders.",
                    "action", "Improve verification process",
                    "category", "USER_MANAGEMENT"
                ));
            }
            
            // Check campaign success rates
            List<Campaign> allCampaigns = campaignRepository.findAll();
            long successfulCampaigns = allCampaigns.stream()
                .filter(c -> c.getCompletionPercentage() >= 100.0)
                .count();
            
            double successRate = allCampaigns.size() > 0 ? 
                (double) successfulCampaigns / allCampaigns.size() * 100 : 0;
            
            if (successRate < 30) {
                recommendations.add(Map.of(
                    "type", "PLATFORM_OPTIMIZATION",
                    "priority", "HIGH",
                    "title", "Low Campaign Success Rate",
                    "description", "Current success rate is " + String.format("%.1f", successRate) + "%. Consider providing better guidance to campaign creators.",
                    "action", "Improve campaign creation guidance",
                    "category", "PLATFORM_OPTIMIZATION"
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error generating recommendations", e);
        }
        
        return recommendations;
    }

    /**
     * Generate system alerts for critical issues
     */
    private List<Map<String, Object>> generateSystemAlerts() {
        logger.debug("Generating system alerts");
        
        List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        
        try {
            // Check for campaigns pending too long (30+ days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long oldPendingCampaigns = campaignRepository.findAll().stream()
                .filter(c -> "pending".equals(c.getStatus()) && c.getCreatedAt().isBefore(thirtyDaysAgo))
                .count();
            
            if (oldPendingCampaigns > 0) {
                alerts.add(Map.of(
                    "type", "WARNING",
                    "severity", "MEDIUM",
                    "title", "Old Pending Campaigns",
                    "message", oldPendingCampaigns + " campaigns have been pending for over 30 days",
                    "action", "Review and process old pending campaigns",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
            // Check system health
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            double memoryUsage = ((double) (totalMemory - freeMemory) / totalMemory) * 100;
            
            if (memoryUsage > 80) {
                alerts.add(Map.of(
                    "type", "SYSTEM",
                    "severity", "HIGH",
                    "title", "High Memory Usage",
                    "message", "System memory usage is at " + String.format("%.1f", memoryUsage) + "%",
                    "action", "Monitor system performance and consider scaling",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
            if (!orphanedCampaigns.isEmpty()) {
                alerts.add(Map.of(
                    "type", "DATA_INTEGRITY",
                    "severity", "MEDIUM",
                    "title", "Orphaned Campaign Data",
                    "message", orphanedCampaigns.size() + " campaigns have invalid creator references",
                    "action", "Run data cleanup to fix orphaned campaigns",
                    "timestamp", LocalDateTime.now()
                ));
            }
            
        } catch (Exception e) {
            logger.error("Error generating system alerts", e);
        }
        
        return alerts;
    }
}package com.fundizen.fundizen_backend.service;

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
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserService userService;

    // ===== CAMPAIGN ANALYTICS =====

    /**
     * Get campaign analytics for admin dashboard
     */
    public Map<String, Object> getCampaignAnalytics() {
        logger.info("Generating campaign analytics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        LocalDate now = LocalDate.now();
        
        // Status distribution
        Map<String, Long> statusDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        // Category distribution
        Map<String, Long> categoryDistribution = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        // Active vs Expired
        long activeCampaigns = allCampaigns.stream()
            .filter(Campaign::isActive)
            .count();
        
        long expiredCampaigns = allCampaigns.stream()
            .filter(Campaign::isExpired)
            .count();
        
        // Financial metrics
        double totalGoalAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getGoalAmount)
            .sum();
        
        double totalRaisedAmount = allCampaigns.stream()
            .mapToDouble(Campaign::getRaisedAmount)
            .sum();
        
        // Success rate
        long successfulCampaigns = allCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .count();
        
        double successRate = allCampaigns.size() > 0 ? 
            (double) successfulCampaigns / allCampaigns.size() * 100 : 0;
        
        // Recent activity (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long recentCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
        
        return Map.of(
            "statusDistribution", statusDistribution,
            "categoryDistribution", categoryDistribution,
            "activityMetrics", Map.of(
                "active", activeCampaigns,
                "expired", expiredCampaigns,
                "recentlyCreated", recentCampaigns
            ),
            "financialMetrics", Map.of(
                "totalGoalAmount", totalGoalAmount,
                "totalRaisedAmount", totalRaisedAmount,
                "averageGoalAmount", allCampaigns.size() > 0 ? totalGoalAmount / allCampaigns.size() : 0,
                "averageRaisedAmount", allCampaigns.size() > 0 ? totalRaisedAmount / allCampaigns.size() : 0,
                "fundingEfficiency", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0
            ),
            "performanceMetrics", Map.of(
                "successfulCampaigns", successfulCampaigns,
                "successRate", successRate,
                "totalCampaigns", allCampaigns.size()
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get detailed campaign statistics by date range
     */
    public Map<String, Object> getCampaignStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating campaign stats for date range: {} to {}", startDate, endDate);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(startDate) && c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Daily breakdown
        Map<LocalDate, Long> dailyCampaignCount = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().toLocalDate(),
                Collectors.counting()
            ));
        
        // Category performance
        Map<String, Map<String, Object>> categoryPerformance = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    double totalGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double totalRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    return Map.of(
                        "count", categoryCampaigns.size(),
                        "totalGoal", totalGoal,
                        "totalRaised", totalRaised,
                        "efficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
                    );
                }
            ));
        
        return Map.of(
            "period", Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "totalCampaigns", filteredCampaigns.size()
            ),
            "dailyBreakdown", dailyCampaignCount,
            "categoryPerformance", categoryPerformance,
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get top performing campaigns
     */
    public List<Map<String, Object>> getTopPerformingCampaigns(int limit) {
        logger.info("Fetching top {} performing campaigns", limit);
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        return allCampaigns.stream()
            .filter(c -> c.getGoalAmount() > 0)
            .sorted((a, b) -> Double.compare(b.getCompletionPercentage(), a.getCompletionPercentage()))
            .limit(limit)
            .map(campaign -> {
                User creator = userService.getUserById(campaign.getCreatorId());
                return Map.of(
                    "id", campaign.getId(),
                    "name", campaign.getName(),
                    "category", campaign.getCategory(),
                    "goalAmount", campaign.getGoalAmount(),
                    "raisedAmount", campaign.getRaisedAmount(),
                    "completionPercentage", campaign.getCompletionPercentage(),
                    "status", campaign.getStatus(),
                    "creatorUsername", creator != null ? creator.getUsername() : "Unknown",
                    "createdAt", campaign.getCreatedAt()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Get advanced campaign statistics with filters
     */
    public Map<String, Object> getAdvancedCampaignStats(String category, String status, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating advanced campaign statistics with filters");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Apply filters
        List<Campaign> filteredCampaigns = allCampaigns.stream()
            .filter(c -> category == null || category.equals("all") || category.equals(c.getCategory()))
            .filter(c -> status == null || status.equals("all") || status.equals(c.getStatus()))
            .filter(c -> startDate == null || c.getCreatedAt().isAfter(startDate))
            .filter(c -> endDate == null || c.getCreatedAt().isBefore(endDate))
            .collect(Collectors.toList());
        
        // Calculate detailed statistics
        double totalGoal = filteredCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaised = filteredCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        Map<String, Long> statusBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
        
        Map<String, Long> categoryBreakdown = filteredCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
        
        List<Campaign> successfulCampaigns = filteredCampaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .collect(Collectors.toList());
        
        return Map.of(
            "filters", Map.of(
                "category", category != null ? category : "all",
                "status", status != null ? status : "all",
                "startDate", startDate,
                "endDate", endDate
            ),
            "summary", Map.of(
                "totalCampaigns", filteredCampaigns.size(),
                "totalGoalAmount", totalGoal,
                "totalRaisedAmount", totalRaised,
                "averageGoalAmount", filteredCampaigns.size() > 0 ? totalGoal / filteredCampaigns.size() : 0,
                "averageRaisedAmount", filteredCampaigns.size() > 0 ? totalRaised / filteredCampaigns.size() : 0,
                "successfulCampaigns", successfulCampaigns.size(),
                "successRate", filteredCampaigns.size() > 0 ? 
                    (double) successfulCampaigns.size() / filteredCampaigns.size() * 100 : 0,
                "fundingEfficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0
            ),
            "breakdowns", Map.of(
                "byStatus", statusBreakdown,
                "byCategory", categoryBreakdown
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get campaign performance trends
     */
    public Map<String, Object