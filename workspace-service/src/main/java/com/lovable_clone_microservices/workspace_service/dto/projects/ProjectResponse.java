package com.lovable_clone_microservices.workspace_service.dto.projects;


import java.time.Instant;

public record ProjectResponse(Long id,
                              String name,
                              Instant createdAt,
                              Instant updatedAt
                             ) {
}
