package com.fundizen.fundizen_backend.service.analytics;

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
public class UserAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(UserAnalyticsService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CampaignRepository campaignRepository;

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

    /**
     * Get user demographics and distribution
     */
    public Map<String, Object> getUserDemographics() {
        logger.info("Generating user demographics");
        
        List<User> allUsers = userRepository.findAll();
        
        // Registration patterns by month
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        Map<String, Long> monthlyRegistrations = allUsers.stream()
            .filter(u -> u.getCreatedAt().isAfter(sixMonthsAgo))
            .collect(Collectors.groupingBy(
                u -> u.getCreatedAt().getYear() + "-" + String.format("%02d", u.getCreatedAt().getMonthValue()),
                Collectors.counting()
            ));
        
        // Verification completion rate
        long totalUsers = allUsers.size();
        long verifiedUsers = allUsers.stream().filter(User::isVerified).count();
        double verificationRate = totalUsers > 0 ? (double) verifiedUsers / totalUsers * 100 : 0;
        
        // Account age distribution
        LocalDateTime now = LocalDateTime.now();
        Map<String, Long> accountAgeDistribution = allUsers.stream()
            .collect(Collectors.groupingBy(
                u -> {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(u.getCreatedAt(), now);
                    if (days < 30) return "new_users_30d";
                    else if (days < 90) return "users_30_90d";
                    else if (days < 365) return "users_90_365d";
                    else return "veteran_users_1y+";
                },
                Collectors.counting()
            ));
        
        return Map.of(
            "summary", Map.of(
                "totalUsers", totalUsers,
                "verificationRate", verificationRate,
                "averageAccountAge", calculateAverageAccountAge(allUsers)
            ),
            "registrationTrends", Map.of(
                "monthlyRegistrations", monthlyRegistrations,
                "accountAgeDistribution", accountAgeDistribution
            ),
            "userSegments", Map.of(
                "byRole", allUsers.stream().collect(Collectors.groupingBy(User::getRole, Collectors.counting())),
                "byAuthMethod", allUsers.stream().collect(Collectors.groupingBy(
                    u -> u.isFirebaseUser() ? "firebase" : "local", 
                    Collectors.counting()
                )),
                "byVerificationStatus", allUsers.stream().collect(Collectors.groupingBy(
                    u -> u.isVerified() ? "verified" : "unverified",
                    Collectors.counting()
                ))
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Calculate average account age in days
     */
    private double calculateAverageAccountAge(List<User> users) {
        if (users.isEmpty()) return 0.0;
        
        LocalDateTime now = LocalDateTime.now();
        return users.stream()
            .mapToLong(u -> java.time.temporal.ChronoUnit.DAYS.between(u.getCreatedAt(), now))
            .average()
            .orElse(0.0);
    }
}