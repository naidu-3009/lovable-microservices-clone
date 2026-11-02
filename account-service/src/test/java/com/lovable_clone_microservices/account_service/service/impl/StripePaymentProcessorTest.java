package com.lovable_clone_microservices.account_service.service.impl;

import com.lovable_clone_microservices.account_service.dto.subscription.CheckoutRequest;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.entity.User;
import com.lovable_clone_microservices.account_service.repository.PlanRepository;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.SubscriptionService;
import com.lovable_clone_microservices.account_service.services.implementation.StripePaymentProcessor;
import com.lovable_clone_microservices.common_library.error.BadRequestException;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import com.lovable_clone_microservices.common_library.security.AuthUtil;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripePaymentProcessorTest {

    @Mock
    AuthUtil authUtil;
    @Mock
    UserRepository userRepository;
    @Mock
    PlanRepository planRepository;
    @Mock
    SubscriptionService subscriptionService;

    @InjectMocks
    StripePaymentProcessor paymentProcessor;

    @Test
    void createCheckoutSessionUrl_shouldRejectUnknownPlan() {
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentProcessor.createCheckoutSessionUrl(new CheckoutRequest(999L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(authUtil, userRepository);
    }

    @Test
    void createCheckoutSessionUrl_shouldRejectInactivePlan() {
        Plan plan = new Plan();
        plan.setId(3L);
        plan.setActive(false);
        plan.setStripePriceId("price_123");
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> paymentProcessor.createCheckoutSessionUrl(new CheckoutRequest(3L)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unavailable");

        verifyNoInteractions(authUtil, userRepository);
    }

    @Test
    void createCheckoutSessionUrl_shouldRejectPlanWithoutStripePrice() {
        Plan plan = new Plan();
        plan.setId(3L);
        plan.setActive(true);
        plan.setStripePriceId(" ");
        when(planRepository.findById(3L)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> paymentProcessor.createCheckoutSessionUrl(new CheckoutRequest(3L)))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(authUtil, userRepository);
    }

    @Test
    void openCustomerPortal_shouldRejectUserWithoutStripeCustomerId() {
        User user = User.builder().id(10L).username("nishanth@example.com").stripeCustomerId(null).build();
        when(authUtil.getCurrentUserId()).thenReturn(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> paymentProcessor.openCustomerPortal())
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stripe Customer Id");
    }

    @Test
    void handleWebhookEvent_shouldSaveStripeCustomerAndActivateSubscriptionOnCheckoutCompletion() {
        Long userId = 10L;
        Long planId = 3L;
        User user = User.builder().id(userId).username("nishanth@example.com").build();
        Session session = mock(Session.class);
        when(session.getSubscription()).thenReturn("sub_123");
        when(session.getCustomer()).thenReturn("cus_123");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        Map<String, String> metadata = Map.of("user_id", "10", "plan_id", "3");

        paymentProcessor.handleWebhookEvent("checkout.session.completed", session, metadata);

        org.assertj.core.api.Assertions.assertThat(user.getStripeCustomerId()).isEqualTo("cus_123");
        verify(userRepository).save(user);
        verify(subscriptionService).activateSubscription(userId, planId, "sub_123", "cus_123");
    }

    @Test
    void handleWebhookEvent_shouldCancelSubscriptionWhenDeletionEventArrives() {
        com.stripe.model.Subscription stripeSubscription = mock(com.stripe.model.Subscription.class);
        when(stripeSubscription.getId()).thenReturn("sub_deleted");

        paymentProcessor.handleWebhookEvent("customer.subscription.deleted", stripeSubscription, Map.of());

        verify(subscriptionService).cancelSubscription("sub_deleted");
    }

    @Test
    void handleWebhookEvent_shouldIgnoreUnknownEventTypes() {
        paymentProcessor.handleWebhookEvent("some.unhandled.event", mock(StripeObject.class), Map.of());

        verifyNoInteractions(subscriptionService, userRepository);
    }
}
