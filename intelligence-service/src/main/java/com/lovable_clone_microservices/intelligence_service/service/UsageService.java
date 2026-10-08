package com.lovable_clone_microservices.intelligence_service.service;


public interface UsageService {

    void checkDailyTokensUsage();
    void recordTokenUsage(Long id, int totalTokens);

}

