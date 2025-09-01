package com.fundizen.fundizen_backend.service;

import com.fundizen.fundizen_backend.models.Campaign;
import com.fundizen.fundizen_backend.repository.CampaignRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;

    public Campaign createCampaign(Campaign campaign) {
        campaign.setStatus("pending");
        campaign.setVerified(false);
        campaign.setRaisedAmount(0.0);
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    public List<Campaign> getPublicCampaigns() {
        return campaignRepository.findByVerifiedTrue();
    }

    public List<Campaign> getActiveCampaigns() {
        return campaignRepository.findByVerifiedTrueAndStatusAndEndDateAfter(
            "approved", LocalDate.now());
    }

    public List<Campaign> getPendingCampaigns() {
        return campaignRepository.findByStatusOrderByCreatedAtDesc("pending");
    }

    public Campaign getCampaignById(String id) {
        Optional<Campaign> campaign = campaignRepository.findById(id);
        return campaign.orElse(null);
    }

    public Campaign verifyCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        campaign.approve();
        return campaignRepository.save(campaign);
    }

    public Campaign rejectCampaign(String campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found"));

        campaign.reject();
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getCampaignsByCategory(String category) {
        return campaignRepository.findByCategoryAndVerifiedTrue(category);
    }

    public List<Campaign> getExpiredCampaigns() {
        return campaignRepository.findByEndDateBefore(LocalDate.now());
    }

    public Campaign updateCampaign(String id, Campaign updatedCampaign) {
        Optional<Campaign> existingCampaign = campaignRepository.findById(id);
        if (existingCampaign.isPresent()) {
            Campaign campaign = existingCampaign.get();
            
            // Update fields
            campaign.setName(updatedCampaign.getName());
            campaign.setCategory(updatedCampaign.getCategory());
            campaign.setDescription(updatedCampaign.getDescription());
            campaign.setImageUrl(updatedCampaign.getImageUrl());
            campaign.setGoalAmount(updatedCampaign.getGoalAmount());
            campaign.setStartDate(updatedCampaign.getStartDate());
            campaign.setEndDate(updatedCampaign.getEndDate());
            campaign.setDocumentUrl(updatedCampaign.getDocumentUrl());
            
            return campaignRepository.save(campaign);
        }
        return null;
    }

    public boolean deleteCampaign(String id) {
        Optional<Campaign> campaign = campaignRepository.findById(id);
        if (campaign.isPresent()) {
            campaignRepository.deleteById(id);
            return true;
        }
        return false;
    }
}