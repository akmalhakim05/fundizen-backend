package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.service.analytics.CampaignAnalyticsService;
import com.fundizen.fundizen_backend.service.analytics.UserAnalyticsService;
import com.fundizen.fundizen_backend.service.analytics.FinancialAnalyticsService;
import com.fundizen.fundizen_backend.service.analytics.PlatformAnalyticsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Refactored AdminAnalyticsService that delegates to specialized analytics services
 * This approach provides better separation of concerns and maintainability
 */
@Service
public class AdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private CampaignAnalyticsService campaignAnalyticsService;
    
    @Autowired
    private UserAnalyticsService userAnalyticsService;
    
    @Autowired
    private FinancialAnalyticsService financialAnalyticsService;
    
    @Autowired
    private PlatformAnalyticsService platformAnalyticsService;

    // ===== CAMPAIGN ANALYTICS DELEGATION =====

    /**
     * Get campaign analytics for admin dashboard
     */
    public Map<String, Object> getCampaignAnalytics() {
        return campaignAnalyticsService.getCampaignAnalytics();
    }

    /**
     * Get detailed campaign statistics by date range
     */
    public Map<String, Object> getCampaignStatsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return campaignAnalyticsService.getCampaignStatsByDateRange(startDate, endDate);
    }

    /**
     * Get top performing campaigns
     */
    public List<Map<String, Object>> getTopPerformingCampaigns(int limit) {
        return campaignAnalyticsService.getTopPerformingCampaigns(limit);
    }

    /**
     * Get advanced campaign statistics with filters
     */
    public Map<String, Object> getAdvancedCampaignStats(String category, String status, 
                                                       LocalDateTime startDate, LocalDateTime endDate) {
        return campaignAnalyticsService.getAdvancedCampaignStats(category, status, startDate, endDate);
    }

    /**
     * Get campaign performance trends
     */
    public Map<String, Object> getCampaignPerformanceTrends(int days) {
        return campaignAnalyticsService.getCampaignPerformanceTrends(days);
    }

    // ===== USER ANALYTICS DELEGATION =====

    /**
     * Get user analytics for admin dashboard
     */
    public Map<String, Object> getUserAnalytics() {
        return userAnalyticsService.getUserAnalytics();
    }

    /**
     * Get user registration trends
     */
    public Map<String, Object> getUserRegistrationTrends(int days) {
        return userAnalyticsService.getUserRegistrationTrends(days);
    }

    /**
     * Get most active users (by campaign creation)
     */
    public List<Map<String, Object>> getMostActiveUsers(int limit) {
        return userAnalyticsService.getMostActiveUsers(limit);
    }

    /**
     * Get user engagement metrics
     */
    public Map<String, Object> getUserEngagementMetrics() {
        return userAnalyticsService.getUserEngagementMetrics();
    }

    /**
     * Get user demographics and distribution
     */
    public Map<String, Object> getUserDemographics() {
        return userAnalyticsService.getUserDemographics();
    }

    // ===== FINANCIAL ANALYTICS DELEGATION =====

    /**
     * Get financial analytics and metrics
     */
    public Map<String, Object> getFinancialAnalytics() {
        return financialAnalyticsService.getFinancialAnalytics();
    }

    /**
     * Get funding efficiency metrics
     */
    public Map<String, Object> getFundingEfficiencyMetrics() {
        return financialAnalyticsService.getFundingEfficiencyMetrics();
    }

    /**
     * Get revenue and growth projections
     */
    public Map<String, Object> getRevenueProjections() {
        return financialAnalyticsService.getRevenueProjections();
    }

    /**
     * Get financial performance by category
     */
    public Map<String, Object> getCategoryFinancialPerformance() {
        return financialAnalyticsService.getCategoryFinancialPerformance();
    }

    // ===== PLATFORM ANALYTICS DELEGATION =====

    /**
     * Get platform activity report
     */
    public Map<String, Object> getPlatformActivityReport(int days) {
        return platformAnalyticsService.getPlatformActivityReport(days);
    }

    /**
     * Export platform data for backup or analysis
     */
    public Map<String, Object> exportPlatformData(String format, LocalDateTime startDate, LocalDateTime endDate) {
        return platformAnalyticsService.exportPlatformData(format, startDate, endDate);
    }

    /**
     * Get system health metrics
     */
    public Map<String, Object> getSystemHealth() {
        return platformAnalyticsService.getSystemHealth();
    }

    /**
     * Generate system recommendations
     */
    public List<Map<String, Object>> generateSystemRecommendations() {
        return platformAnalyticsService.generateSystemRecommendations();
    }

    /**
     * Generate system alerts
     */
    public List<Map<String, Object>> generateSystemAlerts() {
        return platformAnalyticsService.generateSystemAlerts();
    }

    /**
     * Get platform performance trends
     */
    public Map<String, Object> getPlatformPerformanceTrends(int days) {
        return platformAnalyticsService.getPlatformPerformanceTrends(days);
    }

    // ===== COMPOSITE ANALYTICS METHODS =====

    /**
     * Generate comprehensive admin report combining all analytics
     */
    public Map<String, Object> generateComprehensiveReport() {
        logger.info("Generating comprehensive admin report using specialized services");
        
        try {
            // Get analytics from all specialized services
            Map<String, Object> campaignAnalytics = campaignAnalyticsService.getCampaignAnalytics();
            Map<String, Object> userAnalytics = userAnalyticsService.getUserAnalytics();
            Map<String, Object> financialAnalytics = financialAnalyticsService.getFinancialAnalytics();
            Map<String, Object> platformReport = platformAnalyticsService.generateComprehensiveReport();
            
            // Get top performers
            List<Map<String, Object>> topCampaigns = campaignAnalyticsService.getTopPerformingCampaigns(10);
            List<Map<String, Object>> topUsers = userAnalyticsService.getMostActiveUsers(10);
            
            // Get recent activity (last 7 days)
            Map<String, Object> recentActivity = platformAnalyticsService.getPlatformActivityReport(7);
            
            // Combine all data into comprehensive report
            return Map.of(
                "reportMetadata", Map.of(
                    "generatedAt", LocalDateTime.now(),
                    "reportType", "COMPREHENSIVE_ADMIN_REPORT",
                    "version", "2.0",
                    "generatedBy", "AdminAnalyticsService"
                ),
                "campaignAnalytics", campaignAnalytics,
                "userAnalytics", userAnalytics,
                "financialAnalytics", financialAnalytics,
                "platformAnalytics", platformReport,
                "topPerformers", Map.of(
                    "campaigns", topCampaigns,
                    "users", topUsers
                ),
                "recentActivity", recentActivity,
                "recommendations", platformAnalyticsService.generateSystemRecommendations(),
                "alerts", platformAnalyticsService.generateSystemAlerts(),
                "summary", generateExecutiveSummary(campaignAnalytics, userAnalytics, financialAnalytics)
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
     * Get dashboard overview (lightweight version for quick loading)
     */
    public Map<String, Object> getDashboardOverview() {
        logger.info("Generating dashboard overview");
        
        try {
            // Get essential metrics only for quick dashboard loading
            Map<String, Object> campaignOverview = campaignAnalyticsService.getCampaignAnalytics();
            Map<String, Object> userOverview = userAnalyticsService.getUserAnalytics();
            Map<String, Object> systemHealth = platformAnalyticsService.getSystemHealth();
            
            // Extract key metrics
            @SuppressWarnings("unchecked")
            Map<String, Object> campaignMetrics = (Map<String, Object>) campaignOverview.get("performanceMetrics");
            @SuppressWarnings("unchecked")
            Map<String, Object> userMetrics = (Map<String, Object>) userOverview.get("activityMetrics");
            
            return Map.of(
                "overview", Map.of(
                    "totalCampaigns", campaignMetrics.get("totalCampaigns"),
                    "totalUsers", userMetrics.get("totalUsers"),
                    "systemStatus", systemHealth.get("status"),
                    "timestamp", LocalDateTime.now()
                ),
                "quickStats", Map.of(
                    "campaignSuccessRate", campaignMetrics.get("successRate"),
                    "recentSignups", userMetrics.get("recentSignups"),
                    "pendingApprovals", ((Map<?, ?>) systemHealth.get("workQueue")).get("pendingApprovals")
                ),
                "alerts", platformAnalyticsService.generateSystemAlerts().stream()
                    .filter(alert -> "HIGH".equals(((Map<?, ?>) alert).get("severity")))
                    .limit(5)
                    .toArray()
            );
            
        } catch (Exception e) {
            logger.error("Error generating dashboard overview", e);
            return Map.of(
                "error", "Failed to generate dashboard overview",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    /**
     * Get analytics for specific time period with all services
     */
    public Map<String, Object> getAnalyticsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Generating analytics for period: {} to {}", startDate, endDate);
        
        try {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
            int days = Math.max(1, (int) daysBetween);
            
            return Map.of(
                "period", Map.of(
                    "startDate", startDate,
                    "endDate", endDate,
                    "days", days
                ),
                "campaignStats", campaignAnalyticsService.getCampaignStatsByDateRange(startDate, endDate),
                "userTrends", userAnalyticsService.getUserRegistrationTrends(days),
                "platformActivity", platformAnalyticsService.getPlatformActivityReport(days),
                "campaignTrends", campaignAnalyticsService.getCampaignPerformanceTrends(days),
                "platformTrends", platformAnalyticsService.getPlatformPerformanceTrends(days),
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error generating analytics for period", e);
            return Map.of(
                "error", "Failed to generate period analytics",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
        }
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Generate executive summary from all analytics data
     */
    private Map<String, Object> generateExecutiveSummary(Map<String, Object> campaignAnalytics,
                                                        Map<String, Object> userAnalytics,
                                                        Map<String, Object> financialAnalytics) {
        try {
            // Extract key metrics from each analytics service
            @SuppressWarnings("unchecked")
            Map<String, Object> campaignPerf = (Map<String, Object>) campaignAnalytics.get("performanceMetrics");
            @SuppressWarnings("unchecked")
            Map<String, Object> userActivity = (Map<String, Object>) userAnalytics.get("activityMetrics");
            @SuppressWarnings("unchecked")
            Map<String, Object> financialOverview = (Map<String, Object>) financialAnalytics.get("overview");
            
            return Map.of(
                "keyMetrics", Map.of(
                    "totalCampaigns", campaignPerf.get("totalCampaigns"),
                    "totalUsers", userActivity.get("totalUsers"),
                    "campaignSuccessRate", campaignPerf.get("successRate"),
                    "totalFundsRaised", financialOverview.get("totalRaisedAmount"),
                    "platformEfficiency", financialOverview.get("overallEfficiency")
                ),
                "highlights", List.of(
                    "Platform processed " + campaignPerf.get("totalCampaigns") + " campaigns",
                    "User base grew to " + userActivity.get("totalUsers") + " registered users",
                    "Campaign success rate: " + String.format("%.1f%%", (Double) campaignPerf.get("successRate")),
                    "Total funds raised: RM " + String.format("%.2f", (Double) financialOverview.get("totalRaisedAmount"))
                ),
                "generatedAt", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error generating executive summary", e);
            return Map.of(
                "error", "Failed to generate executive summary",
                "timestamp", LocalDateTime.now()
            );
        }
    }
}