package com.lovable_clone_microservices.account_service.dto.subscription;

import com.lovable_clone_microservices.common_library.dto.PlanDto;

import java.time.Instant;

public record SubscriptionResponse(
        PlanDto plan,
        String status,
        Instant periodEnd
//        Long tokensUsedThisCycle
) {
}
