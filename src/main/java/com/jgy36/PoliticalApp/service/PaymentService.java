package com.jgy36.PoliticalApp.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.SetupIntentCreateParams;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Create Stripe customer
     */
    public String createCustomer(String email, String name) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)
                .setName(name)
                .build();

        Customer customer = Customer.create(params);
        logger.info("Created Stripe customer: {} for email: {}", customer.getId(), email);
        return customer.getId();
    }

    /**
     * Create setup intent for payment method collection
     */
    public String createSetupIntent(String customerId) throws StripeException {
        SetupIntentCreateParams params = SetupIntentCreateParams.builder()
                .setCustomer(customerId)
                .addPaymentMethodType("card")
                .setUsage(SetupIntentCreateParams.Usage.OFF_SESSION)
                .build();

        SetupIntent setupIntent = SetupIntent.create(params);
        return setupIntent.getClientSecret();
    }

    /**
     * Create subscription
     */
    public String createSubscription(String customerId, String priceId) throws StripeException {
        return createSubscription(customerId, priceId, null, null);
    }

    /**
     * Create subscription with payment method and trial
     */
    public String createSubscription(String customerId, String priceId,
                                     String paymentMethodId, Integer trialDays) throws StripeException {

        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(priceId)
                        .build());

        if (paymentMethodId != null) {
            // Attach payment method to customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            PaymentMethodAttachParams attachParams = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build();
            paymentMethod.attach(attachParams);

            paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
        }

        if (trialDays != null && trialDays > 0) {
            paramsBuilder.setTrialPeriodDays(trialDays.longValue());
        }

        // Enable automatic tax calculation
        paramsBuilder.setAutomaticTax(SubscriptionCreateParams.AutomaticTax.builder()
                .setEnabled(true)
                .build());

        SubscriptionCreateParams params = paramsBuilder.build();
        Subscription subscription = Subscription.create(params);

        logger.info("Created Stripe subscription: {} for customer: {}",
                subscription.getId(), customerId);
        return subscription.getId();
    }

    /**
     * Update subscription
     */
    public void updateSubscription(String subscriptionId, String newPriceId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(subscription.getItems().getData().get(0).getId())
                        .setPrice(newPriceId)
                        .build())
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)                .build();

        subscription.update(params);
        logger.info("Updated Stripe subscription: {} to price: {}", subscriptionId, newPriceId);
    }

    /**
     * Cancel subscription
     */
    public void cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();

        subscription.update(params);
        logger.info("Canceled Stripe subscription: {}", subscriptionId);
    }

    /**
     * Immediately cancel subscription
     */
    public void cancelSubscriptionImmediately(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        subscription.cancel();
        logger.info("Immediately canceled Stripe subscription: {}", subscriptionId);
    }

    /**
     * Renew subscription (for failed payments)
     */
    public void renewSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);

        // This is typically handled automatically by Stripe
        // But you can manually trigger renewal logic here
        logger.info("Attempted to renew Stripe subscription: {}", subscriptionId);
    }

    /**
     * Get subscription details
     */
    public Subscription getSubscription(String subscriptionId) throws StripeException {
        return Subscription.retrieve(subscriptionId);
    }

    /**
     * Get customer details
     */
    public Customer getCustomer(String customerId) throws StripeException {
        return Customer.retrieve(customerId);
    }

    /**
     * Create payment intent for one-time purchases
     */
    public String createPaymentIntent(String customerId, long amount, String currency) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("customer", customerId);
        params.put("automatic_payment_methods", Map.of("enabled", true));

        com.stripe.model.PaymentIntent paymentIntent = com.stripe.model.PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }
}
