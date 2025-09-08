package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.DonationService;
import com.fundizen.fundizen_backend.models.Donation;
import com.fundizen.fundizen_backend.repository.DonationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donations")
@CrossOrigin(origins = "*")
public class DonationController {

    private static final Logger logger = LoggerFactory.getLogger(DonationController.class);

    @Autowired
    private DonationService donationService;

    @Autowired
    private DonationRepository donationRepository;

    /**
     * Get donations for a specific campaign
     * GET /api/donations/campaign/{campaignId}
     */
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<?> getCampaignDonations(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "false") boolean includePrivate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            logger.info("Fetching donations for campaign: {} (includePrivate: {})", campaignId, includePrivate);

            List<Donation> donations = donationService.getCampaignDonations(campaignId, includePrivate);

            // Apply sorting
            if ("desc".equals(sortDir)) {
                donations.sort((a, b) -> {
                    if ("createdAt".equals(sortBy)) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else if ("amount".equals(sortBy)) {
                        return b.getAmount().compareTo(a.getAmount());
                    }
                    return 0;
                });
            } else {
                donations.sort((a, b) -> {
                    if ("createdAt".equals(sortBy)) {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    } else if ("amount".equals(sortBy)) {
                        return a.getAmount().compareTo(b.getAmount());
                    }
                    return 0;
                });
            }

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, donations.size());
            List<Donation> paginatedDonations = donations.subList(start, end);

            // Convert to display format
            List<Map<String, Object>> donationList = paginatedDonations.stream()
                .map(this::convertDonationToPublicFormat)
                .collect(Collectors.toList());

            // Get statistics
            DonationService.CampaignDonationStatistics stats = 
                donationService.getCampaignDonationStatistics(campaignId);

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donationList,
                "statistics", Map.of(
                    "totalAmount", stats.getTotalAmount(),
                    "donationCount", stats.getDonationCount(),
                    "averageAmount", stats.getAverageAmount(),
                    "largestDonation", stats.getLargestDonation(),
                    "uniqueDonors", stats.getUniqueDonors(),
                    "anonymousDonations", stats.getAnonymousDonations(),
                    "donationsWithMessages", stats.getDonationsWithMessages()
                ),
                "pagination", Map.of(
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) donations.size() / size),
                    "totalElements", donations.size(),
                    "size", size
                )
            );

            logger.info("Retrieved {} donations for campaign: {}", donationList.size(), campaignId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donations for campaign: {}", campaignId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get donations by a specific donor
     * GET /api/donations/donor/{donorId}
     */
    @GetMapping("/donor/{donorId}")
    public ResponseEntity<?> getDonorDonations(
            @PathVariable String donorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("Fetching donations for donor: {}", donorId);

            List<Donation> donations = donationService.getDonorDonations(donorId);

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, donations.size());
            List<Donation> paginatedDonations = donations.subList(start, end);

            // Convert to response format
            List<Map<String, Object>> donationList = paginatedDonations.stream()
                .map(this::convertDonationToUserFormat)
                .collect(Collectors.toList());

            // Calculate donor statistics
            double totalDonated = donations.stream()
                .mapToDouble(Donation::getAmount)
                .sum();

            long campaignsSupported = donations.stream()
                .map(Donation::getCampaignId)
                .distinct()
                .count();

            // Recent donations (last 30 days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long recentDonations = donations.stream()
                .filter(d -> d.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donationList,
                "statistics", Map.of(
                    "totalDonated", totalDonated,
                    "donationCount", donations.size(),
                    "campaignsSupported", campaignsSupported,
                    "averageDonation", donations.size() > 0 ? totalDonated / donations.size() : 0.0,
                    "recentDonations", recentDonations
                ),
                "pagination", Map.of(
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) donations.size() / size),
                    "totalElements", donations.size(),
                    "size", size
                )
            );

            logger.info("Retrieved {} donations for donor: {}", donationList.size(), donorId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donations for donor: {}", donorId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donor donations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get donation by ID
     * GET /api/donations/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDonationById(@PathVariable String id) {
        try {
            logger.info("Fetching donation: {}", id);

            Donation donation = donationRepository.findById(id).orElse(null);
            if (donation == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Donation not found"
                ));
            }

            Map<String, Object> response = Map.of(
                "success", true,
                "donation", convertDonationToDetailedFormat(donation)
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donation: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donation: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all donations (admin only - with full details)
     * GET /api/donations
     */
    @GetMapping
    public ResponseEntity<?> getAllDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) String donorId) {
        try {
            logger.info("Fetching all donations - page: {}, size: {}, status: {}, campaignId: {}, donorId: {}", 
                       page, size, status, campaignId, donorId);

            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Donation> donationPage;

            // Apply filters
            if (status != null && campaignId != null) {
                donationPage = donationRepository.findByCampaignIdAndPaymentStatus(campaignId, status, pageable);
            } else if (status != null && donorId != null) {
                donationPage = donationRepository.findByDonorIdAndPaymentStatus(donorId, status, pageable);
            } else if (status != null) {
                donationPage = donationRepository.findByPaymentStatus(status, pageable);
            } else if (campaignId != null) {
                donationPage = donationRepository.findByCampaignId(campaignId, pageable);
            } else if (donorId != null) {
                donationPage = donationRepository.findByDonorId(donorId, pageable);
            } else {
                donationPage = donationService.getDonations(pageable);
            }

            List<Map<String, Object>> donations = donationPage.getContent().stream()
                .map(this::convertDonationToAdminFormat)
                .collect(Collectors.toList());

            // Calculate totals for current page
            double pageTotal = donationPage.getContent().stream()
                .filter(d -> "succeeded".equals(d.getPaymentStatus()))
                .mapToDouble(Donation::getAmount)
                .sum();

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donations,
                "pagination", Map.of(
                    "currentPage", donationPage.getNumber(),
                    "totalPages", donationPage.getTotalPages(),
                    "totalElements", donationPage.getTotalElements(),
                    "size", donationPage.getSize(),
                    "pageTotal", pageTotal
                ),
                "filters", Map.of(
                    "status", status != null ? status : "all",
                    "campaignId", campaignId != null ? campaignId : "all",
                    "donorId", donorId != null ? donorId : "all"
                )
            );

            logger.info("Retrieved {} donations", donations.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching all donations", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get donations with messages for a campaign
     * GET /api/donations/campaign/{campaignId}/messages
     */
    @GetMapping("/campaign/{campaignId}/messages")
    public ResponseEntity<?> getCampaignDonationsWithMessages(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching donations with messages for campaign: {}", campaignId);

            List<Donation> donations = donationRepository.findDonationsWithMessagesByCampaignId(campaignId);

            // Sort by creation date (newest first)
            donations.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, donations.size());
            List<Donation> paginatedDonations = donations.subList(start, end);

            // FIXED: Proper type declaration and lambda return type
            List<Map<String, Object>> donationsWithMessages = paginatedDonations.stream()
                .map(donation -> {
                    Map<String, Object> donationMap = new HashMap<>();
                    donationMap.put("id", donation.getId());
                    donationMap.put("donorName", donation.getDisplayName());
                    donationMap.put("amount", donation.getAmount());
                    donationMap.put("currency", donation.getCurrency());
                    donationMap.put("message", donation.getMessage() != null ? donation.getMessage() : "");
                    donationMap.put("createdAt", donation.getCreatedAt());
                    donationMap.put("isAnonymous", donation.isAnonymous());
                    donationMap.put("timeAgo", calculateTimeAgo(donation.getCreatedAt()));
                    return donationMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donationsWithMessages,
                "pagination", Map.of(
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) donations.size() / size),
                    "totalElements", donations.size(),
                    "size", size
                ),
                "count", donationsWithMessages.size()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donations with messages for campaign: {}", campaignId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donations with messages: " + e.getMessage()
            ));
        }
    }

    /**
     * Get recent donations across platform
     * GET /api/donations/recent
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentDonations(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {
        try {
            logger.info("Fetching recent donations - limit: {}, hours: {}", limit, hours);

            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            List<Donation> recentDonations = donationRepository.findByCreatedAtBetween(since, LocalDateTime.now())
                .stream()
                .filter(d -> "succeeded".equals(d.getPaymentStatus()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .collect(Collectors.toList());

            List<Map<String, Object>> donations = recentDonations.stream()
                .map(donation -> {
                    Map<String, Object> donationMap = convertDonationToPublicFormat(donation);
                    donationMap.put("campaignId", donation.getCampaignId());
                    donationMap.put("timeAgo", calculateTimeAgo(donation.getCreatedAt()));
                    return donationMap;
                })
                .collect(Collectors.toList());

            double totalAmount = recentDonations.stream()
                .mapToDouble(Donation::getAmount)
                .sum();

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donations,
                "summary", Map.of(
                    "count", donations.size(),
                    "totalAmount", totalAmount,
                    "timeframe", hours + " hours",
                    "averageAmount", donations.size() > 0 ? totalAmount / donations.size() : 0.0
                )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching recent donations", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch recent donations: " + e.getMessage()
            ));
        }
    }

    /**
     * Get platform donation statistics
     * GET /api/donations/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getPlatformStatistics() {
        try {
            logger.info("Fetching platform donation statistics");

            DonationService.PlatformDonationStatistics stats = 
                donationService.getPlatformDonationStatistics();

            Map<String, Object> response = Map.of(
                "success", true,
                "statistics", Map.of(
                    "totalAmount", stats.getTotalAmount(),
                    "totalDonations", stats.getTotalDonations(),
                    "uniqueCampaigns", stats.getUniqueCampaigns(),
                    "uniqueDonors", stats.getUniqueDonors(),
                    "totalFees", stats.getTotalFees(),
                    "netAmount", stats.getNetAmount(),
                    "recentDonations", stats.getRecentDonations(),
                    "averageDonation", stats.getTotalDonations() > 0 ? 
                        stats.getTotalAmount() / stats.getTotalDonations() : 0.0
                ),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching platform statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch statistics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get top donors for a campaign
     * GET /api/donations/campaign/{campaignId}/top-donors
     */
    @GetMapping("/campaign/{campaignId}/top-donors")
    public ResponseEntity<?> getTopDonors(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            logger.info("Fetching top donors for campaign: {} (limit: {})", campaignId, limit);

            List<DonationService.TopDonorInfo> topDonors = 
                donationService.getTopDonors(campaignId, limit);

            List<Map<String, Object>> donorList = topDonors.stream()
            .map(donor -> {
                Map<String, Object> donorMap = new HashMap<>();
                donorMap.put("donorId", donor.getDonorId());
                donorMap.put("displayName", donor.getDisplayName());
                donorMap.put("totalAmount", donor.getTotalAmount());
                donorMap.put("donationCount", donor.getDonationCount());
                donorMap.put("averageDonation", donor.getDonationCount() > 0 ? 
                    donor.getTotalAmount() / donor.getDonationCount() : 0.0);
                return donorMap;
            })
            .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "success", true,
                "topDonors", donorList,
                "count", donorList.size(),
                "campaignId", campaignId
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching top donors for campaign: {}", campaignId, e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch top donors: " + e.getMessage()
            ));
        }
    }

    /**
     * Get donation trends (monthly)
     * GET /api/donations/trends
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getDonationTrends(
            @RequestParam(defaultValue = "12") int months) {
        try {
            logger.info("Fetching donation trends for {} months", months);

            List<DonationService.MonthlyDonationTrend> trends = 
                donationService.getMonthlyDonationTrends(months);

            List<Map<String, Object>> trendData = trends.stream()
            .map(trend -> {
                Map<String, Object> trendMap = new HashMap<>();
                trendMap.put("year", trend.getYear());
                trendMap.put("month", trend.getMonth());
                trendMap.put("totalAmount", trend.getTotalAmount());
                trendMap.put("donationCount", trend.getDonationCount());
                trendMap.put("averageAmount", trend.getDonationCount() > 0 ? 
                    trend.getTotalAmount() / trend.getDonationCount() : 0.0);
                trendMap.put("monthName", getMonthName(trend.getMonth()));
                return trendMap;
            })
            .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "success", true,
                "trends", trendData,
                "months", months,
                "summary", Map.of(
                    "totalPeriods", trendData.size(),
                    "totalAmount", trends.stream().mapToDouble(DonationService.MonthlyDonationTrend::getTotalAmount).sum(),
                    "totalDonations", trends.stream().mapToLong(DonationService.MonthlyDonationTrend::getDonationCount).sum()
                )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donation trends", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch donation trends: " + e.getMessage()
            ));
        }
    }

    /**
     * Get donation analytics for admin dashboard
     * GET /api/donations/analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<?> getDonationAnalytics() {
        try {
            logger.info("Fetching donation analytics");

            DonationService.PlatformDonationStatistics platformStats = 
                donationService.getPlatformDonationStatistics();

            // Get recent trends
            List<DonationService.MonthlyDonationTrend> recentTrends = 
                donationService.getMonthlyDonationTrends(6);

            // Calculate growth rate
            double growthRate = calculateGrowthRate(recentTrends);

            // Get donation status breakdown
            Map<String, Long> statusBreakdown = Map.of(
                "succeeded", donationRepository.countByPaymentStatus("succeeded"),
                "pending", donationRepository.countByPaymentStatus("pending"),
                "failed", donationRepository.countByPaymentStatus("failed"),
                "refunded", donationRepository.countByPaymentStatus("refunded")
            );

            Map<String, Object> response = Map.of(
                "success", true,
                "analytics", Map.of(
                    "platform", Map.of(
                        "totalAmount", platformStats.getTotalAmount(),
                        "totalDonations", platformStats.getTotalDonations(),
                        "uniqueCampaigns", platformStats.getUniqueCampaigns(),
                        "uniqueDonors", platformStats.getUniqueDonors(),
                        "averageDonation", platformStats.getTotalDonations() > 0 ? 
                            platformStats.getTotalAmount() / platformStats.getTotalDonations() : 0.0,
                        "recentDonations", platformStats.getRecentDonations(),
                        "growthRate", growthRate
                    ),
                    "statusBreakdown", statusBreakdown,
                    "recentTrends", recentTrends.stream()
                        .map(trend -> Map.of(
                            "period", trend.getYear() + "-" + String.format("%02d", trend.getMonth()),
                            "amount", trend.getTotalAmount(),
                            "count", trend.getDonationCount()
                        ))
                        .collect(Collectors.toList())
                ),
                "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching donation analytics", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch analytics: " + e.getMessage()
            ));
        }
    }

    /**
     * Search donations
     * GET /api/donations/search
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDonations(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            logger.info("Searching donations with query: {}", query);

            // Simple search implementation - can be enhanced with full-text search
            List<Donation> allDonations = donationRepository.findAll();
            
            List<Donation> searchResults = allDonations.stream()
                .filter(donation -> 
                    (donation.getDonorName() != null && donation.getDonorName().toLowerCase().contains(query.toLowerCase())) ||
                    (donation.getDonorEmail() != null && donation.getDonorEmail().toLowerCase().contains(query.toLowerCase())) ||
                    (donation.getMessage() != null && donation.getMessage().toLowerCase().contains(query.toLowerCase())) ||
                    donation.getId().toLowerCase().contains(query.toLowerCase()) ||
                    donation.getCampaignId().toLowerCase().contains(query.toLowerCase())
                )
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, searchResults.size());
            List<Donation> paginatedResults = searchResults.subList(start, end);

            List<Map<String, Object>> donations = paginatedResults.stream()
                .map(this::convertDonationToAdminFormat)
                .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "success", true,
                "donations", donations,
                "search", Map.of(
                    "query", query,
                    "totalResults", searchResults.size()
                ),
                "pagination", Map.of(
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) searchResults.size() / size),
                    "totalElements", searchResults.size(),
                    "size", size
                )
            );

            logger.info("Search returned {} results for query: {}", searchResults.size(), query);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error searching donations", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to search donations: " + e.getMessage()
            ));
        }
    }

    // Helper methods to convert donation objects to different formats

    /**
     * Convert donation to public format (for campaign pages)
     */
    private Map<String, Object> convertDonationToPublicFormat(Donation donation) {
        return Map.of(
            "id", donation.getId(),
            "donorName", donation.getDisplayName(),
            "amount", donation.getAmount(),
            "currency", donation.getCurrency(),
            "message", donation.getMessage() != null ? donation.getMessage() : "",
            "createdAt", donation.getCreatedAt(),
            "isAnonymous", donation.isAnonymous()
        );
    }

    /**
 * Convert donation to user format (for donor's donation history)
 */
private Map<String, Object> convertDonationToUserFormat(Donation donation) {
    Map<String, Object> userMap = new HashMap<>();
    userMap.put("id", donation.getId());
    userMap.put("campaignId", donation.getCampaignId());
    userMap.put("amount", donation.getAmount());
    userMap.put("currency", donation.getCurrency());
    userMap.put("status", donation.getPaymentStatus());
    userMap.put("message", donation.getMessage() != null ? donation.getMessage() : "");
    userMap.put("createdAt", donation.getCreatedAt());
    userMap.put("completedAt", donation.getCompletedAt());
    userMap.put("isRefunded", donation.isRefunded());
    userMap.put("refundReason", donation.getRefundReason());
    userMap.put("canRefund", donation.canBeRefunded());
    return userMap; 
}

    /**
     * Convert donation to admin format (full details for admin panel)
     */
    private Map<String, Object> convertDonationToAdminFormat(Donation donation) {
        Map<String, Object> adminMap = new HashMap<>();
        adminMap.put("id", donation.getId());
        adminMap.put("campaignId", donation.getCampaignId());
        adminMap.put("donorId", donation.getDonorId());
        adminMap.put("donorName", donation.getDonorName());
        adminMap.put("donorEmail", donation.getDonorEmail());
        adminMap.put("amount", donation.getAmount());
        adminMap.put("currency", donation.getCurrency());
        adminMap.put("status", donation.getPaymentStatus());
        adminMap.put("stripePaymentIntentId", donation.getStripePaymentIntentId());
        adminMap.put("stripeChargeId", donation.getStripeChargeId());
        adminMap.put("stripeFee", donation.getStripeFee());
        adminMap.put("platformFee", donation.getPlatformFee());
        adminMap.put("netAmount", donation.getNetAmount());
        adminMap.put("createdAt", donation.getCreatedAt());
        adminMap.put("completedAt", donation.getCompletedAt());
        adminMap.put("isRefunded", donation.isRefunded());
        adminMap.put("refundReason", donation.getRefundReason());
        adminMap.put("refundId", donation.getRefundId());
        adminMap.put("donorIpAddress", donation.getDonorIpAddress());
        adminMap.put("donorCountry", donation.getDonorCountry());
        adminMap.put("isAnonymous", donation.isAnonymous());
        adminMap.put("receiveUpdates", donation.isReceiveUpdates());
        adminMap.put("message", donation.getMessage());
        adminMap.put("timeAgo", calculateTimeAgo(donation.getCreatedAt()));
        return adminMap;
    }


    /**
     * Convert donation to detailed format (single donation view)
     */
    private Map<String, Object> convertDonationToDetailedFormat(Donation donation) {
        Map<String, Object> detailedMap = new HashMap<>();
        detailedMap.put("id", donation.getId());
        detailedMap.put("campaignId", donation.getCampaignId());
        detailedMap.put("donorName", donation.getDisplayName());
        detailedMap.put("amount", donation.getAmount());
        detailedMap.put("currency", donation.getCurrency());
        detailedMap.put("status", donation.getPaymentStatus());
        detailedMap.put("message", donation.getMessage() != null ? donation.getMessage() : "");
        detailedMap.put("createdAt", donation.getCreatedAt());
        detailedMap.put("completedAt", donation.getCompletedAt());
        detailedMap.put("isAnonymous", donation.isAnonymous());
        detailedMap.put("isRefunded", donation.isRefunded());
        detailedMap.put("refundReason", donation.getRefundReason());
        detailedMap.put("receiveUpdates", donation.isReceiveUpdates());
        
        // Fees sub-map
        Map<String, Object> feesMap = new HashMap<>();
        feesMap.put("stripeFee", donation.getStripeFee() != null ? donation.getStripeFee() : 0.0);
        feesMap.put("platformFee", donation.getPlatformFee() != null ? donation.getPlatformFee() : 0.0);
        feesMap.put("netAmount", donation.getNetAmount() != null ? donation.getNetAmount() : donation.getAmount());
        detailedMap.put("fees", feesMap);
        
        // Payment sub-map
        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("stripePaymentIntentId", donation.getStripePaymentIntentId());
        paymentMap.put("stripeChargeId", donation.getStripeChargeId());
        paymentMap.put("canRefund", donation.canBeRefunded());
        detailedMap.put("payment", paymentMap);
        
        // Metadata sub-map
        Map<String, Object> metadataMap = new HashMap<>();
        metadataMap.put("donorCountry", donation.getDonorCountry());
        metadataMap.put("timeAgo", calculateTimeAgo(donation.getCreatedAt()));
        detailedMap.put("metadata", metadataMap);
        
        return detailedMap;
    }

    /**
     * Calculate time ago in human readable format
     */
    private String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown";
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        
        long days = hours / 24;
        if (days < 7) return days + " day" + (days == 1 ? "" : "s") + " ago";
        
        long weeks = days / 7;
        if (weeks < 4) return weeks + " week" + (weeks == 1 ? "" : "s") + " ago";
        
        long months = days / 30;
        if (months < 12) return months + " month" + (months == 1 ? "" : "s") + " ago";
        
        long years = days / 365;
        return years + " year" + (years == 1 ? "" : "s") + " ago";
    }

    /**
     * Get month name from month number
     */
    private String getMonthName(Integer month) {
        String[] months = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        };
        return month >= 1 && month <= 12 ? months[month - 1] : "Unknown";
    }

    /**
     * Calculate growth rate from monthly trends
     */
    private double calculateGrowthRate(List<DonationService.MonthlyDonationTrend> trends) {
        if (trends.size() < 2) return 0.0;
        
        trends.sort((a, b) -> {
            int yearCompare = a.getYear().compareTo(b.getYear());
            return yearCompare != 0 ? yearCompare : a.getMonth().compareTo(b.getMonth());
        });
        
        DonationService.MonthlyDonationTrend earliest = trends.get(0);
        DonationService.MonthlyDonationTrend latest = trends.get(trends.size() - 1);
        
        if (earliest.getTotalAmount() == 0) return 0.0;
        
        double growth = ((latest.getTotalAmount() - earliest.getTotalAmount()) / earliest.getTotalAmount()) * 100;
        return Math.round(growth * 100.0) / 100.0; // Round to 2 decimal places
    }
}