package com.lovable_clone_microservices.account_service.services;



import com.lovable_clone_microservices.account_service.dto.subscription.SubscriptionResponse;
import com.lovable_clone_microservices.common_library.enums.SubscriptionStatus;

import java.time.Instant;

public interface SubscriptionService {
     SubscriptionResponse getCurrentSubscription();

    void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId);

    void updateSubscription(String gatewaySubscriptionId, SubscriptionStatus status, Instant periodStart, Instant periodEnd, Boolean cancelAtPeriodEnd, Long planId);

    void cancelSubscription(String gatewaySubscriptionId);

    void renewSubscriptionPeriod(String subscriptionId, Instant periodStart, Instant periodEnd);

    void markSubscriptionPastDue(String subscriptionId);

}
