package com.fundizen.fundizen_backend.repository;

import com.fundizen.fundizen_backend.models.Campaign;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

@Repository
public interface CampaignRepository extends MongoRepository<Campaign, String> {
    
    // Existing methods
    List<Campaign> findByVerifiedTrue();
    List<Campaign> findByVerifiedFalse();
    
    // New optimized query methods
    List<Campaign> findByVerifiedTrueAndStatusAndEndDateAfter(String status, LocalDate date);
    List<Campaign> findByStatusOrderByCreatedAtDesc(String status);
    List<Campaign> findByCategoryAndVerifiedTrue(String category);
    List<Campaign> findByEndDateBefore(LocalDate date);
    List<Campaign> findByCreatorId(String creatorId);
    
    // Pagination support
    Page<Campaign> findByVerifiedTrueAndStatus(String status, Pageable pageable);
    Page<Campaign> findByCategoryAndVerifiedTrue(String category, Pageable pageable);
    
    // Custom queries for advanced filtering
    @Query("{ 'verified': true, 'status': 'approved', 'endDate': { $gte: ?0 }, 'startDate': { $lte: ?0 } }")
    List<Campaign> findActiveCampaigns(LocalDate currentDate);
    
    @Query("{ 'goalAmount': { $gte: ?0, $lte: ?1 }, 'verified': true }")
    List<Campaign> findByGoalAmountRange(Double minAmount, Double maxAmount);
}
