package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.service.CampaignService;

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

    // Create a new campaign (no authentication required for now)
    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@Valid @RequestBody Campaign campaign, BindingResult result) {
        try {
            // Check for validation errors
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(400).body(Map.of("errors", errors));
            }
            
            // Set a default creator ID since we don't have authentication
            if (campaign.getCreatorId() == null || campaign.getCreatorId().isEmpty()) {
                campaign.setCreatorId("default-user");
            }
            
            Campaign createdCampaign = campaignService.createCampaign(campaign);
            return ResponseEntity.ok(createdCampaign);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating campaign: " + e.getMessage());
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