package com.fundizen.fundizen_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignResponseDTO {
    private String id;
    private String creatorId;
    private String creatorUsername; // Include creator info
    private String name;
    private String category;
    private String description;
    private String imageUrl;
    private Double goalAmount;
    private Double raisedAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private boolean verified;
    private LocalDateTime createdAt;
    
    // Computed fields
    private double completionPercentage;
    private long daysRemaining;
    private boolean isActive;
    private boolean canReceiveDonations;
    
    // Helper method to create from Campaign entity
    public static CampaignResponseDTO fromCampaign(Campaign campaign, String creatorUsername) {
        return CampaignResponseDTO.builder()
                .id(campaign.getId())
                .creatorId(campaign.getCreatorId())
                .creatorUsername(creatorUsername)
                .name(campaign.getName())
                .category(campaign.getCategory())
                .description(campaign.getDescription())
                .imageUrl(campaign.getImageUrl())
                .goalAmount(campaign.getGoalAmount())
                .raisedAmount(campaign.getRaisedAmount())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .status(campaign.getStatus())
                .verified(campaign.isVerified())
                .createdAt(campaign.getCreatedAt())
                .completionPercentage(campaign.getCompletionPercentage())
                .daysRemaining(campaign.getDaysRemaining())
                .isActive(campaign.isActive())
                .canReceiveDonations(campaign.canReceiveDonations())
                .build();
    }
}