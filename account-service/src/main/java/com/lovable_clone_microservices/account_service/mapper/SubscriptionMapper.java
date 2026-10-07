package com.lovable_clone_microservices.account_service.mapper;



import com.lovable_clone_microservices.account_service.dto.subscription.SubscriptionResponse;
import com.lovable_clone_microservices.account_service.entity.Plan;
import com.lovable_clone_microservices.account_service.entity.Subscription;
import com.lovable_clone_microservices.common_library.dto.PlanDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(source = "currentPeriodEnd",target = "periodEnd")
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);

}
