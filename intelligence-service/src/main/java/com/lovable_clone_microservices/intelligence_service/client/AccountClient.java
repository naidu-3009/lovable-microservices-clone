package com.lovable_clone_microservices.intelligence_service.client;


import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@FeignClient(name = "account-service",path = "/account")
public interface AccountClient {

    @GetMapping("/internal/v1/users/by-email")
    Optional<UserDto> getUserByEmail(@RequestParam("email") String email);

    @GetMapping("/internal/v1/billing/current-plan")
    PlanDto getCurrentSubscribedPlanByUser();
}
