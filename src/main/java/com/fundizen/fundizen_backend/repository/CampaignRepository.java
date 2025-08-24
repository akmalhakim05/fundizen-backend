package com.fundizen.fundizen_backend.repository;

import com.fundizen.fundizen_backend.models.Campaign;

import org.springframework.stereotype.Repository;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String>{
    List<Campaign> findByVerifiedTrue(); // Public campaigns only
    List<Campaign> findByVerifiedFalse(); // Pending campaigns (admin only)
}
