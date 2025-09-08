package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.DonationService;
import com.fundizen.fundizen_backend.service.StripeService;
import com.fundizen.fundizen_backend.models.Donation;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private DonationService donationService;

    @Autowired
    private StripeService stripeService;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * Create donation and payment intent
     * POST /api/payment/donate
     */
    @PostMapping("/donate")
    public ResponseEntity<?> createDonation(@Valid @RequestBody DonationRequest request, 
                                          BindingResult result, HttpServletRequest httpRequest) {
        try {
            logger.info("Received donation request for campaign: {}, amount: {}", 
                       request.getCampaignId(), request.getAmount());

            // Validate request
            if (result.hasErrors()) {
                List<String> errors = result.getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                
                logger.warn("Donation request validation failed: {}", errors);
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Validation failed",
                    "errors", errors
                ));
            }

            // Get client IP address
            String ipAddress = getClientIpAddress(httpRequest);

            // Create donation
            DonationService.DonationResult result1 = donationService.createDonation(
                request.getCampaignId(),
                request.getDonorId(),
                request.getAmount(),
                request.getDonorName(),
                request.getDonorEmail(),
                request.getMessage(),
                request.isAnonymous(),
                request.isReceiveUpdates(),
                ipAddress
            );

            if (!result1.isSuccess()) {
                logger.error("Failed to create donation: {}", result1.getMessage());
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", result1.getMessage()
                ));
            }

            // Calculate fees for display
            StripeService.StripeFeesCalculation fees = stripeService.calculateFees(request.getAmount(), "MYR");

            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Donation created successfully",
                "donation", Map.of(
                    "id", result1.getDonation().getId(),
                    "amount", result1.getDonation().getAmount(),
                    "currency", result1.getDonation().getCurrency(),
                    "status", result1.getDonation().getPaymentStatus()
                ),
                "payment", Map.of(
                    "clientSecret", result1.getClientSecret(),
                    "paymentIntentId", result1.getDonation().getStripePaymentIntentId()
                ),
                "fees", Map.of(
                    "stripeFee", fees.getStripeFee(),
                    "platformFee", fees.getPlatformFee(),
                    "totalFees", fees.getTotalFees(),
                    "netAmount", fees.getNetAmount()
                )
            );

            logger.info("Donation created successfully: {} for campaign: {}", 
                       result1.getDonation().getId(), request.getCampaignId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing donation request", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to process donation: " + e.getMessage()
            ));
        }
    }

    /**
     * Create Stripe Checkout Session (alternative donation flow)
     * POST /api/payment/checkout-session
     */
    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutSessionRequest request) {
        try {
            logger.info("Creating checkout session for campaign: {}, amount: {}", 
                       request.getCampaignId(), request.getAmount());

            // Validate basic requirements
            if (request.getCampaignId() == null || request.getAmount() == null || 
                request.getSuccessUrl() == null || request.getCancelUrl() == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Missing required fields: campaignId, amount, successUrl, cancelUrl"
                ));
            }

            // Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("campaign_id", request.getCampaignId());
            if (request.getDonorId() != null) {
                metadata.put("donor_id", request.getDonorId());
            }

            // Create Stripe Checkout Session
            Session session = stripeService.createDonationCheckoutSession(
                request.getAmount(),
                "MYR",
                request.getCampaignId(),
                request.getSuccessUrl(),
                request.getCancelUrl(),
                request.getDonorEmail(),
                metadata
            );

            Map<String, Object> response = Map.of(
                "success", true,
                "sessionId", session.getId(),
                "sessionUrl", session.getUrl(),
                "publicKey", getStripePublishableKey()
            );

            logger.info("Checkout session created successfully: {} for campaign: {}", 
                       session.getId(), request.getCampaignId());

            return ResponseEntity.ok(response);

        } catch (StripeException e) {
            logger.error("Stripe error creating checkout session", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Payment processing error: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error creating checkout session", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to create checkout session: " + e.getMessage()
            ));
        }
    }

    /**
     * Confirm payment (for client-side confirmation)
     * POST /api/payment/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        try {
            logger.info("Confirming payment intent: {}", request.getPaymentIntentId());

            if (request.getPaymentIntentId() == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Payment intent ID is required"
                ));
            }

            // Retrieve and check payment intent status
            PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(request.getPaymentIntentId());

            if (stripeService.isPaymentSuccessful(paymentIntent)) {
                // Payment is already successful
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", "succeeded",
                    "message", "Payment completed successfully"
                ));
            } else if (stripeService.requiresAction(paymentIntent)) {
                // Payment requires additional action (3D Secure, etc.)
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", "requires_action",
                    "clientSecret", paymentIntent.getClientSecret(),
                    "message", "Payment requires additional authentication"
                ));
            } else if (stripeService.isPaymentFailed(paymentIntent)) {
                // Payment failed
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "status", "failed",
                    "message", "Payment failed"
                ));
            } else {
                // Payment is still processing
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", "processing",
                    "message", "Payment is being processed"
                ));
            }

        } catch (StripeException e) {
            logger.error("Stripe error confirming payment", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Payment confirmation error: " + e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error confirming payment", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to confirm payment: " + e.getMessage()
            ));
        }
    }

    /**
     * Create refund
     * POST /api/payment/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<?> createRefund(@RequestBody RefundRequest request) {
        try {
            logger.info("Creating refund for donation: {}", request.getDonationId());

            if (request.getDonationId() == null || request.getReason() == null) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Donation ID and reason are required"
                ));
            }

            DonationService.RefundResult result = donationService.createRefund(
                request.getDonationId(),
                request.getReason(),
                request.getAmount()
            );

            if (!result.isSuccess()) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", result.getMessage()
                ));
            }

            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Refund processed successfully",
                "refund", Map.of(
                    "donationId", result.getDonation().getId(),
                    "refundId", result.getRefundId(),
                    "amount", result.getDonation().getAmount(),
                    "status", "refunded"
                )
            );

            logger.info("Refund created successfully: {} for donation: {}", 
                       result.getRefundId(), request.getDonationId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error creating refund", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to create refund: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payment configuration
     * GET /api/payment/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getPaymentConfig() {
        try {
            Map<String, Object> config = Map.of(
                "publishableKey", getStripePublishableKey(),
                "supportedCurrencies", List.of("MYR", "USD", "SGD"),
                "supportedPaymentMethods", stripeService.getSupportedPaymentMethods(),
                "minimumAmount", 1.0,
                "maximumAmount", 100000.0,
                "currency", "MYR",
                "country", "MY"
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "config", config
            ));

        } catch (Exception e) {
            logger.error("Error fetching payment config", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to fetch payment configuration"
            ));
        }
    }

    /**
     * Calculate fees
     * POST /api/payment/calculate-fees
     */
    @PostMapping("/calculate-fees")
    public ResponseEntity<?> calculateFees(@RequestBody FeeCalculationRequest request) {
        try {
            if (request.getAmount() == null || request.getAmount() <= 0) {
                return ResponseEntity.status(400).body(Map.of(
                    "success", false,
                    "error", "Valid amount is required"
                ));
            }

            StripeService.StripeFeesCalculation fees = stripeService.calculateFees(
                request.getAmount(), 
                request.getCurrency() != null ? request.getCurrency() : "MYR"
            );

            Map<String, Object> response = Map.of(
                "success", true,
                "calculation", Map.of(
                    "originalAmount", fees.getOriginalAmount(),
                    "stripeFee", fees.getStripeFee(),
                    "platformFee", fees.getPlatformFee(),
                    "totalFees", fees.getTotalFees(),
                    "netAmount", fees.getNetAmount(),
                    "currency", request.getCurrency() != null ? request.getCurrency() : "MYR"
                )
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error calculating fees", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to calculate fees: " + e.getMessage()
            ));
        }
    }

    /**
     * Stripe webhook endpoint
     * POST /api/payment/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            logger.debug("Received Stripe webhook");

            if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
                logger.warn("Webhook secret not configured, skipping signature verification");
                return ResponseEntity.ok("Webhook secret not configured");
            }

            // Verify webhook signature
            Event event = stripeService.constructWebhookEvent(payload, sigHeader, webhookSecret);

            logger.info("Processing webhook event: {} - {}", event.getType(), event.getId());

            // Handle different event types
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                case "payment_intent.canceled":
                    handlePaymentCanceled(event);
                    break;
                case "charge.succeeded":
                    handleChargeSucceeded(event);
                    break;
                default:
                    logger.debug("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            logger.error("Error processing webhook", e);
            return ResponseEntity.status(400).body("Webhook processing failed");
        }
    }

    // Helper methods for webhook processing
    private void handlePaymentSucceeded(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                donationService.processSuccessfulPayment(paymentIntent.getId(), null);
            }
        } catch (Exception e) {
            logger.error("Error handling payment succeeded webhook", e);
        }
    }

    private void handlePaymentFailed(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                String failureReason = paymentIntent.getLastPaymentError() != null ? 
                    paymentIntent.getLastPaymentError().getMessage() : "Unknown error";
                donationService.processFailedPayment(paymentIntent.getId(), failureReason);
            }
        } catch (Exception e) {
            logger.error("Error handling payment failed webhook", e);
        }
    }

    private void handlePaymentCanceled(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            if (paymentIntent != null) {
                donationService.processFailedPayment(paymentIntent.getId(), "Payment canceled");
            }
        } catch (Exception e) {
            logger.error("Error handling payment canceled webhook", e);
        }
    }

    private void handleChargeSucceeded(Event event) {
        try {
            com.stripe.model.Charge charge = (com.stripe.model.Charge) event.getDataObjectDeserializer().getObject().orElse(null);
            if (charge != null && charge.getPaymentIntent() != null) {
                donationService.processSuccessfulPayment(charge.getPaymentIntent(), charge.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling charge succeeded webhook", e);
        }
    }

    // Utility methods
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String getStripePublishableKey() {
        // Try environment variable first
        String publishableKey = System.getenv("STRIPE_PUBLISHABLE_KEY");
        if (publishableKey != null) {
            return publishableKey;
        }
        
        // Return placeholder - in production, this should be properly configured
        return "pk_test_placeholder";
    }

    // Request DTOs
    public static class DonationRequest {
        private String campaignId;
        private String donorId;
        private String donorName;
        private String donorEmail;
        private Double amount;
        private String message;
        private boolean isAnonymous = false;
        private boolean receiveUpdates = false;

        // Getters and setters
        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        
        public String getDonorId() { return donorId; }
        public void setDonorId(String donorId) { this.donorId = donorId; }
        
        public String getDonorName() { return donorName; }
        public void setDonorName(String donorName) { this.donorName = donorName; }
        
        public String getDonorEmail() { return donorEmail; }
        public void setDonorEmail(String donorEmail) { this.donorEmail = donorEmail; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public boolean isAnonymous() { return isAnonymous; }
        public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
        
        public boolean isReceiveUpdates() { return receiveUpdates; }
        public void setReceiveUpdates(boolean receiveUpdates) { this.receiveUpdates = receiveUpdates; }
    }

    public static class CheckoutSessionRequest {
        private String campaignId;
        private String donorId;
        private String donorEmail;
        private Double amount;
        private String successUrl;
        private String cancelUrl;

        // Getters and setters
        public String getCampaignId() { return campaignId; }
        public void setCampaignId(String campaignId) { this.campaignId = campaignId; }
        
        public String getDonorId() { return donorId; }
        public void setDonorId(String donorId) { this.donorId = donorId; }
        
        public String getDonorEmail() { return donorEmail; }
        public void setDonorEmail(String donorEmail) { this.donorEmail = donorEmail; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getSuccessUrl() { return successUrl; }
        public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }
        
        public String getCancelUrl() { return cancelUrl; }
        public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }
    }

    public static class PaymentConfirmRequest {
        private String paymentIntentId;

        public String getPaymentIntentId() { return paymentIntentId; }
        public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
    }

    public static class RefundRequest {
        private String donationId;
        private String reason;
        private Double amount;

        public String getDonationId() { return donationId; }
        public void setDonationId(String donationId) { this.donationId = donationId; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
    }

    public static class FeeCalculationRequest {
        private Double amount;
        private String currency;

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
    }
}