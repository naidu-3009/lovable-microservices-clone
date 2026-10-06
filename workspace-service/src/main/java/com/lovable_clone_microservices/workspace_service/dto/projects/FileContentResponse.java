package com.lovable_clone_microservices.workspace_service.dto.projects;

public record FileContentResponse(
        String path,
        String content
) {
}
