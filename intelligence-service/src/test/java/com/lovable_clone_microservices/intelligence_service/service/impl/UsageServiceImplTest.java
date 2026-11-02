package com.lovable_clone_microservices.intelligence_service.service.impl;

import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.lovable_clone_microservices.intelligence_service.client.AccountClient;
import com.lovable_clone_microservices.intelligence_service.entity.UsageLog;
import com.lovable_clone_microservices.intelligence_service.repository.UsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsageServiceImplTest {

    @Mock UsageLogRepository usageLogRepository;
    @Mock AuthUtil authUtil;
    @Mock AccountClient accountClient;

    @InjectMocks UsageServiceImpl usageService;

    @Test
    void checkDailyTokensUsage_shouldCreateDailyLogWhenOneDoesNotExist() {
        PlanDto plan = plan(false, 1000);
        when(authUtil.getCurrentUserId()).thenReturn(5L);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.empty());
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usageService.checkDailyTokensUsage();

        ArgumentCaptor<UsageLog> captor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(5L);
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.now());
        assertThat(captor.getValue().getTokensUsed()).isZero();
    }

    @Test
    void checkDailyTokensUsage_shouldAllowUsageBelowLimit() {
        PlanDto plan = plan(false, 1000);
        UsageLog log = UsageLog.builder().userId(5L).date(LocalDate.now()).tokensUsed(999).build();
        when(authUtil.getCurrentUserId()).thenReturn(5L);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.of(log));

        usageService.checkDailyTokensUsage();

        verify(usageLogRepository, never()).save(any(UsageLog.class));
    }

    @Test
    void checkDailyTokensUsage_shouldRejectUsageAtOrAboveLimit() {
        PlanDto plan = plan(false, 1000);
        UsageLog log = UsageLog.builder().userId(5L).date(LocalDate.now()).tokensUsed(1000).build();
        when(authUtil.getCurrentUserId()).thenReturn(5L);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> usageService.checkDailyTokensUsage())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void checkDailyTokensUsage_shouldAllowUnlimitedPlan() {
        PlanDto plan = plan(true, 1);
        UsageLog log = UsageLog.builder().userId(5L).date(LocalDate.now()).tokensUsed(10_000).build();
        when(authUtil.getCurrentUserId()).thenReturn(5L);
        when(accountClient.getCurrentSubscribedPlanByUser()).thenReturn(plan);
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.of(log));

        usageService.checkDailyTokensUsage();

        verify(usageLogRepository, never()).save(any(UsageLog.class));
    }

    @Test
    void recordTokenUsage_shouldCreateLogAndRecordTokensWhenNoDailyLogExists() {
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.empty());
        when(usageLogRepository.save(any(UsageLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        usageService.recordTokenUsage(5L, 125);

        ArgumentCaptor<UsageLog> captor = ArgumentCaptor.forClass(UsageLog.class);
        verify(usageLogRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().getLast().getUserId()).isEqualTo(5L);
        assertThat(captor.getAllValues().getLast().getTokensUsed()).isEqualTo(125);
    }

    @Test
    void recordTokenUsage_shouldIncrementExistingDailyLog() {
        UsageLog existing = UsageLog.builder().userId(5L).date(LocalDate.now()).tokensUsed(250).build();
        when(usageLogRepository.findByUserIdAndDate(5L, LocalDate.now())).thenReturn(Optional.of(existing));

        usageService.recordTokenUsage(5L, 75);

        assertThat(existing.getTokensUsed()).isEqualTo(325);
        verify(usageLogRepository).save(existing);
    }

    private PlanDto plan(boolean unlimited, int maxTokensPerDay) {
        PlanDto plan = new PlanDto();
        plan.setUnlimitedAi(unlimited);
        plan.setMaxTokensPerDay(maxTokensPerDay);
        return plan;
    }
}
