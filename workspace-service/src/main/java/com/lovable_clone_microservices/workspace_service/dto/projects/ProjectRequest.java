package com.lovable_clone_microservices.workspace_service.dto.projects;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank String name
) {
}
