package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.repository.CampaignRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    public Campaign createCampaign(Campaign campaign) {
        // Default to unverified
        campaign.setVerified(false);
        campaign.setStatus("pending");
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getPublicCampaigns() {
        return campaignRepository.findByVerifiedTrue();
    }

    public List<Campaign> getPendingCampaigns() {
        return campaignRepository.findByVerifiedFalse();
    }

    public Campaign verifyCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        campaign.setVerified(true);
        campaign.setStatus("approved");
        return campaignRepository.save(campaign);
    }

    public Campaign rejectCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        campaign.setVerified(false);
        campaign.setStatus("rejected");
        return campaignRepository.save(campaign);
    }
}
