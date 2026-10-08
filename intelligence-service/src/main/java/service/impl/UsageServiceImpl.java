package service.impl;


import client.AccountClient;
import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import entity.UsageLog;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import repository.UsageLogRepository;
import service.UsageService;

import java.time.LocalDate;

@RequiredArgsConstructor
@Service
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class UsageServiceImpl implements UsageService {

    UsageLogRepository usageLogRepository;
    AuthUtil authUtil;
    private final AccountClient accountClient;
    @Override
    public void checkDailyTokensUsage() {
        Long userId = authUtil.getCurrentUserId();
        PlanDto plan=accountClient.getCurrentSubscribedPlanByUser();

        LocalDate today = LocalDate.now();

            UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today).
                orElseGet(() -> createNewDailyLog(userId, today));

        if(plan.getUnlimitedAi()) return;

        int currentUsage = todayLog.getTokensUsed();
        int limit = plan.getMaxTokensPerDay();

        if(currentUsage >=  limit) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily limit reached, Upgrade now");
        }

    }

    @Override
    public void recordTokenUsage(Long userId, int actualTokens) {
        LocalDate today = LocalDate.now();

        UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today).
                orElseGet(() -> createNewDailyLog(userId, today));

        todayLog.setTokensUsed(todayLog.getTokensUsed() + actualTokens);
        usageLogRepository.save(todayLog);
    }




    private UsageLog createNewDailyLog(Long userId, LocalDate date) {
        UsageLog newLog = UsageLog.builder()
                .userId(userId)
                .date(date)
                .tokensUsed(0)
                .build();
        return usageLogRepository.save(newLog);
    }
}
