package com.fundizen.fundizen_backend.service.analytics;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.repository.CampaignRepository;
import com.fundizen.fundizen_backend.service.UserService;

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
public class CampaignAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignAnalyticsService.class);

    @Autowired
    private CampaignRepository campaignRepository;
    
    @Autowired
    private UserService userService;

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
}