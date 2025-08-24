package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.service.CampaignService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    // Create a new campaign (unverified by default) 
    @PostMapping("/create")
    public Campaign createCampaign(@RequestBody Campaign campaign) {
        return campaignService.createCampaign(campaign);
    }

    // Get all public campaigns
    @GetMapping("/public")
    public List<Campaign> getPublicCampaigns() {
        return campaignService.getPublicCampaigns();
    }

    // Get all pending campaigns(admin_only)
    @GetMapping("/pending")
    public List<Campaign> getPendingCampaigns() {
        return campaignService.getPendingCampaigns();
    }

    // Verify a campaign(admin_only)
    @PostMapping("/verify/{id}")
    public Campaign verifyCampaign(@PathVariable String id) {
        return campaignService.verifyCampaign(id);
    }

    // Reject a campaign(admin_only)
    @PostMapping("/reject/{id}")
    public Campaign rejectCampaign(@PathVariable String id) {
        return campaignService.rejectCampaign(id);
    }
}
