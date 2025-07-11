package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Autowired
    private SubscriptionService subscriptionService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            logger.info("Received Stripe webhook: {}", event.getType());

            switch (event.getType()) {
                case "customer.subscription.created":
                case "customer.subscription.updated":
                    handleSubscriptionUpdate(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handlePaymentFailed(event);
                    break;
                default:
                    logger.info("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (SignatureVerificationException e) {
            logger.error("Invalid webhook signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid signature");
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }

    private void handleSubscriptionUpdate(Event event) {
        try {
            Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (stripeSubscription != null) {
                subscriptionService.processStripeWebhook(
                        event.getType(),
                        stripeSubscription.getId(),
                        stripeSubscription
                );
            }
        } catch (Exception e) {
            logger.error("Error handling subscription update: {}", e.getMessage());
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        try {
            Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer()
                    .getObject().orElse(null);

            if (stripeSubscription != null) {
                subscriptionService.processStripeWebhook(
                        event.getType(),
                        stripeSubscription.getId(),
                        stripeSubscription
                );
            }
        } catch (Exception e) {
            logger.error("Error handling subscription deletion: {}", e.getMessage());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        try {
            // Extract subscription ID from invoice
            var invoice = event.getDataObjectDeserializer().getObject().orElse(null);
            // Process payment success
            logger.info("Payment succeeded for invoice");
        } catch (Exception e) {
            logger.error("Error handling payment success: {}", e.getMessage());
        }
    }

    private void handlePaymentFailed(Event event) {
        try {
            // Extract subscription ID from invoice
            var invoice = event.getDataObjectDeserializer().getObject().orElse(null);
            // Process payment failure
            logger.warn("Payment failed for invoice");
        } catch (Exception e) {
            logger.error("Error handling payment failure: {}", e.getMessage());
        }
    }
}
