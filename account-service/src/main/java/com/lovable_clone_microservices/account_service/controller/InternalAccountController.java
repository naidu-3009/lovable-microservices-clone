package com.lovable_clone_microservices.account_service.controller;

import com.lovable_clone_microservices.account_service.mapper.UserMapper;
import com.lovable_clone_microservices.account_service.repository.UserRepository;
import com.lovable_clone_microservices.account_service.services.SubscriptionService;
import com.lovable_clone_microservices.common_library.dto.PlanDto;
import com.lovable_clone_microservices.common_library.dto.UserDto;
import com.lovable_clone_microservices.common_library.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalAccountController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SubscriptionService subscriptionService;

    @GetMapping("/users/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(userMapper::toUserDto)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }

    @GetMapping("/users/by-email")
    public Optional<UserDto> getUserByEmail(@RequestParam String email) {
        return userRepository.findByUsernameIgnoreCase(email)
                .map(userMapper::toUserDto);
    }

    @GetMapping("/billing/current-plan")
    public PlanDto getCurrentSubscribedPlan() {
        return subscriptionService.getCurrentSubscribedPlanByUser();
    }
}
