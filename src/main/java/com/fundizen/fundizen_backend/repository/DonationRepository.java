package com.fundizen.fundizen_backend.repository;

import com.fundizen.fundizen_backend.models.Donation;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;

@Repository
public interface DonationRepository extends MongoRepository<Donation, String> {
    // Basic queries
    List<Donation> findByCampaignId(String campaignId);
    List<Donation> findByDonorId(String donorId);
    Optional<Donation> findByStripePaymentIntentId(String paymentIntentId);
    
    // Status-based queries
    List<Donation> findByPaymentStatus(String status);
    List<Donation> findByCampaignIdAndPaymentStatus(String campaignId, String status);
    List<Donation> findByDonorIdAndPaymentStatus(String donorId, String status);
    
    // Successful donations only
    List<Donation> findByCampaignIdAndPaymentStatusOrderByCreatedAtDesc(String campaignId, String status);
    List<Donation> findByDonorIdAndPaymentStatusOrderByCreatedAtDesc(String donorId, String status);
    
    // Public donations (for display on campaign page)
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded', 'showInPublicList': true }")
    List<Donation> findPublicDonationsByCampaignId(String campaignId);
    
    // Pagination support
    Page<Donation> findByCampaignId(String campaignId, Pageable pageable);
    Page<Donation> findByDonorId(String donorId, Pageable pageable);
    Page<Donation> findByPaymentStatus(String status, Pageable pageable);
    
    // Date range queries
    List<Donation> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Donation> findByCampaignIdAndCreatedAtBetween(String campaignId, LocalDateTime startDate, LocalDateTime endDate);
    
    // Aggregation queries for statistics
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded' }")
    List<Donation> findSuccessfulDonationsByCampaignId(String campaignId);
    
    @Query("{ 'donorId': ?0, 'paymentStatus': 'succeeded' }")
    List<Donation> findSuccessfulDonationsByDonorId(String donorId);
    
    // Amount-based queries
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded', 'amount': { $gte: ?1 } }")
    List<Donation> findLargeDonationsByCampaignId(String campaignId, Double minAmount);
    
    // Recent donations
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded', 'createdAt': { $gte: ?1 } }")
    List<Donation> findRecentDonationsByCampaignId(String campaignId, LocalDateTime since);
    
    // Top donors for a campaign
    @Aggregation(pipeline = {
        "{ $match: { 'campaignId': ?0, 'paymentStatus': 'succeeded', 'donorId': { $ne: null } } }",
        "{ $group: { '_id': '$donorId', 'totalAmount': { $sum: '$amount' }, 'donationCount': { $sum: 1 } } }",
        "{ $sort: { 'totalAmount': -1 } }",
        "{ $limit: ?1 }"
    })
    List<TopDonorProjection> findTopDonorsByCampaignId(String campaignId, int limit);
    
    // Campaign donation statistics
    @Aggregation(pipeline = {
        "{ $match: { 'campaignId': ?0, 'paymentStatus': 'succeeded' } }",
        "{ $group: { '_id': null, 'totalAmount': { $sum: '$amount' }, 'donationCount': { $sum: 1 }, 'avgAmount': { $avg: '$amount' } } }"
    })
    Optional<CampaignDonationStats> getCampaignDonationStats(String campaignId);
    
    // Refund queries
    List<Donation> findByIsRefundedTrue();
    List<Donation> findByCampaignIdAndIsRefundedTrue(String campaignId);
    
    // Failed payments (for retry logic)
    @Query("{ 'paymentStatus': 'failed', 'createdAt': { $gte: ?0 } }")
    List<Donation> findRecentFailedPayments(LocalDateTime since);
    
    // Pending payments (for cleanup)
    @Query("{ 'paymentStatus': { $in: ['pending', 'processing'] }, 'createdAt': { $lt: ?0 } }")
    List<Donation> findOldPendingPayments(LocalDateTime before);
    
    // Anonymous donations
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded', 'isAnonymous': true }")
    List<Donation> findAnonymousDonationsByCampaignId(String campaignId);
    
    // Donations with messages
    @Query("{ 'campaignId': ?0, 'paymentStatus': 'succeeded', 'message': { $ne: null, $ne: '' }, 'showInPublicList': true }")
    List<Donation> findDonationsWithMessagesByCampaignId(String campaignId);
    
    // Count queries
    long countByCampaignIdAndPaymentStatus(String campaignId, String status);
    long countByDonorIdAndPaymentStatus(String donorId, String status);
    long countByPaymentStatus(String status);
    
    // Email-based queries (for guest donations)
    List<Donation> findByDonorEmailAndPaymentStatus(String email, String status);
    
    // IP-based queries (for fraud detection)
    @Query("{ 'donorIpAddress': ?0, 'createdAt': { $gte: ?1 } }")
    List<Donation> findDonationsByIpAddressSince(String ipAddress, LocalDateTime since);
    
    // Large donation alerts
    @Query("{ 'amount': { $gte: ?0 }, 'paymentStatus': 'succeeded', 'createdAt': { $gte: ?1 } }")
    List<Donation> findLargeDonationsSince(Double minAmount, LocalDateTime since);
    
    // Projection interfaces for aggregation results
    interface TopDonorProjection {
        String getDonorId();
        Double getTotalAmount();
        Long getDonationCount();
    }
    
    interface CampaignDonationStats {
        Double getTotalAmount();
        Long getDonationCount();
        Double getAvgAmount();
    }
    
    // Monthly donation statistics
    @Aggregation(pipeline = {
        "{ $match: { 'paymentStatus': 'succeeded', 'createdAt': { $gte: ?0, $lte: ?1 } } }",
        "{ $group: { '_id': { 'year': { $year: '$createdAt' }, 'month': { $month: '$createdAt' } }, 'totalAmount': { $sum: '$amount' }, 'count': { $sum: 1 } } }",
        "{ $sort: { '_id.year': 1, '_id.month': 1 } }"
    })
    List<MonthlyDonationStats> getMonthlyDonationStats(LocalDateTime startDate, LocalDateTime endDate);
    
    interface MonthlyDonationStats {
        MonthYear getId();
        Double getTotalAmount();
        Long getCount();
        
        interface MonthYear {
            Integer getYear();
            Integer getMonth();
        }
    }
}
