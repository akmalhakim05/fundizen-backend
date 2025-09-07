package com.fundizen.fundizen_backend.service;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.google.api.client.util.Value;

@Service
public class StripeService {

    @Value("${STRIPE_API_KEY}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentIntent createPaymentIntent(Long amount, String currency) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount); // in cents
        params.put("currency", currency);
        params.put("payment_method_types", List.of("card"));

        return PaymentIntent.create(params);
    }
}
