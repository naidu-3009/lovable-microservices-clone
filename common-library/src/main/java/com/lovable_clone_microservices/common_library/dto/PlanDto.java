package com.lovable_clone_microservices.common_library.dto;

import lombok.Getter;
import lombok.Setter;

@Getter

public class PlanDto {
    Long id;
    String name;
    Integer maxProjects;
    Integer maxTokensPerDay;
    Boolean unlimitedAi;
    String  price;
}
