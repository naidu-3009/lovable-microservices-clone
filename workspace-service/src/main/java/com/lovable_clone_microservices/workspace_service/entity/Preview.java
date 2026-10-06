package com.lovable_clone_microservices.workspace_service.entity;


import com.lovable_clone_microservices.common_library.enums.PreviewStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class Preview {
    Long id;

    Project project;

    String namespace;
    String pod_name;
    PreviewStatus status;
    String preview_url;

    Instant startedAt;
    Instant terminatedAt;
    Instant createdAt;

    public static class ProjectMemberId {
        Long projectId;
        Long UserId;

    }
}
