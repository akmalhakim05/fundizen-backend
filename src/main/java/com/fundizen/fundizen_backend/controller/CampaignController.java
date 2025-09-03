package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.models.User;
import com.fundizen.fundizen_backend.service.CampaignService;
import com.fundizen.fundizen_backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private AuthService authService;

    // Create a new campaign (no authentication required for now)
    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(
            @RequestHeader("Authorization") String idToken,
            @Valid @RequestBody Campaign campaign,
            BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(400).body(Map.of("errors", errors));
            }

            // Verify Firebase token
            String token = idToken.replace("Bearer ", "");
            User user = authService.verifyIdToken(token);

            if (!user.isVerified()) {
                return ResponseEntity.status(403).body("Email not verified. Please verify first.");
            }

            // Assign creator
            campaign.setCreatorId(user.getId());

            Campaign createdCampaign = campaignService.createCampaign(campaign);
            return ResponseEntity.ok(createdCampaign);

        } catch (Exception e) {
            return ResponseEntity.status(401).body("Unauthorized: " + e.getMessage());
        }
    }

    // Get all public campaigns
    @GetMapping("/public")
    public ResponseEntity<List<Campaign>> getPublicCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getPublicCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all pending campaigns (no authentication required for now)
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getPendingCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching pending campaigns: " + e.getMessage());
        }
    }

    // Verify a campaign (no authentication required for now)
    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verifyCampaign(@PathVariable String id) {
        try {
            Campaign campaign = campaignService.verifyCampaign(id);
            return ResponseEntity.ok(campaign);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Campaign not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error verifying campaign: " + e.getMessage());
        }
    }

    // Reject a campaign (no authentication required for now)
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaign(@PathVariable String id) {
        try {
            Campaign campaign = campaignService.rejectCampaign(id);
            return ResponseEntity.ok(campaign);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Campaign not found");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error rejecting campaign: " + e.getMessage());
        }
    }
}