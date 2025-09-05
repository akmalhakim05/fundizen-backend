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
@CrossOrigin(
    origins = {"http://localhost:3000", "http://127.0.0.1:3000", "https://fundizen.my"},
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS},
    allowedHeaders = "*",
    allowCredentials = "true"
)
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    // Create a new campaign (no authentication required)
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
            
            Campaign createdCampaign = campaignService.createCampaign(campaign);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign created successfully",
                "campaign", createdCampaign
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error creating campaign: " + e.getMessage()
            ));
        }
    }

    // Get all campaigns
    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getAllCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all active campaigns
    @GetMapping("/active")
    public ResponseEntity<List<Campaign>> getActiveCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getActiveCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all pending campaigns
    @GetMapping("/pending")
    public ResponseEntity<List<Campaign>> getPendingCampaigns() {
        try {
            List<Campaign> campaigns = campaignService.getPendingCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get campaign by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(@PathVariable String id) {
        try {
            Campaign campaign = campaignService.getCampaignById(id);
            if (campaign == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Campaign not found"
                ));
            }
            return ResponseEntity.ok(campaign);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error fetching campaign: " + e.getMessage()
            ));
        }
    }

    // Verify a campaign (no authentication required for simplicity)
    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verifyCampaign(@PathVariable String id) {
        try {
            Campaign campaign = campaignService.verifyCampaign(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign verified successfully",
                "campaign", campaign
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Campaign not found"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error verifying campaign: " + e.getMessage()
            ));
        }
    }

    // Reject a campaign (no authentication required for simplicity)
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaign(@PathVariable String id) {
        try {
            Campaign campaign = campaignService.rejectCampaign(id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign rejected",
                "campaign", campaign
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "error", "Campaign not found"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error rejecting campaign: " + e.getMessage()
            ));
        }
    }

    // Get campaigns by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Campaign>> getCampaignsByCategory(@PathVariable String category) {
        try {
            List<Campaign> campaigns = campaignService.getCampaignsByCategory(category);
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update campaign
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable String id, @Valid @RequestBody Campaign campaign, BindingResult result) {
        try {
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(400).body(Map.of("errors", errors));
            }

            Campaign updatedCampaign = campaignService.updateCampaign(id, campaign);
            if (updatedCampaign == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Campaign not found"
                ));
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign updated successfully",
                "campaign", updatedCampaign
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error updating campaign: " + e.getMessage()
            ));
        }
    }

    // Delete campaign
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable String id) {
        try {
            boolean deleted = campaignService.deleteCampaign(id);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", "Campaign not found"
                ));
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campaign deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Error deleting campaign: " + e.getMessage()
            ));
        }
    }
}