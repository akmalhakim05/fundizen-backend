package com.fundizen.fundizen_backend.service.analytics;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.repository.CampaignRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FinancialAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(FinancialAnalyticsService.class);

    @Autowired
    private CampaignRepository campaignRepository;

    /**
     * Get comprehensive financial analytics
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

    /**
     * Get funding efficiency metrics
     */
    public Map<String, Object> getFundingEfficiencyMetrics() {
        logger.info("Calculating funding efficiency metrics");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Efficiency by goal amount ranges
        Map<String, Map<String, Object>> goalRangeEfficiency = allCampaigns.stream()
            .collect(Collectors.groupingBy(this::categorizeByGoalAmount))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> campaigns = entry.getValue();
                    double totalGoal = campaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double totalRaised = campaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    long successfulCount = campaigns.stream()
                        .filter(c -> c.getCompletionPercentage() >= 100.0)
                        .count();
                    
                    return Map.of(
                        "campaignCount", campaigns.size(),
                        "totalGoal", totalGoal,
                        "totalRaised", totalRaised,
                        "efficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0,
                        "successRate", campaigns.size() > 0 ? (double) successfulCount / campaigns.size() * 100 : 0
                    );
                }
            ));
        
        // Time-based efficiency (campaigns by duration)
        Map<String, Map<String, Object>> durationEfficiency = allCampaigns.stream()
            .filter(c -> c.getStartDate() != null && c.getEndDate() != null)
            .collect(Collectors.groupingBy(this::categorizeByCampaignDuration))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> campaigns = entry.getValue();
                    double avgEfficiency = campaigns.stream()
                        .mapToDouble(Campaign::getCompletionPercentage)
                        .average()
                        .orElse(0.0);
                    
                    return Map.of(
                        "campaignCount", campaigns.size(),
                        "averageEfficiency", avgEfficiency,
                        "successfulCampaigns", campaigns.stream()
                            .filter(c -> c.getCompletionPercentage() >= 100.0)
                            .count()
                    );
                }
            ));
        
        return Map.of(
            "goalRangeAnalysis", goalRangeEfficiency,
            "durationAnalysis", durationEfficiency,
            "overallMetrics", calculateOverallEfficiencyMetrics(allCampaigns),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get revenue and growth projections
     */
    public Map<String, Object> getRevenueProjections() {
        logger.info("Generating revenue projections");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        // Historical growth data (last 6 months)
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<Campaign> recentCampaigns = allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(sixMonthsAgo))
            .collect(Collectors.toList());
        
        // Monthly revenue data
        Map<String, Double> monthlyRevenue = recentCampaigns.stream()
            .collect(Collectors.groupingBy(
                c -> c.getCreatedAt().getYear() + "-" + String.format("%02d", c.getCreatedAt().getMonthValue()),
                Collectors.summingDouble(Campaign::getRaisedAmount)
            ));
        
        // Growth rate calculation
        double growthRate = calculateMonthlyGrowthRate(monthlyRevenue);
        
        // Active campaigns potential
        List<Campaign> activeCampaigns = allCampaigns.stream()
            .filter(Campaign::isActive)
            .collect(Collectors.toList());
        
        double potentialRevenue = activeCampaigns.stream()
            .mapToDouble(c -> c.getGoalAmount() - c.getRaisedAmount())
            .sum();
        
        return Map.of(
            "historicalData", Map.of(
                "monthlyRevenue", monthlyRevenue,
                "totalRevenueLastSixMonths", recentCampaigns.stream()
                    .mapToDouble(Campaign::getRaisedAmount)
                    .sum()
            ),
            "growthMetrics", Map.of(
                "monthlyGrowthRate", growthRate,
                "projectedNextMonth", projectNextMonthRevenue(monthlyRevenue, growthRate),
                "annualizedGrowthRate", growthRate * 12
            ),
            "potentialRevenue", Map.of(
                "activeCampaignsPotential", potentialRevenue,
                "averageCampaignValue", activeCampaigns.size() > 0 ? 
                    activeCampaigns.stream().mapToDouble(Campaign::getGoalAmount).average().orElse(0.0) : 0,
                "completionRisk", calculateCompletionRisk(activeCampaigns)
            ),
            "timestamp", LocalDateTime.now()
        );
    }

    /**
     * Get financial performance by category
     */
    public Map<String, Object> getCategoryFinancialPerformance() {
        logger.info("Analyzing financial performance by category");
        
        List<Campaign> allCampaigns = campaignRepository.findAll();
        
        Map<String, Map<String, Object>> categoryPerformance = allCampaigns.stream()
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    List<Campaign> categoryCampaigns = entry.getValue();
                    
                    double totalGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
                    double totalRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
                    double avgGoal = categoryCampaigns.stream().mapToDouble(Campaign::getGoalAmount).average().orElse(0.0);
                    double avgRaised = categoryCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).average().orElse(0.0);
                    
                    long successfulCampaigns = categoryCampaigns.stream()
                        .filter(c -> c.getCompletionPercentage() >= 100.0)
                        .count();
                    
                    // Risk assessment
                    double riskScore = calculateCategoryRisk(categoryCampaigns);
                    
                    return Map.of(
                        "campaignCount", categoryCampaigns.size(),
                        "totalGoal", totalGoal,
                        "totalRaised", totalRaised,
                        "averageGoal", avgGoal,
                        "averageRaised", avgRaised,
                        "efficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0,
                        "successRate", categoryCampaigns.size() > 0 ? (double) successfulCampaigns / categoryCampaigns.size() * 100 : 0,
                        "riskScore", riskScore,
                        "marketShare", totalRaised / allCampaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum() * 100
                    );
                }
            ));
        
        return Map.of(
            "categoryPerformance", categoryPerformance,
            "topPerformingCategories", getTopPerformingCategories(categoryPerformance, 5),
            "emergingCategories", getEmergingCategories(allCampaigns),
            "timestamp", LocalDateTime.now()
        );
    }

    // Private helper methods
    
    private String categorizeByGoalAmount(Campaign campaign) {
        double goal = campaign.getGoalAmount();
        if (goal <= 1000) return "small_1k";
        else if (goal <= 5000) return "medium_5k";
        else if (goal <= 10000) return "large_10k";
        else if (goal <= 50000) return "xlarge_50k";
        else return "enterprise_50k+";
    }
    
    private String categorizeByCampaignDuration(Campaign campaign) {
        if (campaign.getStartDate() == null || campaign.getEndDate() == null) {
            return "unknown";
        }
        
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            campaign.getStartDate(), campaign.getEndDate()
        );
        
        if (days <= 30) return "short_30d";
        else if (days <= 60) return "medium_60d";
        else if (days <= 90) return "long_90d";
        else return "extended_90d+";
    }
    
    private Map<String, Object> calculateOverallEfficiencyMetrics(List<Campaign> campaigns) {
        double totalGoal = campaigns.stream().mapToDouble(Campaign::getGoalAmount).sum();
        double totalRaised = campaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum();
        
        long successfulCampaigns = campaigns.stream()
            .filter(c -> c.getCompletionPercentage() >= 100.0)
            .count();
        
        return Map.of(
            "overallEfficiency", totalGoal > 0 ? (totalRaised / totalGoal) * 100 : 0,
            "successRate", campaigns.size() > 0 ? (double) successfulCampaigns / campaigns.size() * 100 : 0,
            "averageCompletionRate", campaigns.stream()
                .mapToDouble(Campaign::getCompletionPercentage)
                .average()
                .orElse(0.0)
        );
    }
    
    private double calculateMonthlyGrowthRate(Map<String, Double> monthlyRevenue) {
        if (monthlyRevenue.size() < 2) return 0.0;
        
        List<Double> revenues = monthlyRevenue.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
        
        double totalGrowth = 0.0;
        int periods = 0;
        
        for (int i = 1; i < revenues.size(); i++) {
            double previous = revenues.get(i - 1);
            double current = revenues.get(i);
            
            if (previous > 0) {
                totalGrowth += (current - previous) / previous * 100;
                periods++;
            }
        }
        
        return periods > 0 ? totalGrowth / periods : 0.0;
    }
    
    private double projectNextMonthRevenue(Map<String, Double> monthlyRevenue, double growthRate) {
        if (monthlyRevenue.isEmpty()) return 0.0;
        
        double lastMonthRevenue = monthlyRevenue.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
            .findFirst()
            .map(Map.Entry::getValue)
            .orElse(0.0);
        
        return lastMonthRevenue * (1 + growthRate / 100);
    }
    
    private double calculateCompletionRisk(List<Campaign> activeCampaigns) {
        if (activeCampaigns.isEmpty()) return 0.0;
        
        return activeCampaigns.stream()
            .mapToDouble(c -> {
                double timeRemaining = Math.max(0, c.getDaysRemaining());
                double completionRate = c.getCompletionPercentage();
                
                // Higher risk for campaigns with low completion and little time left
                if (timeRemaining < 7 && completionRate < 50) return 0.8;
                else if (timeRemaining < 14 && completionRate < 75) return 0.6;
                else if (completionRate < 25) return 0.4;
                else return 0.2;
            })
            .average()
            .orElse(0.0);
    }
    
    private double calculateCategoryRisk(List<Campaign> categoryCampaigns) {
        if (categoryCampaigns.isEmpty()) return 0.0;
        
        double avgCompletionRate = categoryCampaigns.stream()
            .mapToDouble(Campaign::getCompletionPercentage)
            .average()
            .orElse(0.0);
        
        long failedCampaigns = categoryCampaigns.stream()
            .filter(c -> c.isExpired() && c.getCompletionPercentage() < 100.0)
            .count();
        
        double failureRate = categoryCampaigns.size() > 0 ? 
            (double) failedCampaigns / categoryCampaigns.size() : 0.0;
        
        // Risk score: higher failure rate and lower avg completion = higher risk
        return (failureRate * 0.6) + ((100 - avgCompletionRate) / 100 * 0.4);
    }
    
    private List<Map<String, Object>> getTopPerformingCategories(
        Map<String, Map<String, Object>> categoryPerformance, int limit) {
        
        return categoryPerformance.entrySet().stream()
            .sorted((a, b) -> Double.compare(
                (Double) b.getValue().get("efficiency"),
                (Double) a.getValue().get("efficiency")
            ))
            .limit(limit)
            .map(entry -> Map.of(
                "category", entry.getKey(),
                "efficiency", entry.getValue().get("efficiency"),
                "totalRaised", entry.getValue().get("totalRaised"),
                "successRate", entry.getValue().get("successRate")
            ))
            .collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> getEmergingCategories(List<Campaign> allCampaigns) {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        
        return allCampaigns.stream()
            .filter(c -> c.getCreatedAt().isAfter(threeMonthsAgo))
            .collect(Collectors.groupingBy(Campaign::getCategory))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() >= 3) // At least 3 campaigns
            .map(entry -> {
                List<Campaign> campaigns = entry.getValue();
                double avgEfficiency = campaigns.stream()
                    .mapToDouble(Campaign::getCompletionPercentage)
                    .average()
                    .orElse(0.0);
                
                return Map.of(
                    "category", entry.getKey(),
                    "recentCampaigns", campaigns.size(),
                    "averageEfficiency", avgEfficiency,
                    "totalRaised", campaigns.stream().mapToDouble(Campaign::getRaisedAmount).sum()
                );
            })
            .sorted((a, b) -> Integer.compare(
                (Integer) b.get("recentCampaigns"),
                (Integer) a.get("recentCampaigns")
            ))
            .limit(5)
            .collect(Collectors.toList());
    }
}