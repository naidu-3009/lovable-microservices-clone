package com.lovable_clone_microservices.account_service.repository;

import com.lovable_clone_microservices.account_service.entity.Subscription;
import com.lovable_clone_microservices.common_library.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface SubscriptionRespository extends JpaRepository<Subscription,Long> {

    /*
    *
    * Get the current active subscription
    * */
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> active);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String subscriptionId);
}
