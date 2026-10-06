package com.lovable_clone_microservices.account_service.services.implementation;


import com.lovable_clone_microservices.account_service.dto.subscription.CheckoutRequest;
import com.lovable_clone_microservices.account_service.dto.subscription.CheckoutResponse;
import com.lovable_clone_microservices.account_service.dto.subscription.PortalResponse;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.entity.Subscription;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.PaymentProcessor;
import com.lovable_clone_microservices.account_service.services.SubscriptionService;
import com.lovable_clone_microservices.common_library.enums.SubscriptionStatus;
import com.lovable_clone_microservices.common_library.error.BadRequestException;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.stripe.exception.StripeException;

import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
//@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE->should not use as we want frontEndUrl flexible to change
/*
* In this service we are managing the events which we receive from stripe (which are just a strings at EOD) and
* we just defined few methods which should get triggered based on the type of event/string we received from webhook
* first we are running stripe cli on our local host and that listens and makes a post request to the api we mentioned and based
* on that we call methods in subscription service impl.
*
* */
public class StripePaymentProcessor implements PaymentProcessor {

    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionService subscriptionService;


    @Value("${client.url}")
    private String frontEndUrl;

    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest checkoutRequest) {
        Plan plan = planRepository.findById(checkoutRequest.planId()).orElseThrow(() -> new ResourceNotFoundException("Plan", checkoutRequest.planId().toString()));
        Long userId = authUtil.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));


        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(plan.getStripePriceId())
                                .setQuantity(1L)
                                .build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)//This tells stripe that this is not one time payment but a recurring one
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .setBillingMode(
                                        SessionCreateParams.SubscriptionData.BillingMode.builder()
                                                .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE) //this flexible allows users to subscribe anyday of month and would be charged 30days/defined period from then
                                                .build())
                                .putMetadata("user_id", userId.toString())
                                .putMetadata("plan_id", plan.getId().toString())
                                .build())
                .setSuccessUrl(frontEndUrl + "/success.html?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontEndUrl + "/cancel.html")
                .putMetadata("user_id", userId.toString())
                .putMetadata("plan_id", plan.getId().toString());
        try {
            String stripeCustomerId = user.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                params.setCustomerEmail(user.getUsername()+"@gmail.com");
            } else {
                params.setCustomer(stripeCustomerId);
            }
            Session session = Session.create(params.build());//this is where we do api call to the stripe
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed for userId={}, planId={}",
                    userId, plan.getId(), e);
            throw new RuntimeException("Unable to create checkout session", e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {
        User user=getUser(authUtil.getCurrentUserId());

        String stripeCustomerId= user.getStripeCustomerId();

        if(stripeCustomerId == null || stripeCustomerId.isEmpty()){
            throw new BadRequestException("User does not have a Stripe Customer Id,User Id "+user.getId());
        }

        try {
            var portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontEndUrl)
                            .build()
            );

            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        switch (type){
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session)stripeObject,metadata);//one-time,on checkout completion
            case "customer.subscription.updated" ->
                    handleCustomerSubscriptionUpdated((com.stripe.model.Subscription) stripeObject);
            case "customer.subscription.deleted" ->
                    handleCustomerSubscriptionDeleted((com.stripe.model.Subscription) stripeObject);
            case "invoice.paid" -> handleInvoicePaid((Invoice) stripeObject);//when invoice is paid
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) stripeObject);//when invoice is not paid ,mark as PAST_DUE
            default -> log.debug("Ignoring the event {}",type);
        }



    }
    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subId=extractSubscription(invoice);
        if(subId==null) return;
        subscriptionService.markSubscriptionPastDue(subId);
    }

    private void handleInvoicePaid(Invoice invoice) {

        String subId=extractSubscription(invoice);
        if(subId==null) return;

        try{
            com.stripe.model.Subscription subscription=com.stripe.model.Subscription.retrieve(subId);
            SubscriptionItem subscriptionItem=subscription.getItems().getData().get(0);
            Instant periodStart=toInstant(subscriptionItem.getCurrentPeriodStart());
            Instant periodEnd=toInstant(subscriptionItem.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(subId,periodStart,periodEnd);


        }
        catch (StripeException e){
            throw new RuntimeException(e);
        }




    }

    private void handleCustomerSubscriptionDeleted(
            com.stripe.model.Subscription subscription) {        if(subscription==null){
            log.error("subscription object was null");
            return;
        }



        subscriptionService.cancelSubscription(String.valueOf(subscription.getId()));

    }

    private void handleCustomerSubscriptionUpdated(
            com.stripe.model.Subscription subscription) {        if(subscription==null){
            log.error("subscription object was null inside handleCustomerSubscriptionUpdated ");
            return;
        }

        SubscriptionStatus status= mapStripeStatusToEnum(String.valueOf(subscription.getStatus()));
        if(status==null){
            log.warn("Unknown status '{}' for subscription '{}'",subscription.getStatus(),subscription.getId());
            return;
        }

        SubscriptionItem subscriptionItem=subscription.getItems().getData().get(0);
        Instant periodStart=toInstant(subscriptionItem.getCurrentPeriodStart());
        Instant periodEnd=toInstant(subscriptionItem.getCurrentPeriodEnd());

        Long planId=resolvePlanId(subscriptionItem.getPrice());

        subscriptionService.updateSubscription(
                subscription.getId(),status,periodStart,periodEnd,subscription.getCancelAtPeriodEnd(),planId
        );
    }





    private void handleCheckoutSessionCompleted(Session session,Map<String ,String> metadata) {

        if(session==null){
            log.error("session object was null inside handleCheckoutSessionCompleted");
            return;
        }

        Long userId=Long.parseLong(metadata.get("user_id"));
        Long planId=Long.parseLong(metadata.get("plan_id"));
        String subscriptionId=session.getSubscription();
        String customerId=session.getCustomer();

        User user=getUser(userId);
        if(user.getStripeCustomerId()==null){
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        subscriptionService.activateSubscription(userId,planId,subscriptionId,customerId);
    }


    //helper functions used in above functions

    public User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("user",userId.toString()));
    }

    private Long resolvePlanId(Price price) {
        if(price==null||price.getId()==null) return null;

        return planRepository.findByStripePriceId(price.getId()).map(Plan::getId).orElse(null);
    }

    private Instant toInstant(Long epoch) {
        return epoch !=null ? Instant.ofEpochSecond(epoch) : null;
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        return switch (status){
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trailing" -> SubscriptionStatus.TRAILING;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "past_due","unpaid","paused","incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            default -> {
                log.warn("Unmapped Stripe status: {}",status);
                yield null;
            }
        };
    }

    private String extractSubscription(Invoice invoice){
        var parent=invoice.getParent();
        if(parent==null) return null;

        var subDetails=parent.getSubscriptionDetails();
        if(subDetails==null) return null;

        return subDetails.getSubscription();

    }



}



