package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.CampaignService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    // Create a new campaign (requires verified user)
    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@RequestBody Campaign campaign) {
        try {
            // Get authenticated user from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            // Check if user is verified
            if (!currentUser.isVerified()) {
                return ResponseEntity.status(403).body("Only verified users can create campaigns");
            }
            
            // Set the creator ID to current user's ID
            campaign.setCreatorId(currentUser.getId());
            
            Campaign createdCampaign = campaignService.createCampaign(campaign);
            return ResponseEntity.ok(createdCampaign);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating campaign: " + e.getMessage());
        }
    }

    // Get all public campaigns (no authentication required)
    @GetMapping("/public")
    public ResponseEntity<List<Campaign>> getPublicCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getPublicCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all pending campaigns (admin only)
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCampaigns() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            // Check if user is admin
            if (!"admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("Admin access required");
            }
            
            List<Campaign> campaigns = campaignService.getPendingCampaigns();
            return ResponseEntity.ok(campaigns);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching pending campaigns: " + e.getMessage());
        }
    }


    // Verify a campaign (admin only)
    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verifyCampaign(@PathVariable String id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            // Check if user is admin
            if (!"admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("Admin access required");
            }
            
            Campaign campaign = campaignService.verifyCampaign(id);
            return ResponseEntity.ok(campaign);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Campaign not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying campaign: " + e.getMessage());
        }
    }

    // Reject a campaign (admin only)
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaign(@PathVariable String id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
                return ResponseEntity.status(401).body("Authentication required");
            }
            
            User currentUser = (User) authentication.getPrincipal();
            
            // Check if user is admin
            if (!"admin".equals(currentUser.getRole())) {
                return ResponseEntity.status(403).body("Admin access required");
            }
            
            Campaign campaign = campaignService.rejectCampaign(id);
            return ResponseEntity.ok(campaign);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Campaign not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error rejecting campaign: " + e.getMessage());
        }
    }
}
