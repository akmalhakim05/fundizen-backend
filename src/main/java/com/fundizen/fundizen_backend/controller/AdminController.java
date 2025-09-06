package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.CampaignService;
import com.fundizen.fundizen_backend.service.UserService;
import com.fundizen.fundizen_backend.dto.CampaignResponseDTO;
import com.fundizen.fundizen_backend.dto.UserResponseDTO;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin // Simple CORS - allows all origins
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private UserService userService;

    // ===== DASHBOARD ENDPOINTS =====

    /**
     * Get admin dashboard statistics
     * GET /api/admin/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        try {
            logger.info("Fetching admin dashboard statistics");
            
            // Campaign statistics
            List<Campaign> allCampaigns = campaignService.getAllCampaigns();
            List<Campaign> pendingCampaigns = campaignService.getPendingCampaigns();
            List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();
            
            long totalCampaigns = allCampaigns.size();
            long pendingCount = pendingCampaigns.size();
            long activeCount = activeCampaigns.size();
            long approvedCount = allCampaigns.stream()
                .filter(c -> "approved".equals(c.getStatus()))
                .count();
            long rejectedCount = allCampaigns.stream()
                .filter(c -> "rejected".equals(c.getStatus()))
                .count();

            // User statistics
            long totalUsers = userService.getTotalUsers();
            long totalAdmins = userService.getTotalAdmins();
            long verifiedUsers = userService.getVerifiedUsersCount();
            long unverifiedUsers = userService.getUnverifiedUsersCount();

            // Recent activity
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            List<User> recentUsers = userService.getRecentUsers(weekAgo);

            Map<String, Object> stats = Map.of(
                "campaigns", Map.of(
                    "total", totalCampaigns,
                    "pending", pendingCount,
                    "active", activeCount,
                    "approved", approvedCount,
                    "rejected", rejectedCount
                ),
                "users", Map.of(
                    "total", totalUsers,
                    "admins", totalAdmins,
                    "verified", verifiedUsers,
                    "unverified", unverifiedUsers,
                    "recentSignups", recentUsers.size()
                ),
                "activity", Map.of(
                    "recentUsersCount", recentUsers.size(),
                    "pendingApprovals", pendingCount
                ),
                "timestamp", LocalDateTime.now()
            );

            logger.info("Dashboard stats compiled successfully");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error fetching dashboard statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch dashboard statistics",
                "message", e.getMessage()
            ));
        }
    }

    // ===== CAMPAIGN MANAGEMENT ENDPOINTS =====

    /**
     * Get all campaigns for admin review (with pagination)
     * GET /api/admin/campaigns
     */
    @GetMapping("/campaigns")
    public ResponseEntity<?> getAllCampaignsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        try {
            logger.info("Admin fetching campaigns - page: {}, size: {}, status: {}, category: {}", 
                       page, size, status, category);

            List<Campaign> campaigns = campaignService.getAllCampaigns();
            
            // Filter by status if provided
            if (status != null && !status.isEmpty()) {
                campaigns = campaigns.stream()
                    .filter(c -> status.equals(c.getStatus()))
                    .collect(Collectors.toList());
            }
            
            // Filter by category if provided
            if (category != null && !category.isEmpty()) {
                campaigns = campaigns.stream()
                    .filter(c -> category.equals(c.getCategory()))
                    .collect(Collectors.toList());
            }

            // Sort campaigns
            if ("desc".equals(sortDir)) {
                campaigns.sort((a, b) -> {
                    if ("createdAt".equals(sortBy)) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    } else if ("name".equals(sortBy)) {
                        return b.getName().compareTo(a.getName());
                    } else if ("goalAmount".equals(sortBy)) {
                        return b.getGoalAmount().compareTo(a.getGoalAmount());
                    }
                    return 0;
                });
            } else {
                campaigns.sort((a, b) -> {
                    if ("createdAt".equals(sortBy)) {
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    } else if ("name".equals(sortBy)) {
                        return a.getName().compareTo(b.getName());
                    } else if ("goalAmount".equals(sortBy)) {
                        return a.getGoalAmount().compareTo(b.getGoalAmount());
                    }
                    return 0;
                });
            }

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, campaigns.size());
            List<Campaign> paginatedCampaigns = campaigns.subList(start, end);

            // Convert to response DTOs
            List<CampaignResponseDTO> campaignDTOs = paginatedCampaigns.stream()
                .map(campaign -> {
                    User creator = userService.getUserById(campaign.getCreatorId());
                    String creatorUsername = creator != null ? creator.getUsername() : "Unknown";
                    return CampaignResponseDTO.fromCampaign(campaign, creatorUsername);
                })
                .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "campaigns", campaignDTOs,
                "pagination", Map.of(
                    "currentPage", page,
                    "totalPages", (int) Math.ceil((double) campaigns.size() / size),
                    "totalElements", campaigns.size(),
                    "size", size
                ),
                "filters", Map.of(
                    "status", status != null ? status : "all",
                    "category", category != null ? category : "all"
                )
            );

            logger.info("Retrieved {} campaigns for admin review", campaignDTOs.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching campaigns for admin", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch campaigns",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get pending campaigns for approval
     * GET /api/admin/campaigns/pending
     */
    @GetMapping("/campaigns/pending")
    public ResponseEntity<?> getPendingCampaigns() {
        try {
            logger.info("Admin fetching pending campaigns");
            
            List<Campaign> pendingCampaigns = campaignService.getPendingCampaigns();
            
            List<CampaignResponseDTO> campaignDTOs = pendingCampaigns.stream()
                .map(campaign -> {
                    User creator = userService.getUserById(campaign.getCreatorId());
                    String creatorUsername = creator != null ? creator.getUsername() : "Unknown";
                    return CampaignResponseDTO.fromCampaign(campaign, creatorUsername);
                })
                .collect(Collectors.toList());

            logger.info("Retrieved {} pending campaigns", campaignDTOs.size());
            return ResponseEntity.ok(Map.of(
                "campaigns", campaignDTOs,
                "count", campaignDTOs.size()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching pending campaigns", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch pending campaigns",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Approve a campaign
     * POST /api/admin/campaigns/{id}/approve
     */
    @PostMapping("/campaigns/{id}/approve")
    public ResponseEntity<?> approveCampaign(@PathVariable String id, @RequestBody(required = false) Map<String, String> request) {
        try {
            logger.info("Admin approving campaign: {}", id);
            
            Campaign campaign = campaignService.verifyCampaign(id);
            
            User creator = userService.getUserById(campaign.getCreatorId());
            String creatorUsername = creator != null ? creator.getUsername() : "Unknown";
            
            CampaignResponseDTO campaignDTO = CampaignResponseDTO.fromCampaign(campaign, creatorUsername);
            
            logger.info("Campaign approved successfully: {} by creator: {}", campaign.getName(), creatorUsername);
            
            return ResponseEntity.ok(Map.of(
                "message", "Campaign approved successfully",
                "campaign", campaignDTO,
                "action", "approved",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (RuntimeException e) {
            logger.error("Campaign not found for approval: {}", id);
            return ResponseEntity.status(404).body(Map.of(
                "error", "Campaign not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error approving campaign: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to approve campaign",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Reject a campaign
     * POST /api/admin/campaigns/{id}/reject
     */
    @PostMapping("/campaigns/{id}/reject")
    public ResponseEntity<?> rejectCampaign(@PathVariable String id, @RequestBody(required = false) Map<String, String> request) {
        try {
            logger.info("Admin rejecting campaign: {}", id);
            
            String reason = request != null ? request.get("reason") : "No reason provided";
            
            Campaign campaign = campaignService.rejectCampaign(id);
            
            // Update rejection reason if provided
            if (reason != null && !reason.trim().isEmpty()) {
                campaign.setRejectionReason(reason);
                // You might want to save this - depends on your CampaignService implementation
            }
            
            User creator = userService.getUserById(campaign.getCreatorId());
            String creatorUsername = creator != null ? creator.getUsername() : "Unknown";
            
            CampaignResponseDTO campaignDTO = CampaignResponseDTO.fromCampaign(campaign, creatorUsername);
            
            logger.info("Campaign rejected successfully: {} by creator: {}, reason: {}", 
                       campaign.getName(), creatorUsername, reason);
            
            return ResponseEntity.ok(Map.of(
                "message", "Campaign rejected successfully",
                "campaign", campaignDTO,
                "action", "rejected",
                "reason", reason,
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (RuntimeException e) {
            logger.error("Campaign not found for rejection: {}", id);
            return ResponseEntity.status(404).body(Map.of(
                "error", "Campaign not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error rejecting campaign: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to reject campaign",
                "message", e.getMessage()
            ));
        }
    }

    // ===== USER MANAGEMENT ENDPOINTS =====

    /**
     * Get all users for admin management (with pagination)
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsersForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String verified,
            @RequestParam(required = false) String search) {
        try {
            logger.info("Admin fetching users - page: {}, size: {}, role: {}, verified: {}, search: {}", 
                       page, size, role, verified, search);

            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> userPage = userService.getAllUsers(pageable);
            
            List<User> users = userPage.getContent();
            
            // Apply filters
            if (role != null && !role.isEmpty() && !"all".equals(role)) {
                users = users.stream()
                    .filter(u -> role.equals(u.getRole()))
                    .collect(Collectors.toList());
            }
            
            if (verified != null && !verified.isEmpty() && !"all".equals(verified)) {
                boolean isVerified = "true".equals(verified);
                users = users.stream()
                    .filter(u -> u.isVerified() == isVerified)
                    .collect(Collectors.toList());
            }
            
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase().trim();
                users = users.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(searchLower) ||
                               u.getEmail().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
            }

            // Convert to response DTOs and remove passwords
            List<UserResponseDTO> userDTOs = users.stream()
                .map(user -> {
                    user.setPassword(null); // Remove password for security
                    return UserResponseDTO.fromUser(user);
                })
                .collect(Collectors.toList());

            Map<String, Object> response = Map.of(
                "users", userDTOs,
                "pagination", Map.of(
                    "currentPage", userPage.getNumber(),
                    "totalPages", userPage.getTotalPages(),
                    "totalElements", userPage.getTotalElements(),
                    "size", userPage.getSize()
                ),
                "filters", Map.of(
                    "role", role != null ? role : "all",
                    "verified", verified != null ? verified : "all",
                    "search", search != null ? search : ""
                )
            );

            logger.info("Retrieved {} users for admin management", userDTOs.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error fetching users for admin", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch users",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get user details by ID
     * GET /api/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserDetails(@PathVariable String id) {
        try {
            logger.info("Admin fetching user details: {}", id);
            
            User user = userService.getUserById(id);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "User not found",
                    "message", "No user exists with the provided ID"
                ));
            }
            
            // Remove password for security
            user.setPassword(null);
            
            // Get user's campaigns
            List<Campaign> userCampaigns = campaignService.getAllCampaigns().stream()
                .filter(c -> id.equals(c.getCreatorId()))
                .collect(Collectors.toList());
            
            Map<String, Object> userDetails = Map.of(
                "user", UserResponseDTO.fromUser(user),
                "campaigns", userCampaigns.stream()
                    .map(campaign -> CampaignResponseDTO.fromCampaign(campaign, user.getUsername()))
                    .collect(Collectors.toList()),
                "statistics", Map.of(
                    "totalCampaigns", userCampaigns.size(),
                    "activeCampaigns", userCampaigns.stream()
                        .filter(Campaign::isActive)
                        .count(),
                    "approvedCampaigns", userCampaigns.stream()
                        .filter(c -> "approved".equals(c.getStatus()))
                        .count()
                )
            );
            
            logger.info("Retrieved user details for: {}", user.getUsername());
            return ResponseEntity.ok(userDetails);
            
        } catch (Exception e) {
            logger.error("Error fetching user details: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch user details",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Promote user to admin
     * POST /api/admin/users/{id}/promote
     */
    @PostMapping("/users/{id}/promote")
    public ResponseEntity<?> promoteUserToAdmin(@PathVariable String id) {
        try {
            logger.info("Admin promoting user to admin: {}", id);
            
            User user = userService.promoteToAdmin(id);
            user.setPassword(null); // Remove password for security
            
            logger.info("User promoted to admin successfully: {}", user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "message", "User promoted to admin successfully",
                "user", UserResponseDTO.fromUser(user),
                "action", "promoted",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (RuntimeException e) {
            logger.error("User not found for promotion: {}", id);
            return ResponseEntity.status(404).body(Map.of(
                "error", "User not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error promoting user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to promote user",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Demote admin to regular user
     * POST /api/admin/users/{id}/demote
     */
    @PostMapping("/users/{id}/demote")
    public ResponseEntity<?> demoteAdminToUser(@PathVariable String id) {
        try {
            logger.info("Admin demoting user: {}", id);
            
            User user = userService.demoteToUser(id);
            user.setPassword(null); // Remove password for security
            
            logger.info("User demoted to regular user successfully: {}", user.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "message", "User demoted to regular user successfully",
                "user", UserResponseDTO.fromUser(user),
                "action", "demoted",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (RuntimeException e) {
            logger.error("User not found for demotion: {}", id);
            return ResponseEntity.status(404).body(Map.of(
                "error", "User not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error demoting user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to demote user",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Delete user (admin only)
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            logger.info("Admin deleting user: {}", id);
            
            boolean deleted = userService.deleteUser(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of(
                    "error", "User not found",
                    "message", "No user exists with the provided ID"
                ));
            }
            
            logger.info("User deleted successfully: {}", id);
            
            return ResponseEntity.ok(Map.of(
                "message", "User deleted successfully",
                "action", "deleted",
                "timestamp", LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            logger.error("Error deleting user: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to delete user",
                "message", e.getMessage()
            ));
        }
    }

    // ===== SYSTEM MANAGEMENT ENDPOINTS =====

    /**
     * Get system statistics
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStatistics() {
        try {
            logger.info("Admin fetching system statistics");
            
            // Comprehensive system stats
            List<Campaign> allCampaigns = campaignService.getAllCampaigns();
            List<User> allUsers = userService.getAllUsers();
            
            // Campaign analytics
            Map<String, Long> campaignsByStatus = allCampaigns.stream()
                .collect(Collectors.groupingBy(Campaign::getStatus, Collectors.counting()));
            
            Map<String, Long> campaignsByCategory = allCampaigns.stream()
                .collect(Collectors.groupingBy(Campaign::getCategory, Collectors.counting()));
            
            // User analytics
            Map<String, Long> usersByRole = allUsers.stream()
                .collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
            
            // Financial stats
            double totalGoalAmount = allCampaigns.stream()
                .mapToDouble(Campaign::getGoalAmount)
                .sum();
            
            double totalRaisedAmount = allCampaigns.stream()
                .mapToDouble(Campaign::getRaisedAmount)
                .sum();

            Map<String, Object> stats = Map.of(
                "overview", Map.of(
                    "totalCampaigns", allCampaigns.size(),
                    "totalUsers", allUsers.size(),
                    "totalGoalAmount", totalGoalAmount,
                    "totalRaisedAmount", totalRaisedAmount,
                    "platformSuccessRate", totalGoalAmount > 0 ? (totalRaisedAmount / totalGoalAmount) * 100 : 0
                ),
                "campaigns", Map.of(
                    "byStatus", campaignsByStatus,
                    "byCategory", campaignsByCategory
                ),
                "users", Map.of(
                    "byRole", usersByRole,
                    "verificationStats", Map.of(
                        "verified", userService.getVerifiedUsersCount(),
                        "unverified", userService.getUnverifiedUsersCount()
                    )
                ),
                "timestamp", LocalDateTime.now()
            );

            logger.info("System statistics compiled successfully");
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error fetching system statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch system statistics",
                "message", e.getMessage()
            ));
        }
    }
}