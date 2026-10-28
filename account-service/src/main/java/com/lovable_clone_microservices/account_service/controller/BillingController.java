package com.lovable_clone_microservices.account_service.controller;

import com.lovable_clone_microservices.account_service.dto.subscription.*;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.lovable_clone_microservices.account_service.services.PaymentProcessor;
import com.lovable_clone_microservices.account_service.services.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequiredArgsConstructor
//@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
@Slf4j
public class BillingController {
    private final SubscriptionService subscriptionService;
    private final PaymentProcessor paymentProcessor;
    private final PlanRepository planRepository;

    @Value("${stripe.webhook.secret}")
    String webHookSecret;


    @GetMapping("/api/me/subscription")
    public ResponseEntity<SubscriptionResponse> getMySubscription() {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription());
    }

    @PostMapping("/api/payment/checkout")
    public ResponseEntity<CheckoutResponse> createCheckoutResponse(
            @RequestBody CheckoutRequest checkoutRequest
    ) {
        return ResponseEntity.ok(paymentProcessor.createCheckoutSessionUrl(checkoutRequest));
    }

    @PostMapping("/api/payment/portal")
    public ResponseEntity<PortalResponse> openCustomerPortal() {
        return ResponseEntity.ok(paymentProcessor.openCustomerPortal());
    }


    @PostMapping("/webhooks/payment")
    public ResponseEntity<String> handlePaymentWebhooks(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, signHeader, webHookSecret);

            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;

            if (deserializer.getObject().isPresent()) { // happy case
                stripeObject = deserializer.getObject().get();
            } else {
                // Fallback: Deserialize from raw JSON
                try {
                    stripeObject = deserializer.deserializeUnsafe();
                    if (stripeObject == null) {
                        log.warn("Failed to deserialize webhook object for event: {}", event.getType());
                        return ResponseEntity.ok().build();
                    }
                } catch (Exception e) {
                    log.error("Unsafe deserialization failed for event {}: {}", event.getType(), e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Deserialization failed");
                }
            }

            // Now extract metadata only if it's a Checkout Session
            Map<String, String> metadata = new HashMap<>();
            if (stripeObject instanceof Session session) {
                metadata = session.getMetadata();
            }

            // Pass to your processor
            paymentProcessor.handleWebhookEvent(event.getType(), stripeObject, metadata);
            return ResponseEntity.ok().build();
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook: invalid signature");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Invalid Stripe signature");
        }


    }

    @GetMapping("/api/payment/plans")
    public ResponseEntity<List<AvailablePlanResponse>> getAvailablePlans() {
        List<AvailablePlanResponse> plans = new ArrayList<>();

        for (Plan plan : planRepository.findAll()) {
            if (!Boolean.TRUE.equals(plan.getActive())
                    || plan.getStripePriceId() == null
                    || plan.getStripePriceId().isBlank()) {
                continue;
            }

            try {
                Price price = Price.retrieve(plan.getStripePriceId());

                if (!Boolean.TRUE.equals(price.getActive())
                        || price.getRecurring() == null
                        || price.getUnitAmount() == null) {
                    continue;
                }

                long intervalCount =
                        price.getRecurring().getIntervalCount() == null
                                ? 1L
                                : price.getRecurring().getIntervalCount();

                plans.add(new AvailablePlanResponse(
                        plan.getId(),
                        plan.getName(),
                        price.getUnitAmount(),
                        price.getCurrency().toUpperCase(Locale.ROOT),
                        price.getRecurring().getInterval(),
                        intervalCount,
                        plan.getMaxProjects(),
                        plan.getMaxTokensPerDay(),
                        plan.getMaxPreviews(),
                        plan.getUnlimitedAi()
                ));
            } catch (StripeException e) {
                log.error(
                        "Failed to load Stripe price for plan {}",
                        plan.getId(),
                        e
                );

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Stripe pricing is temporarily unavailable",
                        e
                );
            }
        }

        return ResponseEntity.ok(plans);
    }


    /*

createCheckoutSessionUrl()
→ Ask Stripe to create Checkout.

Session.create()
→ Actual Stripe API call.

@PostMapping("/webhooks/payment")
→ Stripe's entry point into our backend.

Webhook.constructEvent()
→ Verify webhook signature + construct Event.

getDataObjectDeserializer()
→ Convert event data into Stripe Java object.

handleWebhookEvent()
→ Route event to correct handler.

handleCheckoutSessionCompleted()
→ Create/activate local subscription after checkout.

handleCustomerSubscriptionUpdated()
→ Sync changed Stripe subscription state to our DB.

handleCustomerSubscriptionDeleted()
→ Cancel local subscription.

handleInvoicePaid()
→ Renew local billing period.

handleInvoicePaymentFailed()
→ Mark local subscription PAST_DUE.

extractSubscription()
→ Get Stripe Subscription ID from Invoice.

resolvePlanId()
→ Convert Stripe Price ID → our Plan ID.

mapStripeStatusToEnum()
→ Convert Stripe status → our SubscriptionStatus.*/
}

/*                 CHECKOUT
                    │
                    ↓
             MY BACKEND → STRIPE
                    │
              Session.create()
                    │
                    ↓
             Stripe Checkout
                    │
                 USER PAYS
                    │
                    ↓
                 WEBHOOK
                    │
                    ↓
             STRIPE → MY BACKEND
                    │
                    ↓
          verify Stripe signature
                    │
                    ↓
             deserialize event
                    │
                    ↓
             get Stripe object
                    │
                    ↓
           StripePaymentProcessor
                    │
              switch(event)
                    │
       ┌────────────┼────────────┐
       ↓            ↓            ↓
    Session    Subscription    Invoice
       │            │            │
       ↓            ↓            ↓
    initial      changes      paid/failed
       │            │            │
       └────────────┼────────────┘
                    ↓
           SubscriptionService
                    │
                    ↓
              YOUR DATABASE*/