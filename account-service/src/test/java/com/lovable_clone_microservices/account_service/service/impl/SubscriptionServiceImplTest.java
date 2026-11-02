package com.lovable_clone_microservices.account_service.service.impl;

import com.lovable_clone_microservices.account_service.dto.subscription.SubscriptionResponse;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.entity.Subscription;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.mapper.SubscriptionMapper;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.lovable_clone_microservices.account_service.repository.SubscriptionRespository;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.implementation.SubscriptionServiceImpl;
import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.enums.SubscriptionStatus;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    AuthUtil authUtil;
    @Mock
    SubscriptionRespository subscriptionRespository;
    @Mock
    SubscriptionMapper subscriptionMapper;
    @Mock
    UserRepository userRepository;
    @Mock
    PlanRepository planRepository;

    @InjectMocks
    SubscriptionServiceImpl subscriptionService;

    @Test
    void activateSubscription_shouldCreateIncompleteSubscription() {
        User user = User.builder().id(10L).username("n@example.com").build();
        Plan plan = new Plan();
        plan.setId(3L);
        when(subscriptionRespository.existsByStripeSubscriptionId("sub_new")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));

        subscriptionService.activateSubscription(10L, 3L, "sub_new", "cus_10");

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRespository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getPlan()).isSameAs(plan);
        assertThat(captor.getValue().getStripeSubscriptionId()).isEqualTo("sub_new");
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.INCOMPLETE);
    }

    @Test
    void activateSubscription_shouldIgnoreDuplicateStripeSubscription() {
        when(subscriptionRespository.existsByStripeSubscriptionId("sub_existing")).thenReturn(true);

        subscriptionService.activateSubscription(10L, 3L, "sub_existing", "cus_10");

        verifyNoInteractions(userRepository, planRepository);
        verify(subscriptionRespository, never()).save(any(Subscription.class));
    }

    @Test
    void activateSubscription_shouldFailWhenUserIsMissing() {
        when(subscriptionRespository.existsByStripeSubscriptionId("sub_new")).thenReturn(false);
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.activateSubscription(10L, 3L, "sub_new", "cus_10"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(planRepository, never()).findById(any());
        verify(subscriptionRespository, never()).save(any(Subscription.class));
    }

    @Test
    void updateSubscription_shouldUpdateOnlyProvidedChangedFields() {
        Plan existingPlan = new Plan();
        existingPlan.setId(3L);
        Subscription subscription = Subscription.builder()
                .stripeSubscriptionId("sub_1")
                .plan(existingPlan)
                .status(SubscriptionStatus.INCOMPLETE)
                .cancelAtPeriodEnd(false)
                .build();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-02-01T00:00:00Z");
        when(subscriptionRespository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(subscription));

        subscriptionService.updateSubscription(
                "sub_1", SubscriptionStatus.ACTIVE, start, end, true, null);

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(start);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(end);
        assertThat(subscription.getCancelAtPeriodEnd()).isTrue();
        verify(subscriptionRespository).save(subscription);
    }

    @Test
    void updateSubscription_shouldNotSaveWhenNothingChanged() {
        Subscription subscription = Subscription.builder()
                .stripeSubscriptionId("sub_1")
                .plan(planWithId(3L))
                .status(SubscriptionStatus.ACTIVE)
                .cancelAtPeriodEnd(false)
                .build();
        when(subscriptionRespository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(subscription));

        subscriptionService.updateSubscription("sub_1", SubscriptionStatus.ACTIVE, null, null, false, 3L);

        verify(subscriptionRespository, never()).save(any(Subscription.class));
        verify(planRepository, never()).findById(any());
    }

    @Test
    void cancelSubscription_shouldSetCanceledStatus() {
        Subscription subscription = Subscription.builder()
                .stripeSubscriptionId("sub_1")
                .status(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRespository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(subscription));

        subscriptionService.cancelSubscription("sub_1");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(subscriptionRespository).save(subscription);
    }

    @Test
    void renewSubscriptionPeriod_shouldActivatePastDueAndUsePreviousEndAsStartWhenStartIsNull() {
        Instant previousEnd = Instant.parse("2026-03-01T00:00:00Z");
        Instant newEnd = Instant.parse("2026-04-01T00:00:00Z");
        Subscription subscription = Subscription.builder()
                .stripeSubscriptionId("sub_1")
                .status(SubscriptionStatus.PAST_DUE)
                .currentPeriodEnd(previousEnd)
                .build();
        when(subscriptionRespository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(subscription));

        subscriptionService.renewSubscriptionPeriod("sub_1", null, newEnd);

        assertThat(subscription.getCurrentPeriodStart()).isEqualTo(previousEnd);
        assertThat(subscription.getCurrentPeriodEnd()).isEqualTo(newEnd);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRespository).save(subscription);
    }

    @Test
    void markSubscriptionPastDue_shouldNotSaveWhenAlreadyPastDue() {
        Subscription subscription = Subscription.builder()
                .stripeSubscriptionId("sub_1")
                .status(SubscriptionStatus.PAST_DUE)
                .build();
        when(subscriptionRespository.findByStripeSubscriptionId("sub_1")).thenReturn(Optional.of(subscription));

        subscriptionService.markSubscriptionPastDue("sub_1");

        verify(subscriptionRespository, never()).save(any(Subscription.class));
    }

    @Test
    void getCurrentSubscribedPlanByUser_shouldReturnFreePlanWhenNoSubscriptionExists() {
        when(authUtil.getCurrentUserId()).thenReturn(10L);
        when(subscriptionRespository.findByUserIdAndStatusIn(eq(10L), anySet()))
                .thenReturn(Optional.empty());
        when(subscriptionMapper.toSubscriptionResponse(any(Subscription.class)))
                .thenReturn(new SubscriptionResponse(null, null, null));
        Plan freePlan = new Plan();
        freePlan.setId(1L);
        freePlan.setName("Free");
        when(planRepository.findByNameIgnoreCaseAndActiveTrue("Free")).thenReturn(Optional.of(freePlan));
        PlanDto freeDto = new PlanDto();
        freeDto.setId(1L);
        freeDto.setName("Free");
        when(subscriptionMapper.toPlanResponse(freePlan)).thenReturn(freeDto);

        PlanDto result = subscriptionService.getCurrentSubscribedPlanByUser();

        assertThat(result.getName()).isEqualTo("Free");
        assertThat(result.getId()).isEqualTo(1L);
    }

    private Plan planWithId(Long id) {
        Plan plan = new Plan();
        plan.setId(id);
        return plan;
    }
}
