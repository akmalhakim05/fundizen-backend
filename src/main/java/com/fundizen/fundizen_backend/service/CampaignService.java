package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.repository.CampaignRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    public Campaign createCampaign(Campaign campaign) {
        // Use business logic methods instead of direct field access
        campaign.setStatus("pending");
        campaign.setVerified(false);
        campaign.setRaisedAmount(0.0);
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getPublicCampaigns() {
        return campaignRepository.findByVerifiedTrue();
    }

    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findByVerifiedTrueAndStatusAndEndDateAfter(
            "approved", LocalDate.now());
    }

    public List<Campaign> getPendingCampaigns() {
        return campaignRepository.findByVerifiedFalse();
    }

    public Campaign verifyCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Use business logic method
        campaign.approve();
        return campaignRepository.save(campaign);
    }

    public Campaign rejectCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        // Use business logic method
        campaign.reject();
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getCampaignsByCategory(String category) {
        return campaignRepository.findByCategoryAndVerifiedTrue(category);
    }

    public List<Campaign> getExpiredCampaigns() {
        return campaignRepository.findByEndDateBefore(LocalDate.now());
    }
}
