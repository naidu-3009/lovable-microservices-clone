package com.lovable_clone_microservices.workspace_service.dto.projects;


import com.lovable_clone_microservices.common_library.enums.ProjectMemberRole;

import java.time.Instant;

public record ProjectSummaryResponse(
        Long projectId,
        String name,
        Instant createdAt,
        Instant updatedAt,
        ProjectMemberRole role
) {
}
