package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.service.CampaignService;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin // Simple CORS - allows all origins
public class CampaignController {

    private static final Logger logger = LoggerFactory.getLogger(CampaignController.class);

    @Autowired
    private CampaignService campaignService;

    // Create a new campaign with improved error handling
    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@Valid @RequestBody Campaign campaign, BindingResult result) {
        try {
            logger.info("Received campaign creation request for: {}", campaign.getName());
            logger.debug("Campaign data: {}", campaign);
            
            // Check for validation errors
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> {
                            String fieldError = error.getField() + ": " + error.getDefaultMessage();
                            logger.warn("Validation error: {}", fieldError);
                            return fieldError;
                        })
                        .collect(Collectors.toList());
                
                logger.error("Campaign creation failed due to validation errors: {}", errors);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Validation failed",
                    "errors", errors,
                    "message", "Please fix the validation errors and try again"
                ));
            }
            
            // Additional custom validation
            if (campaign.getName() != null && campaign.getName().trim().isEmpty()) {
                logger.error("Campaign name is empty after trimming");
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Campaign name cannot be empty"
                ));
            }
            
            if (campaign.getDescription() != null && campaign.getDescription().trim().length() < 10) {
                logger.error("Campaign description too short: {} characters", campaign.getDescription().trim().length());
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Description must be at least 10 characters long"
                ));
            }
            
            // Log before creation
            logger.info("Creating campaign with valid data");
            
            Campaign createdCampaign = campaignService.createCampaign(campaign);
            
            logger.info("Campaign created successfully with ID: {}", createdCampaign.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Campaign created successfully",
                "campaign", createdCampaign,
                "status", "pending_review"
            ));
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for campaign creation: {}", e.getMessage());
            return ResponseEntity.status(400).body(Map.of(
                "error", "Invalid data provided",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error creating campaign", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "An unexpected error occurred while creating the campaign"
            ));
        }
    }

    // Get all campaigns
    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns() {
        try {
            logger.info("Fetching all campaigns");
            List<Campaign> campaigns = campaignService.getAllCampaigns();
            logger.info("Retrieved {} campaigns", campaigns.size());
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            logger.error("Error fetching all campaigns", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all active campaigns
    @GetMapping("/active")
    public ResponseEntity<List<Campaign>> getActiveCampaigns() {
        try {
            logger.info("Fetching active campaigns");
            List<Campaign> campaigns = campaignService.getActiveCampaigns();
            logger.info("Retrieved {} active campaigns", campaigns.size());
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            logger.error("Error fetching active campaigns", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get all pending campaigns
    @GetMapping("/pending")
    public ResponseEntity<List<Campaign>> getPendingCampaigns() {
        try {
            logger.info("Fetching pending campaigns");
            List<Campaign> campaigns = campaignService.getPendingCampaigns();
            logger.info("Retrieved {} pending campaigns", campaigns.size());
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            logger.error("Error fetching pending campaigns", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // Get campaign by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(@PathVariable String id) {
        try {
            logger.info("Fetching campaign with ID: {}", id);
            Campaign campaign = campaignService.getCampaignById(id);
            if (campaign == null) {
                logger.warn("Campaign not found with ID: {}", id);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Campaign not found",
                    "message", "No campaign exists with the provided ID"
                ));
            }
            logger.info("Retrieved campaign: {}", campaign.getName());
            return ResponseEntity.ok(campaign);
        } catch (Exception e) {
            logger.error("Error fetching campaign with ID: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Error fetching campaign: " + e.getMessage()
            ));
        }
    }

    // Verify a campaign
    @PostMapping("/verify/{id}")
    public ResponseEntity<?> verifyCampaign(@PathVariable String id) {
        try {
            logger.info("Verifying campaign with ID: {}", id);
            Campaign campaign = campaignService.verifyCampaign(id);
            logger.info("Campaign verified successfully: {}", campaign.getName());
            return ResponseEntity.ok(Map.of(
                "message", "Campaign verified successfully",
                "campaign", campaign
            ));
        } catch (RuntimeException e) {
            logger.error("Campaign not found for verification: {}", id);
            return ResponseEntity.status(404).body(Map.of(
                "error", "Campaign not found",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error verifying campaign: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Error verifying campaign: " + e.getMessage()
            ));
        }
    }

    // Reject a campaign
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectCampaign(@PathVariable String id) {
        try {
            logger.info("Rejecting campaign with ID: {}", id);
            Campaign campaign = campaignService.rejectCampaign(id);
            logger.info("Campaign rejected successfully: {}", campaign.getName());
            return ResponseEntity.ok(Map.of(
                "message", "Campaign rejected successfully",
                "campaign", campaign
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
                "error", "Internal server error",
                "message", "Error rejecting campaign: " + e.getMessage()
            ));
        }
    }

    // Get campaigns by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Campaign>> getCampaignsByCategory(@PathVariable String category) {
        try {
            logger.info("Fetching campaigns for category: {}", category);
            List<Campaign> campaigns = campaignService.getCampaignsByCategory(category);
            logger.info("Retrieved {} campaigns for category: {}", campaigns.size(), category);
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            logger.error("Error fetching campaigns for category: {}", category, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // Update campaign
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable String id, @Valid @RequestBody Campaign campaign, BindingResult result) {
        try {
            logger.info("Updating campaign with ID: {}", id);
            
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                
                logger.error("Campaign update failed due to validation errors: {}", errors);
                return ResponseEntity.status(400).body(Map.of(
                    "error", "Validation failed",
                    "errors", errors
                ));
            }

            Campaign updatedCampaign = campaignService.updateCampaign(id, campaign);
            if (updatedCampaign == null) {
                logger.warn("Campaign not found for update: {}", id);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Campaign not found",
                    "message", "No campaign exists with the provided ID"
                ));
            }
            
            logger.info("Campaign updated successfully: {}", updatedCampaign.getName());
            return ResponseEntity.ok(Map.of(
                "message", "Campaign updated successfully",
                "campaign", updatedCampaign
            ));
        } catch (Exception e) {
            logger.error("Error updating campaign: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Error updating campaign: " + e.getMessage()
            ));
        }
    }

    // Delete campaign
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable String id) {
        try {
            logger.info("Deleting campaign with ID: {}", id);
            boolean deleted = campaignService.deleteCampaign(id);
            if (!deleted) {
                logger.warn("Campaign not found for deletion: {}", id);
                return ResponseEntity.status(404).body(Map.of(
                    "error", "Campaign not found",
                    "message", "No campaign exists with the provided ID"
                ));
            }
            
            logger.info("Campaign deleted successfully: {}", id);
            return ResponseEntity.ok(Map.of(
                "message", "Campaign deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Error deleting campaign: {}", id, e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "message", "Error deleting campaign: " + e.getMessage()
            ));
        }
    }
}