package com.fundizen.fundizen_backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.net.Webhook;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);
    
    // Stripe fee calculation constants
    private static final double STRIPE_PERCENTAGE_FEE = 0.034; // 3.4% for Malaysia
    private static final double STRIPE_FIXED_FEE = 1.50; // RM 1.50 fixed fee
    private static final double PLATFORM_FEE_PERCENTAGE = 0.05; // 5% platform fee (optional)

    @PostConstruct
    public void init() {
        // Initialize Stripe with API key
        String stripeSecretKey = getStripeApiKey();
        if (stripeSecretKey == null || stripeSecretKey.trim().isEmpty()) {
            logger.error("Stripe API key not found! Please set STRIPE_SECRET_KEY environment variable.");
            throw new IllegalStateException("Stripe API key is required");
        }
        
        Stripe.apiKey = stripeSecretKey;
        logger.info("Stripe service initialized successfully");
    }

    /**
     * Create a Payment Intent for donation
     */
    public PaymentIntent createDonationPaymentIntent(Double amount, String currency, String campaignId, 
                                                   String donorEmail, Map<String, String> metadata) throws StripeException {
        try {
            logger.info("Creating payment intent for campaign: {}, amount: {} {}", campaignId, amount, currency);
            
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = convertToStripeAmount(amount, currency);
            
            // Enhanced metadata
            Map<String, String> enhancedMetadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
            enhancedMetadata.put("campaign_id", campaignId);
            enhancedMetadata.put("donation_type", "campaign_donation");
            enhancedMetadata.put("platform", "fundizen");
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency.toLowerCase())
                    .setReceiptEmail(donorEmail)
                    .putAllMetadata(enhancedMetadata)
                    .setDescription("Donation to Campaign: " + campaignId)
                    .setStatementDescriptor("FUNDIZEN DONATION")
                    .setStatementDescriptorSuffix("CAMP-" + campaignId.substring(0, Math.min(campaignId.length(), 8)))
                    // Enable automatic payment methods
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                            .build()
                    )
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            
            logger.info("Payment Intent created successfully: {} for amount: {} {}", 
                       paymentIntent.getId(), amount, currency);
            
            return paymentIntent;
            
        } catch (StripeException e) {
            logger.error("Failed to create payment intent for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Create Checkout Session for donation (alternative to Payment Intent)
     */
    public Session createDonationCheckoutSession(Double amount, String currency, String campaignId,
                                               String successUrl, String cancelUrl, String donorEmail,
                                               Map<String, String> metadata) throws StripeException {
        try {
            logger.info("Creating checkout session for campaign: {}, amount: {} {}", campaignId, amount, currency);
            
            // Convert amount to cents
            long amountInCents = convertToStripeAmount(amount, currency);
            
            // Enhanced metadata
            Map<String, String> enhancedMetadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
            enhancedMetadata.put("campaign_id", campaignId);
            enhancedMetadata.put("donation_type", "campaign_donation");
            enhancedMetadata.put("platform", "fundizen");
            
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .setCustomerEmail(donorEmail)
                    .putAllMetadata(enhancedMetadata)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency.toLowerCase())
                                    .setUnitAmount(amountInCents)
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Donation to Campaign")
                                            .setDescription("Your generous donation will help make this campaign successful")
                                            .build()
                                    )
                                    .build()
                            )
                            .setQuantity(1L)
                            .build()
                    )
                    .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                            .setStatementDescriptor("FUNDIZEN DONATION")
                            .setStatementDescriptorSuffix("CAMP-" + campaignId.substring(0, Math.min(campaignId.length(), 8)))
                            .build()
                    )
                    .build();

            Session session = Session.create(params);
            
            logger.info("Checkout session created successfully: {} for campaign: {}", session.getId(), campaignId);
            
            return session;
            
        } catch (StripeException e) {
            logger.error("Failed to create checkout session for campaign: {} - Error: {}", campaignId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieve a Payment Intent
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        try {
            logger.debug("Retrieving payment intent: {}", paymentIntentId);
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve payment intent: {} - Error: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Confirm a Payment Intent (server-side confirmation)
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId, String paymentMethodId) throws StripeException {
        try {
            logger.info("Confirming payment intent: {} with payment method: {}", paymentIntentId, paymentMethodId);
            
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(paymentMethodId)
                    .build();
            
            PaymentIntent confirmedPaymentIntent = paymentIntent.confirm(params);
            
            logger.info("Payment intent confirmed successfully: {} - Status: {}", 
                       paymentIntentId, confirmedPaymentIntent.getStatus());
            
            return confirmedPaymentIntent;
            
        } catch (StripeException e) {
            logger.error("Failed to confirm payment intent: {} - Error: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Cancel a Payment Intent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId, String reason) throws StripeException {
        try {
            logger.info("Canceling payment intent: {} - Reason: {}", paymentIntentId, reason);
            
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            PaymentIntentCancelParams params = PaymentIntentCancelParams.builder()
                    .setCancellationReason(
                        reason != null ? PaymentIntentCancelParams.CancellationReason.valueOf(reason.toUpperCase()) 
                                      : PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER
                    )
                    .build();
            
            PaymentIntent canceledPaymentIntent = paymentIntent.cancel(params);
            
            logger.info("Payment intent canceled successfully: {}", paymentIntentId);
            
            return canceledPaymentIntent;
            
        } catch (StripeException e) {
            logger.error("Failed to cancel payment intent: {} - Error: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Create a refund for a successful payment
     */
    public Refund createRefund(String paymentIntentId, Double refundAmount, String reason) throws StripeException {
        try {
            logger.info("Creating refund for payment intent: {} - Amount: {}", paymentIntentId, refundAmount);
            
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId);
            
            // If refund amount is specified, convert to cents
            if (refundAmount != null) {
                long refundInCents = convertToStripeAmount(refundAmount, "MYR");
                paramsBuilder.setAmount(refundInCents);
            }
            
            // Add reason if provided
            if (reason != null && !reason.trim().isEmpty()) {
                try {
                    RefundCreateParams.Reason refundReason = RefundCreateParams.Reason.valueOf(reason.toUpperCase());
                    paramsBuilder.setReason(refundReason);
                } catch (IllegalArgumentException e) {
                    // Default to requested_by_customer if invalid reason
                    paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
                }
            }
            
            RefundCreateParams params = paramsBuilder.build();
            Refund refund = Refund.create(params);
            
            logger.info("Refund created successfully: {} for payment intent: {}", refund.getId(), paymentIntentId);
            
            return refund;
            
        } catch (StripeException e) {
            logger.error("Failed to create refund for payment intent: {} - Error: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve refund information
     */
    public Refund retrieveRefund(String refundId) throws StripeException {
        try {
            logger.debug("Retrieving refund: {}", refundId);
            return Refund.retrieve(refundId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve refund: {} - Error: {}", refundId, e.getMessage());
            throw e;
        }
    }

    /**
     * List refunds for a payment intent
     */
    public RefundCollection listRefundsForPaymentIntent(String paymentIntentId) throws StripeException {
        try {
            logger.debug("Listing refunds for payment intent: {}", paymentIntentId);
            
            RefundListParams params = RefundListParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .setLimit(100L)
                    .build();
            
            return Refund.list(params);
            
        } catch (StripeException e) {
            logger.error("Failed to list refunds for payment intent: {} - Error: {}", paymentIntentId, e.getMessage());
            throw e;
        }
    }

    /**
     * Calculate Stripe fees for a given amount
     */
    public StripeFeesCalculation calculateFees(Double amount, String currency) {
        try {
            logger.debug("Calculating fees for amount: {} {}", amount, currency);
            
            // Stripe fee calculation: percentage + fixed fee
            double stripeFee = (amount * STRIPE_PERCENTAGE_FEE) + STRIPE_FIXED_FEE;
            
            // Platform fee (optional)
            double platformFee = amount * PLATFORM_FEE_PERCENTAGE;
            
            // Net amount after all fees
            double netAmount = amount - stripeFee - platformFee;
            
            // Round to 2 decimal places
            stripeFee = BigDecimal.valueOf(stripeFee).setScale(2, RoundingMode.HALF_UP).doubleValue();
            platformFee = BigDecimal.valueOf(platformFee).setScale(2, RoundingMode.HALF_UP).doubleValue();
            netAmount = BigDecimal.valueOf(netAmount).setScale(2, RoundingMode.HALF_UP).doubleValue();
            
            return new StripeFeesCalculation(amount, stripeFee, platformFee, netAmount);
            
        } catch (Exception e) {
            logger.error("Error calculating fees for amount: {} - Error: {}", amount, e.getMessage());
            return new StripeFeesCalculation(amount, 0.0, 0.0, amount);
        }
    }

    /**
     * Get payment method details
     */
    public PaymentMethod retrievePaymentMethod(String paymentMethodId) throws StripeException {
        try {
            logger.debug("Retrieving payment method: {}", paymentMethodId);
            return PaymentMethod.retrieve(paymentMethodId);
        } catch (StripeException e) {
            logger.error("Failed to retrieve payment method: {} - Error: {}", paymentMethodId, e.getMessage());
            throw e;
        }
    }

    /**
     * Create a Customer in Stripe
     */
    public Customer createCustomer(String email, String name, Map<String, String> metadata) throws StripeException {
        try {
            logger.info("Creating Stripe customer for email: {}", email);
            
            CustomerCreateParams.Builder paramsBuilder = CustomerCreateParams.builder()
                    .setEmail(email);
            
            if (name != null && !name.trim().isEmpty()) {
                paramsBuilder.setName(name);
            }
            
            if (metadata != null && !metadata.isEmpty()) {
                paramsBuilder.putAllMetadata(metadata);
            }
            
            CustomerCreateParams params = paramsBuilder.build();
            Customer customer = Customer.create(params);
            
            logger.info("Stripe customer created successfully: {} for email: {}", customer.getId(), email);
            
            return customer;
            
        } catch (StripeException e) {
            logger.error("Failed to create Stripe customer for email: {} - Error: {}", email, e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieve customer by email
     */
    public Customer findCustomerByEmail(String email) throws StripeException {
        try {
            logger.debug("Searching for customer by email: {}", email);
            
            CustomerSearchParams params = CustomerSearchParams.builder()
                    .setQuery("email:'" + email + "'")
                    .setLimit(1L)
                    .build();
            
            CustomerSearchResult result = Customer.search(params);
            
            if (result.getData().size() > 0) {
                Customer customer = result.getData().get(0);
                logger.debug("Found existing customer: {} for email: {}", customer.getId(), email);
                return customer;
            }
            
            logger.debug("No customer found for email: {}", email);
            return null;
            
        } catch (StripeException e) {
            logger.error("Failed to search for customer by email: {} - Error: {}", email, e.getMessage());
            throw e;
        }
    }

    /**
     * Webhook signature verification
     */
    public Event constructWebhookEvent(String payload, String sigHeader, String webhookSecret) throws Exception {
        try {
            logger.debug("Constructing webhook event from payload");
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            logger.error("Failed to construct webhook event - Error: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get supported payment methods for Malaysia
     */
    public List<String> getSupportedPaymentMethods() {
        return Arrays.asList(
            "card",           // Credit/Debit cards
            "fpx",            // Malaysian online banking
            "grabpay",        // GrabPay wallet
            "paynow"          // PayNow (for Singapore users)
        );
    }

    /**
     * Validate webhook signature
     */
    public boolean isValidWebhookSignature(String payload, String sigHeader, String webhookSecret) {
        try {
            constructWebhookEvent(payload, sigHeader, webhookSecret);
            return true;
        } catch (Exception e) {
            logger.warn("Invalid webhook signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convert amount to Stripe's smallest currency unit (cents for MYR)
     */
    private long convertToStripeAmount(Double amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        
        // For MYR and most currencies, multiply by 100 to get cents
        // For zero-decimal currencies (like JPY), this would be different
        switch (currency.toUpperCase()) {
            case "JPY":
            case "KRW":
                return Math.round(amount);
            default:
                return Math.round(amount * 100);
        }
    }

    /**
     * Convert Stripe amount back to standard currency unit
     */
    public Double convertFromStripeAmount(Long stripeAmount, String currency) {
        if (stripeAmount == null) {
            return 0.0;
        }
        
        switch (currency.toUpperCase()) {
            case "JPY":
            case "KRW":
                return stripeAmount.doubleValue();
            default:
                return stripeAmount / 100.0;
        }
    }

    /**
     * Get Stripe API key from environment
     */
    private String getStripeApiKey() {
        // Try system environment first
        String apiKey = System.getenv("STRIPE_SECRET_KEY");
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            return apiKey;
        }
        
        // Try dotenv file as fallback
        try {
            Dotenv dotenv = Dotenv.load();
            return dotenv.get("STRIPE_SECRET_KEY");
        } catch (Exception e) {
            logger.warn("Could not load .env file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if payment intent requires action (like 3D Secure)
     */
    public boolean requiresAction(PaymentIntent paymentIntent) {
        return "requires_action".equals(paymentIntent.getStatus()) || 
               "requires_source_action".equals(paymentIntent.getStatus());
    }

    /**
     * Check if payment intent is successful
     */
    public boolean isPaymentSuccessful(PaymentIntent paymentIntent) {
        return "succeeded".equals(paymentIntent.getStatus());
    }

    /**
     * Check if payment intent failed
     */
    public boolean isPaymentFailed(PaymentIntent paymentIntent) {
        return "payment_failed".equals(paymentIntent.getStatus()) || 
               "canceled".equals(paymentIntent.getStatus());
    }

    /**
     * Inner class for fee calculation results
     */
    public static class StripeFeesCalculation {
        private final Double originalAmount;
        private final Double stripeFee;
        private final Double platformFee;
        private final Double netAmount;

        public StripeFeesCalculation(Double originalAmount, Double stripeFee, Double platformFee, Double netAmount) {
            this.originalAmount = originalAmount;
            this.stripeFee = stripeFee;
            this.platformFee = platformFee;
            this.netAmount = netAmount;
        }

        public Double getOriginalAmount() { return originalAmount; }
        public Double getStripeFee() { return stripeFee; }
        public Double getPlatformFee() { return platformFee; }
        public Double getNetAmount() { return netAmount; }
        public Double getTotalFees() { return stripeFee + platformFee; }

        @Override
        public String toString() {
            return String.format(
                "StripeFeesCalculation{originalAmount=%.2f, stripeFee=%.2f, platformFee=%.2f, netAmount=%.2f}",
                originalAmount, stripeFee, platformFee, netAmount
            );
        }
    }
}