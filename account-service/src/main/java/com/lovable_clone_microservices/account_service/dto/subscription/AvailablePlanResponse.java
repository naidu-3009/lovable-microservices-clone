package com.lovable_clone_microservices.account_service.dto.subscription;

public record AvailablePlanResponse(
        Long id,
        String name,
        Long unitAmountMinor,
        String currency,
        String interval,
        Long intervalCount,
        Integer maxProjects,
        Integer maxTokensPerDay,
        Integer maxPreviews,
        Boolean unlimitedAi
) {
}