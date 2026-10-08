package service;


public interface UsageService {

    void checkDailyTokensUsage();
    void recordTokenUsage(Long id, int totalTokens);

}

