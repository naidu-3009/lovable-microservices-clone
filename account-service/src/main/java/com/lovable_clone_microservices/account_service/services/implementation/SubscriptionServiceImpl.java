package com.lovable_clone_microservices.account_service.services.implementation;


import com.lovable_clone_microservices.account_service.dto.subscription.SubscriptionResponse;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.entity.Subscription;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.mapper.SubscriptionMapper;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.lovable_clone_microservices.account_service.repository.SubscriptionRespository;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.SubscriptionService;
import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.enums.SubscriptionStatus;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AuthUtil authUtil;
    private final SubscriptionRespository subscriptionRespository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private  final Integer FREE_TIER_PROJECTS_ALLOWED=100;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId=authUtil.getCurrentUserId();
        var currentSubscription= subscriptionRespository.findByUserIdAndStatusIn(userId, Set.of(
                SubscriptionStatus.ACTIVE,SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRAILING
        )).orElse(
                new Subscription()
        );
        System.out.println("SUBSCRIPTION FOUND = " + currentSubscription.getStatus());
        return subscriptionMapper.toSubscriptionResponse(currentSubscription);
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {
        boolean exists=subscriptionRespository.existsByStripeSubscriptionId(subscriptionId);
        if(exists) return;
        User user=getUser(userId);
        Plan plan=getPlan(planId);
        Subscription subscription=Subscription.builder().user(user).plan(plan).stripeSubscriptionId(subscriptionId).status(SubscriptionStatus.INCOMPLETE).build();
        subscriptionRespository.save(subscription);
    }

    @Override
    @Transactional
    public void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        boolean hasUpdated=false;

        if(status!=null || status != subscription.getStatus()){
            subscription.setStatus(status);
            hasUpdated=true;
        }

        if(periodStart != null &&   !periodStart.equals(subscription.getCurrentPeriodStart())){
            subscription.setCurrentPeriodStart(periodStart);
            hasUpdated=true;
        }

        if(periodEnd != null &&   !periodEnd.equals(subscription.getCurrentPeriodEnd())){
            subscription.setCurrentPeriodEnd(periodEnd);
            hasUpdated=true;
        }

        if(cancelAtPeriodEnd !=null && cancelAtPeriodEnd != subscription.getCancelAtPeriodEnd()){
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            hasUpdated=true;
        }

        if(planId!=null && !planId.equals(subscription.getPlan().getId())){
            Plan newPlan=getPlan(planId);
            subscription.setPlan(newPlan);
            hasUpdated=true;
        }

        if(hasUpdated){
            log.debug("Subscription object has been updated for the id : {}",gatewaySubscriptionId);
            subscriptionRespository.save(subscription);
        }
    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        Subscription subscription=getSubscription(gatewaySubscriptionId);
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRespository.save(subscription);
    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {
            Subscription subscription=getSubscription(gatewaySubscriptionId);
            Instant newStart=periodStart !=null ?periodStart : subscription.getCurrentPeriodEnd();
            subscription.setCurrentPeriodStart(newStart);
            subscription.setCurrentPeriodEnd(periodEnd);
            if(subscription.getStatus()== SubscriptionStatus.PAST_DUE || subscription.getStatus()==SubscriptionStatus.INCOMPLETE){
                subscription.setStatus(SubscriptionStatus.ACTIVE);
            }
            subscriptionRespository.save(subscription);
    }

    @Override
    public void markSubscriptionPastDue(String gatewaySubscriptionId) {
        Subscription subscription=subscriptionRespository.findByStripeSubscriptionId(gatewaySubscriptionId).orElseThrow(()->new ResourceNotFoundException("Subscription",gatewaySubscriptionId));
        if(subscription.getStatus()==SubscriptionStatus.PAST_DUE){
            log.debug("Subscription is already past due , gatewaySubscriptionId : {}",gatewaySubscriptionId);
            return;
        }
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRespository.save(subscription);
/*
* can notify user regarding due from here
* */
    }

    @Override
    public PlanDto getCurrentSubscribedPlanByUser() {

        SubscriptionResponse subscriptionResponse =getCurrentSubscription();

        return subscriptionResponse.plan();
    }




    //Internal Methods

    private User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("User",userId.toString()));
    }

    private Plan getPlan(Long planId){
        return planRepository.findById(planId).orElseThrow(()->new ResourceNotFoundException("Plan",planId.toString()));
    }


    private Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRespository.findByStripeSubscriptionId(gatewaySubscriptionId).orElseThrow(() -> new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }
}
